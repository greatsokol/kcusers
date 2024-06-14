package org.gs.kcusers.controller;

import org.gs.kcusers.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommonController {
    @Autowired
    protected UserRepository userRepository;

    @Value("${front.adminroles}")
    protected String adminRoles;

    @Value("${front.userroles}")
    protected String userRoles;

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
        DefaultOidcUser principal = (DefaultOidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal.getPreferredUsername();
    }

    protected List<String> grantedAuthoritiesList() {
        DefaultOidcUser principal = (DefaultOidcUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal.
                getAuthorities().
                stream().
                map(GrantedAuthority::getAuthority).
                toList();
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
}
