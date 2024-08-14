/*
 * Created by Eugene Sokolov 05.08.2024, 10:07.
 */

package org.gs.kcusers.domain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

class EventTest {
    private final String userName = "User Name";
    private final String realmName = "testrealm";
    private final String admLogin = "admin";
    private final String comment = "test comment";
    private final boolean enabled = true;
    private final Long created = Instant.now().toEpochMilli();
    private Event testEvent;

    @BeforeEach
    void createTestEvent() {
        testEvent = new Event(
                userName,
                realmName,
                created,
                admLogin,
                comment,
                enabled
        );
    }

    @AfterEach
    void tearDown() {
        testEvent = null;
    }

    @Test
    void getUserName() {
        Assert.isTrue(userName.equals(testEvent.getUserName()),
                "user name (" + testEvent.getUserName() + ") of event is not equal to " + userName);
    }

    @Test
    void getRealmName() {
        Assert.isTrue(realmName.equals(testEvent.getRealmName()),
                "realm name (" + testEvent.getRealmName() + ") of event is not equal to " + realmName);
    }

    @Test
    void getCreated() {
        Assert.isTrue(created.equals(testEvent.getCreated()),
                "creation time (" + testEvent.getCreated() + ") of event is not equal to " + created);
    }

    @Test
    void getAdmLogin() {
        Assert.isTrue(admLogin.equals(testEvent.getAdmLogin()),
                "admLogin name (" + testEvent.getAdmLogin() + ") of event is not equal to " + admLogin);
    }

    @Test
    void getComment() {
        Assert.isTrue(comment.equals(testEvent.getComment()),
                "comment (" + testEvent.getComment() + ") of event is not equal to " + comment);
    }

    @Test
    void getEnabled() {
        Assert.isTrue(enabled == testEvent.getEnabled(),
                "enabled (" + testEvent.getEnabled() + ") of event is not equal to " + enabled);
    }

    @Test
    void setUserName() {
        String testUserName = userName + "  setUserName Test";
        testEvent.setUserName(testUserName);
        Assert.isTrue(testUserName.equals(testEvent.getUserName()),
                "user name (" + testEvent.getUserName() + ") of event is not equal to " + testUserName);
    }

    @Test
    void setRealmName() {
        String testRealmName = realmName + "  setRealmName Test";
        testEvent.setRealmName(testRealmName);
        Assert.isTrue(testRealmName.equals(testEvent.getRealmName()),
                "realm name (" + testEvent.getRealmName() + ") of event is not equal to " + testRealmName);
    }

    @Test
    void setCreated() {
        Long testCreated = Instant.now().plus(60, ChronoUnit.MINUTES).toEpochMilli();
        testEvent.setCreated(testCreated);
        Assert.isTrue(testCreated.equals(testEvent.getCreated()),
                "creation time (" + testEvent.getCreated() + ") of event is not equal to " + testCreated);
    }

    @Test
    void setAdmLogin() {
        String testAdmLogin = admLogin + "  setAdmLogin Test";
        testEvent.setAdmLogin(testAdmLogin);
        Assert.isTrue(testAdmLogin.equals(testEvent.getAdmLogin()),
                "admLogin name (" + testEvent.getAdmLogin() + ") of event is not equal to " + testAdmLogin);
    }

    @Test
    void setComment() {
        String testComment = comment + "  setComment Test";
        testEvent.setComment(testComment);
        Assert.isTrue(testComment.equals(testEvent.getComment()),
                "comment (" + testEvent.getComment() + ") of event is not equal to " + comment);
    }

    @Test
    void setEnabled() {
        Boolean testEnabled = !enabled;
        testEvent.setEnabled(testEnabled);
        Assert.isTrue(testEnabled == testEvent.getEnabled(),
                "enabled (" + testEvent.getEnabled() + ") of event is not equal to " + testEnabled);
    }

    @Test
    void testEqualsNotEqual() {
        final Event testEvent2 = new Event(
                userName + "2",
                realmName + "2",
                Instant.ofEpochSecond(created).plus(60, ChronoUnit.MINUTES).toEpochMilli(),
                admLogin + "2",
                comment + "2",
                !enabled
        );
        Assert.isTrue(!testEvent.equals(testEvent2), "testEvent equals to testEvent2");
    }

    @Test
    void testEqualsEqual() {
        final Event testEventEqual = new Event(
                userName,
                realmName,
                created,
                admLogin,
                comment,
                enabled
        );
        Assert.isTrue(testEvent.equals(testEventEqual), "testEvent not equals to testEventEqual");
    }

    @Test
    void testHashCode() {
        final Event testEventHash = new Event(
                userName,
                realmName,
                created,
                admLogin,
                comment,
                enabled
        );
        int hash = testEvent.hashCode();
        int hash2 = testEventHash.hashCode();
        Assert.isTrue(hash == hash2, "Hash codes do not match (" + hash + "!=" + hash2 + ")");
    }

    @Test
    void testToString() {
        final Event testEvent2 = new Event(
                userName,
                realmName,
                created,
                admLogin,
                comment,
                enabled
        );
        String string = testEvent.toString();
        String string2 = testEvent2.toString();
        Assert.isTrue(string.equals(string2), string + " not equal to " + string2);
    }
}
