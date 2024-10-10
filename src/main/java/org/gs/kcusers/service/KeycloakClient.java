package org.gs.kcusers.service;

//import jakarta.ws.rs.ForbiddenException;
//import jakarta.ws.rs.NotAuthorizedException;
//import jakarta.ws.rs.NotFoundException;
//import jakarta.ws.rs.ProcessingException;
import org.apache.http.conn.HttpHostConnectException;
import org.gs.kcusers.configs.ProtectedUsers;
import org.gs.kcusers.domain.Event;
import org.gs.kcusers.domain.User;
import org.gs.kcusers.repositories.EventRepository;
import org.gs.kcusers.repositories.UserRepository;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
@EnableScheduling
public class KeycloakClient {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakClient.class);

    final
    ProtectedUsers protectedUsers;

    private final UserRepository userRepository;

    private final EventRepository eventRepository;

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

    @Value("${service.keycloakclient.inactivity.dryrun}")
    private boolean KEYCLOAK_DRY_RUN;

    public KeycloakClient(@Value("${service.cron}") String cron,
                          ProtectedUsers protectedUsers,
                          UserRepository userRepository,
                          EventRepository eventRepository) {
        logger.info("Scheduled task with cron {}", cron);
        this.protectedUsers = protectedUsers;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
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
            logger.info("-- Looking for users of realm {} (skipping protected users {}) ",
                    realmName, protectedUsers.getProtectedUsers().toString());

            users = keycloak
                    .realm(realmName)
                    .users()
                    .list()
                    .stream()
                    // фильтруем пользователей, которые указаны в настройке protectedUsers
                    .filter(userRepresentation -> !protectedUsers.getProtectedUsers().contains(userRepresentation.getUsername()))
                    .map(userRepresentation -> userPresentationToUser(keycloak, realmName, userRepresentation)
                    ).toList();

            logger.info("Users of realm {} found: {}", realmName, users.stream().map(User::getUserName).toList());
        } catch (ProcessingException | ForbiddenException | NotFoundException e) {
            var cause = e.getCause();
            if (cause instanceof NotFoundException || e instanceof NotFoundException) {
                logger.error("Realm {} not found", realmName);
            } else if (cause instanceof HttpHostConnectException ||
                    cause instanceof NotAuthorizedException ||
                    e instanceof ForbiddenException) {
                logger.error("Connection to realm {} refused: {}", realmName, e.getLocalizedMessage());
            } else {
                throw e;
            }
        }

        return users;
    }

    private void processUsers(Keycloak keycloak, List<User> users) {
        if (users == null) return;

        for (var user : users) {
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

                forceUpdate = !Objects.equals(ourSavedUser.getLastLogin(), user.getLastLogin()) ||
                        !Objects.equals(ourSavedUser.getUserId(), user.getUserId()) ||
                        !Objects.equals(ourSavedUser.getCreated(), user.getCreated());

                user.setEnabled(ourSavedUser.getEnabled());
                user.setCreated(ourSavedUser.getCreated());
            } else {
                userRepository.save(user);// add new user
                eventRepository.save(new Event(user.getUserName(), user.getRealmName(), Instant.now().toEpochMilli(),
                        "system", "add user from Keycloack", user.getEnabled()));

            }

            boolean userIsNotInImmunityPeriod = user.userIsNotInImmunityPeriod();
            if (user.getEnabled() && user.userIsOldAndInactive() && userIsNotInImmunityPeriod) {
                // Блокировать пользоваеля, если:
                // * пользователь старый,
                // * не логинился в течении INACTIVITY_DAYS дней
                // * не находится во временном включенном состоянии IMMUNITY_PERIOD_MINUTES минут
                logger.info("User {} ({}) become inactive. Will be DISABLED.",
                        user.getUserName(), user.getRealmName());
                disable = true;
            } else if (user.getEnabled() && userIsNotInImmunityPeriod && user.getManuallyEnabledTime() != null) {
                if (user.userIsInactiveInImmunityPeriod()) {
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
    }

    private boolean updateUser(Keycloak keycloak, User user, String admLogin) {
        try {
            UsersResource usersResource = keycloak.realm(user.getRealmName()).users();
            UserResource userResource = usersResource.get(user.getUserId());
            UserRepresentation userRepresentation = userResource.toRepresentation();
            userRepresentation.setEnabled(user.getEnabled());
            if (!KEYCLOAK_DRY_RUN) {
                userResource.update(userRepresentation);
            } else {
                logger.info("DRY RUN ENBLED. Update of user {} ({}) skipped", user.getUserName(), user.getRealmName());
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
        } catch (ForbiddenException e) {
            logger.error("Error while updating user {} ({}): {}", user.getUserName(),
                    user.getRealmName(), e.getLocalizedMessage());
        }
        return false;
    }

    private void disableUser(Keycloak keycloak, User user) {
        user.setEnabled(false);
        user.setManuallyEnabledTime(null);
        user.setCommentDisabledForInactivity();
        updateUser(keycloak, user, "system");
        logger.info("Successfully DISABLED user {} ({})", user.getUserName(), user.getRealmName());
    }

    public boolean updateUserFromController(User user, String admLogin) {
        try (Keycloak keycloak = buildKeycloak()) {
            return updateUser(keycloak, user, admLogin);
        }
    }
}
