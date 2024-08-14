package org.gs.kcusers.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import org.gs.kcusers.configs.Configurations;
import org.gs.kcusers.configs.LocalizedMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Entity
@Data
@AllArgsConstructor
@IdClass(User.UserPK.class)
@Table(name = "users")
public class User {
    private static Logger logger = LoggerFactory.getLogger(User.class);

    @Id
    String userName;
    @Id
    String realmName;
    @NonNull
    String userId;
    Long lastLogin;
    @NonNull
    Long created;
    @NonNull
    Boolean enabled;

    Long manuallyEnabledTime;
    String comment;

    public User() {
    }

    private String addNow() {
        String formattedDate = DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm:ss z")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());
        return " (" + formattedDate + ")";
    }

    public boolean userIsOldAndInactive() {
        return userIsOld() && userIsInactive();
    }

    public boolean userShouldBeBlocked() {
        return userIsOldAndInactive() && userIsNotInImmunityPeriod();
    }

    public boolean userShouldBeUnblocked() {
        return !userIsInactive() && !userIsNotInImmunityPeriod();
    }

    public void setCommentDisabledForInactivity() {
        setComment(LocalizedMessages.getMessage("backend.user.disabledduetoinactivity",
                new Object[]{Configurations.INACTIVITY_DAYS(), addNow()}));
    }

    public void setCommentEnabledAfterBecomeActive() {
        setComment(LocalizedMessages.getMessage("backend.user.enabledbecomeactive"));
    }

    public void setCommentEnabledTemporarily(String adminUserName) {
        setComment(LocalizedMessages.getMessage("backend.user.enabledforimmunitiperiod",
                new Object[]{adminUserName, Configurations.IMMUNITY_PERIOD_MINUTES(), addNow()}));
    }

    public void setCommentDisabledBy(String adminUserName) {
        setComment(LocalizedMessages.getMessage("backend.user.disabledby",
                new Object[]{adminUserName, addNow()}));
    }

    public void setCommentEnabledBy(String adminUserName) {
        setComment(LocalizedMessages.getMessage("backend.user.enabledby",
                new Object[]{adminUserName, addNow()}));
    }

    public boolean userIsOld() {
        Instant created = Instant.ofEpochMilli(getCreated());
        Instant threshold = Instant.now().minus(Configurations.INACTIVITY_DAYS(), ChronoUnit.DAYS);
        boolean result = created.isBefore(threshold);
        if (result) {
            logger.info("{} ({}) created {} before {} (user is old, INACTIVITY_DAYS={})",
                    getUserName(), getRealmName(), created, threshold, Configurations.INACTIVITY_DAYS());
        } else {
            logger.info("{} ({}) created {} after {} (user is new, INACTIVITY_DAYS={})",
                    getUserName(), getRealmName(), created, threshold, Configurations.INACTIVITY_DAYS());
        }
        return result;
    }

    public boolean userIsInactive() {
        Instant lastlogin = Instant.ofEpochMilli(getLastLogin());
        Instant threshold = Instant.now().minus(Configurations.INACTIVITY_DAYS(), ChronoUnit.DAYS);

        boolean result = lastlogin.isBefore(threshold);
        if (result) {
            logger.info("{} ({}) logged in {} before {} (user is inactive, INACTIVITY_DAYS={})",
                    getUserName(), getRealmName(), lastlogin, threshold, Configurations.INACTIVITY_DAYS());
        } else {
            logger.info("{} ({}) logged in {} after {} (user is active, INACTIVITY_DAYS={})",
                    getUserName(), getRealmName(), lastlogin, threshold, Configurations.INACTIVITY_DAYS());
        }
        return result;
    }

    public boolean userIsNotInImmunityPeriod() {
        Long l = getManuallyEnabledTime();
        if (l == null) return true;
        Instant userManuallyEnabledTime = Instant.ofEpochMilli(l);
        Instant threshold = userManuallyEnabledTime.plus(Configurations.IMMUNITY_PERIOD_MINUTES(), ChronoUnit.MINUTES);
        Instant now = Instant.now();
        boolean result = threshold.isBefore(now);
        if (result) {
            logger.info("{} ({}) manually enabled {} before {} (user become inactive again, IMMUNITY_PERIOD_MINUTES={})",
                    getUserName(), getRealmName(), userManuallyEnabledTime, now, Configurations.IMMUNITY_PERIOD_MINUTES());
        } else {
            logger.info("{} ({}) manually enabled {} after {} (user is still active, IMMUNITY_PERIOD_MINUTES={})",
                    getUserName(), getRealmName(), userManuallyEnabledTime, now, Configurations.IMMUNITY_PERIOD_MINUTES());
        }
        return result;
    }

    public void setUserStatusFromController(boolean enabled, String adminName) {
        if (this.enabled != enabled) {
            boolean userShouldBeBlocked = userIsOldAndInactive();
            boolean enableTemporarily = enabled && userShouldBeBlocked;
            setEnabled(enabled);
            if (enabled) {
                if (enableTemporarily) {
                    setCommentEnabledTemporarily(adminName);
                    setManuallyEnabledTime(Instant.now().toEpochMilli());
                } else {
                    setCommentEnabledBy(adminName);
                    setManuallyEnabledTime(null);
                }
            } else {
                setCommentDisabledBy(adminName);
                setManuallyEnabledTime(null);
            }
        }
    }


    public static class UserPK implements Serializable {
        private String userName;
        private String realmName;

        public UserPK() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UserPK userPK = (UserPK) o;
            return Objects.equals(userName, userPK.userName) && Objects.equals(realmName, userPK.realmName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userName, realmName);
        }
    }
}
