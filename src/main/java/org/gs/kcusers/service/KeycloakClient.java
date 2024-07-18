package org.gs.kcusers.service;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
@EnableScheduling
public class KeycloakClient {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakClient.class);
    @Autowired
    ProtectedUsers protectedUsers;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EventRepository eventRepository;
    @Value("${service.keycloakclient.realms}")
    private String KEYCLOAK_REALMS;

    @Value("${service.keycloakclient.url}")
    private String KEYCLOAK_URL;

    @Value("${service.keycloakclient.admin.realm}")
    private String KEYCLOAK_ADMIN_USER_REALM;

    @Value("${service.keycloakclient.admin.login}")
    private String KEYCLOAK_ADMIN_LOGIN;

    @Value("${service.keycloakclient.admin.password}")
    private String KEYCLOAK_ADMIN_PASSWORD;

    @Value("${service.keycloakclient.client}")
    private String KEYCLOAK_CLIENT;

    @Value("${service.keycloakclient.inactivity.dryrun}")
    private boolean KEYCLOAK_DRY_RUN;

    public KeycloakClient(@Value("${service.cron}") String cron) {
        logger.info("Scheduled task with cron {}", cron);
    }

    private Keycloak buildKeyclak() {
        return KeycloakBuilder.builder()
                .serverUrl(KEYCLOAK_URL)
                .realm(KEYCLOAK_ADMIN_USER_REALM)
                .clientId(KEYCLOAK_CLIENT)
                .grantType(OAuth2Constants.PASSWORD)
                .username(KEYCLOAK_ADMIN_LOGIN)
                .password(KEYCLOAK_ADMIN_PASSWORD)
                .build();
    }

    @Scheduled(cron = "${service.cron}")
    public void startPolling() {
        logger.info("-------- START TASK --------");
        List<String> realmNames = Arrays.asList(KEYCLOAK_REALMS.split("\\s*,\\s*"));
        try (Keycloak keycloak = buildKeyclak()) {
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

        } catch (ProcessingException | NotFoundException e) {
            if (e.getCause() instanceof NotFoundException || e instanceof NotFoundException) {
                logger.error("Realm {} not found", realmName);
            } else if (e.getCause() instanceof HttpHostConnectException ||
                    e.getCause() instanceof NotAuthorizedException) {
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

            // фильтруем недавно созданных пользователей (текущая дата - настройка inactivity.days)
            // фильтруем активных пользователей (текущая дата - настройка inactivity.days)
            // фильтруем недавно включенных вручную пользователей (настройка inactivity.immunityperiodminutes)
            boolean shouldBeBlocked = user.userShouldBeBlocked();
            if (user.getEnabled() && shouldBeBlocked) {
                user.setEnabled(false);
                logger.info("User {} ({}) become inactive. Will be DISABLED.", user.getUserName(), user.getRealmName());
                disable = true;
            } else if (user.getEnabled() && user.userShouldBeUnblocked()) {
                user.setManuallyEnabledTime(null);
                user.setCommentEnabledAfterBecomeActive();
                userRepository.save(user);
                eventRepository.save(new Event(user.getUserName(), user.getRealmName(), Instant.now().toEpochMilli(),
                        "system", user.getComment(), user.getEnabled()));
            } if (forceUpdate){
                userRepository.save(user);
            }

            //else if (!user.getEnabled() && !shouldBeBlocked) {
            //user.setEnabled(true);
            //user.setManuallyEnabledTime(null);
            //user.setCommentEnabledBy("service automatically");
            //userRepository.save(user);
            //} else
//            else if (ourSavedUser == null) {
//                userRepository.save(user); // add new user
//
//            }

            if (disable) {
                if (!KEYCLOAK_DRY_RUN) {
                    disableUser(keycloak, user);
                } else {
                    logger.info("(DRY RUN) DISABLED user {} ({})", user.getUserName(), user.getRealmName());
                }
            }
        }
    }


    private void updateUser(Keycloak keycloak, User user, String admLogin) {
        UsersResource usersResource = keycloak.realm(user.getRealmName()).users();
        UserResource userResource = usersResource.get(user.getUserId());
        UserRepresentation userRepresentation = userResource.toRepresentation();
        userRepresentation.setEnabled(user.getEnabled());
        userResource.update(userRepresentation);
        userRepository.save(user);
        eventRepository.save(new Event(user.getUserName(), user.getRealmName(), Instant.now().toEpochMilli(), admLogin,
                user.getComment(), user.getEnabled()));
    }

    private void disableUser(Keycloak keycloak, User user) {
        user.setEnabled(false);
        user.setManuallyEnabledTime(null);
        user.setCommentDisabledForInactivity();
        updateUser(keycloak, user, "system");
        logger.info("Successfully DISABLED user {} ({})", user.getUserName(), user.getRealmName());
    }

    public void updateUserFromController(User user, String admLogin) {
        try (Keycloak keycloak = buildKeyclak()) {
            updateUser(keycloak, user, admLogin);
        }
    }
}
