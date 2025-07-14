package org.gs.kcusers.service;

//import jakarta.ws.rs.ForbiddenException;
//import jakarta.ws.rs.NotAuthorizedException;
//import jakarta.ws.rs.NotFoundException;
//import jakarta.ws.rs.ProcessingException;

import org.apache.http.conn.HttpHostConnectException;
import org.gs.kcusers.configs.yamlobjects.ProtectedUsers;
import org.gs.kcusers.domain.Audit;
import org.gs.kcusers.domain.Event;
import org.gs.kcusers.domain.User;
import org.gs.kcusers.repositories.AuditRepository;
import org.gs.kcusers.repositories.EventRepository;
import org.gs.kcusers.repositories.UserRepository;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.ClientBuilderWrapper;
import org.keycloak.admin.client.JacksonProvider;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import java.io.FileNotFoundException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.gs.kcusers.configs.yamlobjects.Configurations.KCUSERS_SCHEDULED_SERVICE;
import static org.springframework.util.ResourceUtils.getFile;
import static org.springframework.util.ResourceUtils.isUrl;

@Component
@EnableScheduling
public class KeycloakClient {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakClient.class);

    final
    ProtectedUsers protectedUsers;

    private final UserRepository userRepository;

    private final EventRepository eventRepository;

    private final AuditRepository auditRepository;

    @Value("${service.keycloakclient.realms}")
    private String KEYCLOAK_REALMS;

    @Value("${service.keycloakclient.url}")
    private String KEYCLOAK_URL;

    @Value("${service.keycloakclient.admin.realm}")
    private String KEYCLOAK_ADMIN_USER_REALM;

    @Value("${service.keycloakclient.admin.login:#{null}}")
    private String KEYCLOAK_ADMIN_LOGIN;

    @Value("${service.keycloakclient.admin.password:#{null}}")
    private String KEYCLOAK_ADMIN_PASSWORD;

    @Value("${service.keycloakclient.client.client-id}")
    private String KEYCLOAK_CLIENT;

    @Value("${service.keycloakclient.client.client-secret:#{null}}")
    private String KEYCLOAK_CLIENT_SECRET;

    @Value("${service.keycloakclient.inactivity.dryrun:false}")
    private boolean KEYCLOAK_DRY_RUN;

    @Value("${service.keycloakclient.mtls.enabled:false}")
    boolean mtlsEnabled;

    @Value("${service.keycloakclient.mtls.keyStore.path:#{null}}")
    String path;

    @Value("${service.keycloakclient.mtls.keyStore.type:#{null}}")
    String type;

    @Value("${service.keycloakclient.mtls.keyStore.password:#{null}}")
    String password;

    public KeycloakClient(@Value("${service.cron}") String cron,
                          ProtectedUsers protectedUsers,
                          UserRepository userRepository,
                          EventRepository eventRepository,
                          AuditRepository auditRepository) {
        logger.info("Scheduled task with cron {}", cron);
        this.protectedUsers = protectedUsers;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.auditRepository = auditRepository;
    }

    @PostConstruct
    public void setMtlsEnvironmentProperties() throws FileNotFoundException {
        if (!mtlsEnabled) {
            logger.info("MTLS is disabled");
            return;
        } else {
            logger.info("MTLS is enabled");
        }
        if (path != null && !path.isEmpty() && type != null && !type.isEmpty()) {
            String absolutePath = path;
            logger.info("MTLS properties set: keyStore: '{}', " +
                    "keyStoreType: '{}', " +
                    "keyStorePassword: '{}'", path, type, "****");
            if (isUrl(path)) {
                absolutePath = getFile(absolutePath).getPath();
            }

            System.setProperty("javax.net.ssl.keyStoreType", type);
            System.setProperty("javax.net.ssl.keyStore", absolutePath);
            System.setProperty("javax.net.ssl.keyStorePassword", password);

        } else {
            logger.info("MTLS properties NOT set. Some parameters are empty.");
        }
    }

    private Keycloak buildKeycloak() {
        var builder = KeycloakBuilder.builder()
                .serverUrl(KEYCLOAK_URL)
                .realm(KEYCLOAK_ADMIN_USER_REALM)
                .clientId(KEYCLOAK_CLIENT);

        if (KEYCLOAK_CLIENT_SECRET != null && !KEYCLOAK_CLIENT_SECRET.isEmpty()) {
            // client credentials auth
            builder
                    .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                    .clientSecret(KEYCLOAK_CLIENT_SECRET);

        }
        if (KEYCLOAK_ADMIN_PASSWORD != null && !KEYCLOAK_ADMIN_PASSWORD.isEmpty()
                && KEYCLOAK_ADMIN_LOGIN != null && !KEYCLOAK_ADMIN_LOGIN.isEmpty()) {

            // password auth
            builder
                    .grantType(OAuth2Constants.PASSWORD)
                    .username(KEYCLOAK_ADMIN_LOGIN)
                    .password(KEYCLOAK_ADMIN_PASSWORD);
        }

        // set ssl context to RESTEasy client
        try {
            builder.resteasyClient((ResteasyClient) (ClientBuilderWrapper.create(SSLContext.getDefault(), false).register(JacksonProvider.class, 100)).build());
        } catch (NoSuchAlgorithmException e) {
            auditRepository.save(new Audit(
                    Audit.ENT_KEYCLOAK,
                    null,
                    e.getLocalizedMessage()));
            throw new RuntimeException(e);
        }

        //else throw new IllegalArgumentException("Invalid keycloak admin or password, or secret");
        return builder.build();
    }

    @Scheduled(cron = "${service.cron}")
    public void startPolling() {
        logger.info("-------- START TASK --------");
        List<String> realmNames = Arrays.asList(KEYCLOAK_REALMS.split("\\s*,\\s*"));
        try (Keycloak keycloak = buildKeycloak()) {
            realmNames.forEach(realmName -> processUsers(keycloak, getUsers(keycloak, realmName)));
        }
        logger.info("-------- FINISHED TASK --------");
    }

    private Long getLastLoginTime(Keycloak keycloak, String realmName, UserRepresentation userRepresentation) {
        String from = LocalDate.now().minusDays(90).toString();
        String to = LocalDate.now().plusDays(1).toString();

        return keycloak
                .realm(realmName)
                .getEvents(
                        List.of("LOGIN"),
                        null,
                        userRepresentation.getId(),
                        from,
                        to,
                        null,
                        null,
                        null
                )
                .stream()
                .mapToLong(EventRepresentation::getTime)
                .max()
                .orElse(0L);
    }


    private User userPresentationToUser(Keycloak keycloak, String realmName, UserRepresentation userRepresentation) {
        logUserRepresentation(realmName, userRepresentation);


        var lastLoginTime = getLastLoginTime(keycloak, realmName, userRepresentation);

        User user = userRepository.findByUserNameAndRealmName(userRepresentation.getUsername(), realmName);

        if (user != null) {
            // if user was enabled on keycloak side
            // we have to redisable him
            user.setEnabled(userRepresentation.isEnabled());
            user.setUserId(userRepresentation.getId());
            user.setLastLogin(lastLoginTime);
            return user;
        } else {
            return new User(
                    userRepresentation.getUsername(),
                    realmName,
                    userRepresentation.getId(),
                    lastLoginTime,
                    userRepresentation.getCreatedTimestamp(),
                    userRepresentation.isEnabled(),
                    null,
                    null
            );
        }
    }


    private List<User> getUsers(Keycloak keycloak, String realmName) {
        List<User> users = null;

        try {
            logger.info("-- Looking for users of realm {}", realmName);

            users = keycloak
                    .realm(realmName)
                    .users()
                    .list()
                    .stream()
                    .map(userRepresentation -> userPresentationToUser(keycloak, realmName, userRepresentation)
                    ).toList();

            logger.info("Users of realm {} found: {}", realmName, users.stream().map(User::getUserName).toList());
        } catch (ProcessingException | ForbiddenException | NotFoundException e) {
            var cause = e.getCause();
            if (cause instanceof NotFoundException || e instanceof NotFoundException) {
                String message = String.format("Realm %s not found", realmName);
                logger.error(message);
                auditRepository.save(new Audit(
                        Audit.ENT_KEYCLOAK,
                        realmName,
                        message));
            } else if (cause instanceof HttpHostConnectException ||
                    cause instanceof NotAuthorizedException ||
                    e instanceof ForbiddenException) {
                String message = String.format("Connection to realm %s refused: %s", realmName, e.getLocalizedMessage());
                logger.error(message);
                auditRepository.save(new Audit(
                        Audit.ENT_KEYCLOAK,
                        realmName,
                        message));
            } else {
                auditRepository.save(new Audit(
                        Audit.ENT_KEYCLOAK,
                        realmName,
                        e.getLocalizedMessage()));
                throw e;
            }
        }

        return users;
    }

    private void addNewUser(User user) {
        userRepository.save(user);// add new user
        eventRepository.save(new Event(user.getUserName(), user.getRealmName(), Instant.now().toEpochMilli(),
                KCUSERS_SCHEDULED_SERVICE, "Добавлен пользователь Keycloak", user.getEnabled()));
    }

    private boolean forceUpdateUser(User ourSavedUser, User user) {
        return ourSavedUser == null ||
                !Objects.equals(ourSavedUser.getLastLogin(), user.getLastLogin()) ||
                !Objects.equals(ourSavedUser.getUserId(), user.getUserId()) ||
                !Objects.equals(ourSavedUser.getCreated(), user.getCreated());
    }

    private void processUser(Keycloak keycloak, User user) {
        boolean disable = false;
        boolean forceUpdate = false;
        User ourSavedUser = userRepository.findByUserNameAndRealmName(user.getUserName(), user.getRealmName());
        if (ourSavedUser != null) {
            // такой пользователь уже сохранен у нас в БД
            disable = user.getEnabled() && !ourSavedUser.getEnabled();
            if (disable) {
                // пользователь сохранен у нас как отключенный, а с сервера пришел как включенный
                // выключим его
                logger.info("Found already saved user {} ({}) as disabled. Will be DISABLED.",
                        ourSavedUser.getUserName(), ourSavedUser.getRealmName());
            }

            forceUpdate = forceUpdateUser(ourSavedUser, user);

            user.setEnabled(ourSavedUser.getEnabled());
            user.setCreated(ourSavedUser.getCreated());
        } else {
            addNewUser(user);
        }

        boolean userIsNotInImmunityPeriod = user.userIsNotInImmunityPeriod();
        boolean userIsInactiveInImmunityPeriod = user.userIsInactiveInImmunityPeriod();

        if (user.getEnabled() && user.userIsOldAndInactive() && userIsNotInImmunityPeriod) {
            // Блокировать пользоваеля, если:
            // * пользователь старый,
            // * не логинился в течении INACTIVITY_DAYS дней
            // * не находится во временном включенном состоянии IMMUNITY_PERIOD_MINUTES минут
            logger.info("User {} ({}) become inactive. Will be DISABLED.",
                    user.getUserName(), user.getRealmName());
            disable = true;
        } else if (user.getEnabled()
                && (userIsNotInImmunityPeriod || !userIsInactiveInImmunityPeriod)
                && user.getManuallyEnabledTime() != null) {
            if (userIsInactiveInImmunityPeriod) {
                // Блокировать пользоваеля, если:
                // * находится во временном включенном состоянии IMMUNITY_PERIOD_MINUTES минут
                // * не залогинился за последние IMMUNITY_PERIOD_MINUTES минут
                logger.info("User {} ({}) not logged in in immunity period. Will be DISABLED.",
                        user.getUserName(), user.getRealmName());
                disable = true;
            } else {
                // Разблокировать пользоваеля
                logger.info("User {} ({}) become active. Will be ENABLED.", user.getUserName(), user.getRealmName());
                user.setManuallyEnabledTime(null);
                user.setCommentEnabledAfterBecomeActive();
                userRepository.save(user);
                eventRepository.save(new Event(user.getUserName(), user.getRealmName(), Instant.now().toEpochMilli(),
                        "system", user.getComment(), user.getEnabled()));
            }
        }

        if (forceUpdate) {
            userRepository.save(user);
        }

        if (disable) {
            disableUser(keycloak, user);
        }
    }

    private void justSaveUser(User user) {
        User ourSavedUser = userRepository.findByUserNameAndRealmName(user.getUserName(), user.getRealmName());
        boolean forceUpdate = forceUpdateUser(ourSavedUser, user);
        if (forceUpdate) {
            userRepository.save(user);
            logger.info("Updated user information {} ({})", user.getUserName(), user.getRealmName());
        }
    }

    private void processUsers(Keycloak keycloak, List<User> users) {
        if (users == null) return;
        // проверка незащищенных пользователей (с возможной блокировкой):
        users.stream()
                .filter(this::userIsUnprotected)
                .forEach(user -> processUser(keycloak, user));

        // проверка защищенных пользователей (просто сохранение изменений):
        users.stream()
                .filter(this::userIsProtected)
                .forEach(this::justSaveUser);
    }

    private boolean updateUser(Keycloak keycloak, User user, String admLogin) {
        if (userIsProtected(user)) {
            return false;
        }
        try {
            UsersResource usersResource = keycloak.realm(user.getRealmName()).users();
            UserResource userResource = usersResource.get(user.getUserId());
            UserRepresentation userRepresentation = userResource.toRepresentation();
            userRepresentation.setEnabled(user.getEnabled());
            if (!KEYCLOAK_DRY_RUN) {
                userResource.update(userRepresentation);
                auditRepository.save(new Audit(
                        Audit.ENT_KEYCLOAK,
                        Audit.SUBTYPE_UPDATE,
                        user.getRealmName(),
                        user.getUserName(),
                        user.getEnabled(),
                        user.getComment())
                );
            } else {
                logger.info("DRY RUN ENABLED. Update of user {} ({}) skipped", user.getUserName(), user.getRealmName());
            }
            userRepository.save(user);
            eventRepository.save(
                    new Event(
                            user.getUserName(), user.getRealmName(),
                            Instant.now().toEpochMilli(), admLogin,
                            user.getComment(), user.getEnabled()
                    )
            );
            return true;
        } catch (Exception e) {
            String message = String.format("Error while updating user %s (%s): %s", user.getUserName(),
                    user.getRealmName(), e.getLocalizedMessage());

            logger.error(message);

            auditRepository.save(new Audit(
                    Audit.ENT_KEYCLOAK,
                    Audit.SUBTYPE_ERR,
                    user.getRealmName(),
                    user.getUserName(),
                    user.getEnabled(),
                    message));
        }
        return false;
    }

    private boolean userIsProtected(User user) {
        return protectedUsers.getProtectedusers().contains(user.getUserName());
    }

    private boolean userIsUnprotected(User user) {
        return !protectedUsers.getProtectedusers().contains(user.getUserName());
    }

    private void disableUser(Keycloak keycloak, User user) {
        if (userIsProtected(user)) {
            return;
        }
        user.setEnabled(false);
        user.setManuallyEnabledTime(null);
        user.setCommentDisabledForInactivity();
        updateUser(keycloak, user, "system");
        logger.info("Successfully DISABLED user {} ({})", user.getUserName(), user.getRealmName());
    }

    public boolean updateUserFromController(User user, String admLogin) {
        if (userIsProtected(user)) {
            return false;
        }
        try (Keycloak keycloak = buildKeycloak()) {
            return updateUser(keycloak, user, admLogin);
        }
    }

    private void logUserRepresentation(String realmName, UserRepresentation user) {
        logger.info("DEBUG USERREPRESENTATION realm: {}, origin: {}, name: {}, enabled: {}, " +
                        "emailVerified: {}, firstName: {}, lastName: {}, email: {}, " +
                        "federationLink: {}, attributes: {}, realmRoles: {}, clientRoles: {}",

                realmName,
                user.getOrigin(),
                user.getUsername(),
                user.isEnabled(),
                user.isEmailVerified(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getFederationLink(),
                user.getAttributes(),
                user.getRealmRoles(),
                user.getClientRoles()

        );



        //protected List<CredentialRepresentation> credentials;
        //protected Set<String> disableableCredentialTypes;
        //protected List<String> requiredActions;
        //protected List<FederatedIdentityRepresentation> federatedIdentities;

        //protected Map<String, List<String>> clientRoles;
        //protected List<UserConsentRepresentation> clientConsents;
        //protected Integer notBefore;

    }
}
