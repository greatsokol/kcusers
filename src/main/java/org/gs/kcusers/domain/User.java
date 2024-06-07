package org.gs.kcusers.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import org.gs.kcusers.configs.Configurations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

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

    public boolean userShouldBeBlocked() {
        return userIsOld() && userIsInactive() && userIsNotInImmunityPeriod();
    }

    public void setCommentDisabledForInactivity() {
        setComment("Disabled due to inactivity for " + Configurations.INACTIVITY_DAYS + " days" + addNow());
    }

    public void setCommentEnabledTemporarily() {
        setComment("Enabled for " + Configurations.IMMUNITY_PERIOD_MINUTES + " minutes" + addNow());
    }

    public void setCommentDisabledBy(String adminUserName) {
        setComment("Disabled by " + adminUserName + addNow());
    }

    public void setCommentEnabledBy(String adminUserName) {
        setComment("Enabled by " + adminUserName + addNow());
    }

    public boolean userIsOld() {
        Instant created = Instant.ofEpochMilli(getCreated());
        Instant threshold = Instant.now().minus(Configurations.INACTIVITY_DAYS, ChronoUnit.DAYS);
        boolean result = created.isBefore(threshold);
        if (result) {
            logger.info("{} ({}) created {} before {} (user is old, INACTIVITY_DAYS={})",
                    getUserName(), getRealmName(), created, threshold, Configurations.INACTIVITY_DAYS);
        } else {
            logger.info("{} ({}) created {} after {} (user is new, INACTIVITY_DAYS={})",
                    getUserName(), getRealmName(), created, threshold, Configurations.INACTIVITY_DAYS);
        }
        return result;
    }

    public boolean userIsInactive() {
        Instant lastlogin = Instant.ofEpochMilli(getLastLogin());
        Instant threshold = Instant.now().minus(Configurations.INACTIVITY_DAYS, ChronoUnit.DAYS);

        boolean result = lastlogin.isBefore(threshold);
        if (result) {
            logger.info("{} ({}) logged in {} before {} (user is inactive, INACTIVITY_DAYS={})",
                    getUserName(), getRealmName(), lastlogin, threshold, Configurations.INACTIVITY_DAYS);
        } else {
            logger.info("{} ({}) logged in {} after {} (user is active, INACTIVITY_DAYS={})",
                    getUserName(), getRealmName(), lastlogin, threshold, Configurations.INACTIVITY_DAYS);
        }
        return result;
    }

    public boolean userIsNotInImmunityPeriod() {
        Long l = getManuallyEnabledTime();
        if (l == null) return true;
        Instant userManuallyEnabledTime = Instant.ofEpochMilli(l);
        Instant threshold = userManuallyEnabledTime.plus(Configurations.IMMUNITY_PERIOD_MINUTES, ChronoUnit.MINUTES);
        Instant now = Instant.now();
        boolean result = threshold.isBefore(now);
        if (result) {
            logger.info("{} ({}) manually enabled {} before {} (user become inactive again, IMMUNITY_PERIOD_MINUTES={})",
                    getUserName(), getRealmName(), userManuallyEnabledTime, now, Configurations.IMMUNITY_PERIOD_MINUTES);
        } else {
            logger.info("{} ({}) manually enabled {} after {} (user is still active, IMMUNITY_PERIOD_MINUTES={})",
                    getUserName(), getRealmName(), userManuallyEnabledTime, now, Configurations.IMMUNITY_PERIOD_MINUTES);
        }
        return result;
    }

    protected static class UserPK implements Serializable {
        private String userName;
        private String realmName;

        public UserPK() {
        }
    }
}
