package org.gs.kcusers.domain;

import org.gs.kcusers.configs.Configurations;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

class UserTest {
    private User createTestUser() {
        Long inactiveMillis = Instant.now().minus(
                Configurations.INACTIVITY_DAYS,
                ChronoUnit.DAYS
        ).toEpochMilli();
        Long manuallyTime = Instant.now().minus(
                Configurations.IMMUNITY_PERIOD_MINUTES,
                ChronoUnit.MINUTES
        ).toEpochMilli();

        return new User(
                "userName",
                "realmName",
                "userId",
                inactiveMillis,
                inactiveMillis,
                true,
                manuallyTime,
                "comment"
        );
    }

    @Test
    void userShouldBeBlocked() {
        User user = createTestUser();
        Assert.isTrue(user.userShouldBeBlocked(), "user should not be blocked");
    }

    @Test
    void userIsOld() {
        User user = createTestUser();
        Assert.isTrue(user.userIsOld(), "user is not old");
    }

    @Test
    void userIsInactive() {
        User user = createTestUser();
        Assert.isTrue(user.userIsInactive(), "user is not inactive");
    }

    @Test
    void userIsNotInImmunityPeriod() {
        User user = createTestUser();
        Assert.isTrue(user.userIsNotInImmunityPeriod(), "user is not in immunity period");
    }
}