/*
 * Created by Eugene Sokolov 05.08.2024, 10:07.
 */

package org.gs.kcusers.domain;

import org.gs.kcusers.configs.Configurations;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.util.Assert;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

class UserTest {
    private static MockedStatic<Configurations> mockedSettings;
    private final String userName = "User Name";
    private final String realmName = "testrealm";
    private final boolean userEnabled = true;
    private final String userId = "userId";
    private final String userComment = "comment";

    private final Long lastLogin = Instant.now().minus(
            Configurations.INACTIVITY_DAYS(),
            ChronoUnit.DAYS
    ).toEpochMilli();

    private final Long created = Instant.now().minus(
            Configurations.INACTIVITY_DAYS(),
            ChronoUnit.DAYS
    ).toEpochMilli();

    private final Long manuallyTime = Instant.now().minus(
            Configurations.IMMUNITY_PERIOD_MINUTES(),
            ChronoUnit.MINUTES
    ).toEpochMilli();

    private User testUser;

    @BeforeAll
    static void BeforeAllTest() {
        mockedSettings = Mockito.mockStatic(Configurations.class);
        mockedSettings.when(Configurations::INACTIVITY_DAYS).thenReturn(10);
        mockedSettings.when(Configurations::IMMUNITY_PERIOD_MINUTES).thenReturn(60);
    }

    @AfterAll()
    static void AfterAllTest() {
        mockedSettings.close();
    }

    @BeforeEach
    void createTestUser() {
        this.testUser = new User(
                userName,
                realmName,
                userId,
                lastLogin,
                created,
                userEnabled,
                manuallyTime,
                userComment
        );
    }

    @AfterEach
    void cleanUser() {
        this.testUser = null;
    }

    @Test
    void testUser_WhenUserShouldBeBlocked_ShouldReturnTrue() {
        Assert.isTrue(testUser.userShouldBeBlocked(), "user should not be blocked");
    }

    @Test
    void testUser_WhenUserIsOld_ShouldReturnTrue() {
        Assert.isTrue(testUser.userIsOld(), "user is not old");
    }

    @Test
    void testUser_WhenUserIsInactive_ShouldReturnTrue() {
        Assert.isTrue(testUser.userIsInactive(), "user is not inactive");
    }

    @Test
    void testUser_WhenUserIsNotInImmunityPeriod_ShouldReturnTrue() {
        Assert.isTrue(testUser.userIsNotInImmunityPeriod(), "user is in immunity period");
    }

    @Test
    void testEqualsEqual() {
        final User testUser2 = new User(
                userName,
                realmName,
                userId,
                lastLogin,
                created,
                userEnabled,
                manuallyTime,
                userComment
        );
        Assert.isTrue(testUser2.equals(testUser), "testUser should be equal to testUser2");
    }

    @Test
    void testEqualsNotEqual() {
        final User testUser2 = new User(
                userName + "2",
                realmName + "2",
                userId + "2",
                lastLogin,
                created,
                !userEnabled,
                manuallyTime,
                userComment + "2"
        );
        Assert.isTrue(!testUser2.equals(testUser), "testUser should NOT be equal to testUser2");
    }

    @Test
    void testHashCode() {
        final User testUserHash = new User(
                userName,
                realmName,
                userId,
                lastLogin,
                created,
                userEnabled,
                manuallyTime,
                userComment
        );
        int hash = testUser.hashCode();
        int hash2 = testUserHash.hashCode();
        Assert.isTrue(hash == hash2, "Hash codes do not match (" + hash + "!=" + hash2 + ")");
    }

    @Test
    void testToString() {
        final User testUser2 = new User(
                userName,
                realmName,
                userId,
                lastLogin,
                created,
                userEnabled,
                manuallyTime,
                userComment
        );
        String string = testUser.toString();
        String string2 = testUser2.toString();
        Assert.isTrue(string.equals(string2), string + " not equal to " + string2);
    }

    @Test
    void getUserName() {
        Assert.isTrue(userName.equals(testUser.getUserName()),
                "user name (" + testUser.getUserName() + ") of user is not equal to " + userName);
    }

    @Test
    void getRealmName() {
        Assert.isTrue(realmName.equals(testUser.getRealmName()),
                "realm name (" + testUser.getRealmName() + ") of user is not equal to " + realmName);
    }

    @Test
    void getUserId() {
        Assert.isTrue(userId.equals(testUser.getUserId()),
                "userId (" + testUser.getUserId() + ") of user is not equal to " + userId);
    }

    @Test
    void getLastLogin() {
        Assert.isTrue(lastLogin.equals(testUser.getLastLogin()),
                "lastLogin (" + testUser.getLastLogin() + ") of user is not equal to " + lastLogin);
    }

    @Test
    void getCreated() {
        Assert.isTrue(created.equals(testUser.getCreated()),
                "creation time (" + testUser.getCreated() + ") of user is not equal to " + created);
    }

    @Test
    void getEnabled() {
        Assert.isTrue(userEnabled == testUser.getEnabled(),
                "enabled (" + testUser.getEnabled() + ") of user is not equal to " + userEnabled);
    }

    @Test
    void getManuallyEnabledTime() {
        Assert.isTrue(manuallyTime.equals(testUser.getManuallyEnabledTime()),
                "manuallyEnabledTime (" + testUser.getManuallyEnabledTime() +
                        ") of user is not equal to " + manuallyTime);
    }

    @Test
    void getComment() {
        Assert.isTrue(userComment.equals(testUser.getComment()),
                "comment (" + testUser.getComment() + ") of user is not equal to " + userComment);
    }

    @Test
    void setUserName() {
        String newUserName = userName + " test";
        testUser.setUserName(newUserName);
        Assert.isTrue(newUserName.equals(testUser.getUserName()),
                "user name (" + testUser.getUserName() + ") of user is not equal to " + newUserName);
    }

    @Test
    void setRealmName() {
        String newRealmName = realmName + " test";
        testUser.setRealmName(newRealmName);
        Assert.isTrue(newRealmName.equals(testUser.getRealmName()),
                "realm name (" + testUser.getRealmName() + ") of user is not equal to " + newRealmName);
    }

    @Test
    void setUserId() {
        String newUserId = userId + " test";
        testUser.setUserId(newUserId);
        Assert.isTrue(newUserId.equals(testUser.getUserId()),
                "userId (" + testUser.getUserId() + ") of user is not equal to " + userId);
    }

    @Test
    void setLastLogin() {
        Long newLastLogin = Instant.now().toEpochMilli();
        testUser.setLastLogin(newLastLogin);
        Assert.isTrue(newLastLogin.equals(testUser.getLastLogin()),
                "last login (" + testUser.getLastLogin() + ") of user is not equal to " + newLastLogin);
    }

    @Test
    void setCreated() {
        Long newCreated = Instant.now().toEpochMilli();
        testUser.setCreated(newCreated);
        Assert.isTrue(newCreated.equals(testUser.getCreated()),
                "created (" + testUser.getCreated() + ") of user is not equal to " + newCreated);
    }

    @Test
    void setEnabled() {
        boolean newEnabled = !userEnabled;
        testUser.setEnabled(newEnabled);
        Assert.isTrue(newEnabled == testUser.getEnabled(),
                "enabled (" + testUser.getEnabled() + ") of user is not equal to " + newEnabled);
    }

    @Test
    void setManuallyEnabledTime() {
        Long newEnabledTime = Instant.now().toEpochMilli();
        testUser.setManuallyEnabledTime(newEnabledTime);
        Assert.isTrue(newEnabledTime.equals(testUser.getManuallyEnabledTime()),
                "manually enabled time (" + testUser.getManuallyEnabledTime() +
                        ") of user is not equal to " + newEnabledTime);
    }

    @Test
    void setComment() {
        String newComment = userComment + " test";
        testUser.setComment(newComment);
        Assert.isTrue(newComment.equals(testUser.getComment()),
                "comment (" + testUser.getComment() + ") of user is not equal to " + newComment);
    }
}
