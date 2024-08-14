/*
 * Created by Eugene Sokolov 05.08.2024, 11:41.
 */

package org.gs.kcusers.domain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

class LoginTest {

    private Login testLogin;
    private String userName = "admin name";
    private Long authTime = Instant.now().toEpochMilli();
    private String session = "admin session";
    private String address = "admin ip address";

    @BeforeEach
    void setUp() {
        testLogin = new Login(
                userName,
                authTime,
                session,
                address
        );
    }

    @AfterEach
    void tearDown() {
        testLogin = null;
    }

    @Test
    void getUserName() {
        Assert.isTrue(userName.equals(testLogin.getUserName()),
                "user name (" + testLogin.getUserName() + ") of login is not equal to " + userName);
    }

    @Test
    void getAuthTime() {
        Assert.isTrue(authTime.equals(testLogin.getAuthTime()),
                "auth time (" + testLogin.getAuthTime() + ") of login is not equal to " + authTime);
    }

    @Test
    void getSession() {
        Assert.isTrue(session.equals(testLogin.getSession()),
                "session id (" + testLogin.getSession() + ") of login is not equal to " + session);
    }

    @Test
    void getAddress() {
        Assert.isTrue(address.equals(testLogin.getAddress()),
                "ip address (" + testLogin.getAddress() + ") of login is not equal to " + address);
    }

    @Test
    void setUserName() {
        String newUserName = userName + " test";
        testLogin.setUserName(newUserName);
        Assert.isTrue(newUserName.equals(testLogin.getUserName()),
                "user name (" + testLogin.getUserName() + ") of login is not equal to " + newUserName);
    }

    @Test
    void setAuthTime() {
        Long newAuthTime = Instant.now().plus(60, ChronoUnit.MINUTES).toEpochMilli();
        testLogin.setAuthTime(newAuthTime);
        Assert.isTrue(newAuthTime.equals(testLogin.getAuthTime()),
                "auth time (" + testLogin.getAuthTime() + ") of login is not equal to " + newAuthTime);
    }

    @Test
    void setSession() {
        String newSession = session + " test";
        testLogin.setSession(newSession);
        Assert.isTrue(newSession.equals(testLogin.getSession()),
                "session id (" + testLogin.getSession() + ") of login is not equal to " + newSession);
    }

    @Test
    void setAddress() {
        String newAddress = address + " test";
        testLogin.setAddress(newAddress);
        Assert.isTrue(newAddress.equals(testLogin.getAddress()),
                "ip address (" + testLogin.getAddress() + ") of login is not equal to " + newAddress);
    }

    @Test
    void testEquals() {
        Login testLogin2 = new Login(
                userName,
                authTime,
                session,
                address
        );
        Assert.isTrue(testLogin2.equals(testLogin), testLogin2 + " not equal to " + testLogin);
    }

    @Test
    void testNotEquals() {
        Login testLogin2 = new Login(
                userName + "2",
                Instant.ofEpochSecond(authTime).minus(60, ChronoUnit.MINUTES).toEpochMilli(),
                session + "2",
                address + "2"
        );
        Assert.isTrue(!testLogin2.equals(testLogin), testLogin2 + " should NOT be equal to " + testLogin);
    }

    @Test
    void testHashCode() {
        Login testLogin2 = new Login(
                userName,
                authTime,
                session,
                address
        );
        int testHash2 = testLogin2.hashCode();
        int testHash = testLogin.hashCode();
        Assert.isTrue(testHash == testHash2,
                "Hash code " + testHash +
                        " not equal to hash code " + testHash2);
    }

    @Test
    void testToString() {
        Login testLogin2 = new Login(
                userName,
                authTime,
                session,
                address
        );

        String string = testLogin.toString();
        String string2 = testLogin2.toString();
        Assert.isTrue(string.equals(string2), string + " not equal to " + string2);
    }
}
