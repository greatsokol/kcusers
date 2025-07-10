/*
 * Created by Eugene Sokolov 07.08.2024, 09:41.
 */

package org.gs.kcusers.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class CommonControllerTest {
    private final String adminRoles = "admin1,admin2";
    private final String userRoles = "user1,user2";
    private final String userName = "username";
    private final String adminAuthority = "admin1";
    private final String userAuthority = "user1";

    private CommonController testController;

    @BeforeEach
    void setUp() {
        testController = new CommonController(adminRoles, userRoles);
    }

    @AfterEach
    void tearDown() {
        testController = null;
    }

    @Test
    void getUserRoles() {
        var roles = testController.getUserRoles();
        assertNotNull(roles);
        var allRoles = userRoles + "," + adminRoles;
        Assert.isTrue(roles.equals(Arrays.asList(allRoles.split(","))),
                "user roles (" + roles + ") not equal to " + allRoles);
    }

    @Test
    void getAdminRoles() {
        var roles = testController.getAdminRoles();
        assertNotNull(roles);
        Assert.isTrue(roles.equals(Arrays.asList(adminRoles.split(","))),
                "user roles (" + roles + ") not equal to " + adminRoles);
    }

    @Test
    @WithMockUser(username = userName)
    void getAuthorizedUserName() {
        var principalName = org.gs.kcusers.utils.Utils.getAuthorizedUserName();
        Assert.isTrue(principalName.equals(userName),
                "principal name (" + principalName + ") not equal to " + userName);
    }

    @Test
    @WithMockUser(authorities = {
            adminAuthority,
            userAuthority
    })
    void grantedAuthoritiesList() {
        var authorities = org.gs.kcusers.utils.Utils.grantedAuthoritiesList();
        assertNotNull(authorities);
        List<String> testAuthorities = Arrays.asList(adminAuthority, userAuthority);
        Assert.isTrue(
                authorities.equals(testAuthorities),
                "authorities " + authorities + " not equal to " + testAuthorities
        );
    }

    @Test
    @WithMockUser(authorities = {
            adminAuthority,
            userAuthority
    })
    void grantedAuthoritiesListAsString() {
        var authorities = org.gs.kcusers.utils.Utils.grantedAuthoritiesListAsString();
        String testAuthorities = adminAuthority + ", " + userAuthority;
        Assert.isTrue(
                authorities.equals(testAuthorities),
                "authorities " + authorities + " not equal to " + testAuthorities
        );
    }

    @Test
    @WithMockUser(authorities = {
            "disposableAuthority",
    })
    void userRolesWasNotGranted() {
        Assert.isTrue(!testController.userRolesGranted(),
                "user role was granted with disposable authority");
    }

    @Test
    @WithMockUser(authorities = {
            userAuthority,
    })
    void userRolesGrantedWithUserAuthority() {
        Assert.isTrue(testController.userRolesGranted(),
                "user role was not granted with user authority");
    }

    @Test
    @WithMockUser(authorities = {
            adminAuthority
    })
    void userRolesGrantedWithAdminAuthority() {
        Assert.isTrue(testController.userRolesGranted(),
                "user role was not granted with admin authority");
    }

    @Test
    @WithMockUser(authorities = {
            "disposableAuthority",
    })
    void adminRolesWasNotGranted() {
        Assert.isTrue(!testController.adminRolesGranted(),
                "admin role was granted with disposable authority");
    }

    @Test
    @WithMockUser(authorities = {
            userAuthority,
    })
    void adminRolesWasNotGrantedWithUserAuthority() {
        Assert.isTrue(!testController.adminRolesGranted(),
                "admin role was granted with user authority");
    }

    @Test
    @WithMockUser(authorities = {
            adminAuthority
    })
    void adminRolesGrantedWithAdminAuthority() {
        Assert.isTrue(testController.adminRolesGranted(),
                "admin role was not granted with admin authority");
    }

    @Test
    @WithMockUser(
            username = userName,
            authorities = {
                    adminAuthority,
                    userAuthority
            })
    void getPrincipal() {
        var principal = testController.getPrincipal();
        assertNotNull(principal);
        Assert.isTrue(userName.equals(principal.userName),
                userName + " not equal to principal userName" + principal.userName);
        Assert.isTrue(principal.isAdmin(), principal + " is not admin");
        Assert.isTrue(principal.isUser(), principal + " is not user");
    }

    @Configuration
    public static class Props {

    }
}
