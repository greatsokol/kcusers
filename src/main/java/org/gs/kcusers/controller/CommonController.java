package org.gs.kcusers.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.gs.kcusers.configs.Configurations.ROLES_TOKEN_CLAIM_NAME;


public class CommonController {
    @Value("${front.adminroles}")
    protected String adminRoles;
    @Value("${front.userroles}")
    protected String userRoles;

    CommonController() {

    }

    CommonController(String adminRoles, String userRoles) {
        this.adminRoles = adminRoles;
        this.userRoles = userRoles;
    }

    @Bean
    protected List<String> getUserRoles() {
        List<String> roles1 = Arrays.asList(userRoles.split(","));
        List<String> roles2 = Arrays.asList(adminRoles.split(","));
        return Stream.concat(roles1.stream(), roles2.stream()).collect(Collectors.toList());
    }

    @Bean
    protected List<String> getAdminRoles() {
        return Arrays.asList(adminRoles.split(","));
    }

    protected String getAuthorizedUserName() {
        Object principalobject = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principalobject instanceof DefaultOidcUser principal) {
            return principal.getPreferredUsername();
        } else if (principalobject instanceof Jwt jwt) {
            return jwt.getClaimAsString("preferred_username");
        } else if (principalobject instanceof User user) {
            return user.getUsername();
        }
        return null;
    }

    protected List<String> grantedAuthoritiesList() {
        Object principalobject = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principalobject instanceof DefaultOidcUser principal) {
            return principal.
                    getAuthorities().
                    stream().
                    map(GrantedAuthority::getAuthority).
                    toList();
        } else if (principalobject instanceof Jwt jwt) {
            return jwt.getClaimAsStringList(ROLES_TOKEN_CLAIM_NAME);
            //getClaimAsMap("realm_access").
            //get("roles");
        } else if (principalobject instanceof User user) {
            return user.getAuthorities().
                    stream().
                    map(GrantedAuthority::getAuthority).
                    toList();
        } else return null;
    }

    protected String grantedAuthoritiesListAsString() {
        return String.join(", ", grantedAuthoritiesList());
    }

    protected Boolean userRolesGranted() {
        return grantedAuthoritiesList().stream().anyMatch(getUserRoles()::contains);
    }

    protected Boolean adminRolesGranted() {
        return grantedAuthoritiesList().stream().anyMatch(getAdminRoles()::contains);
    }


    protected Principal getPrincipal() {
        return new Principal(getAuthorizedUserName(), adminRolesGranted(), userRolesGranted());
    }

    protected WebAuthenticationDetails getAuthDetails() {
        var context = SecurityContextHolder.getContext();
        return (WebAuthenticationDetails) context.getAuthentication().getDetails();
    }

    @Data
    @AllArgsConstructor
    public static class Principal {
        String userName;
        boolean admin;
        boolean user;
    }
}
