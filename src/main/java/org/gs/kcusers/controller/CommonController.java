package org.gs.kcusers.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.gs.kcusers.repositories.EventRepository;
import org.gs.kcusers.repositories.LoginRepository;
import org.gs.kcusers.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class CommonController {
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected EventRepository eventRepository;
    @Autowired
    protected LoginRepository loginRepository;
    @Value("${front.adminroles}")
    protected String adminRoles;
    @Value("${front.userroles}")
    protected String userRoles;

//    public static CsrfToken getCurrentCsrfToken() {
//        // quick-test
//        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
//        HttpSession session = attr.getRequest().getSession(false);
//        if (session == null) {
//            return null;
//        }
//        return (CsrfToken) session.getAttribute("org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN");
//    }

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
            return (List<String>) jwt.
                    getClaimAsMap("realm_access").
                    get("roles");
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
        WebAuthenticationDetails authDeatails = (WebAuthenticationDetails) SecurityContextHolder
                .getContext().getAuthentication().getDetails();
        return authDeatails;
    }

    @Data
    @AllArgsConstructor
    static class Principal {
        String userName;
        boolean admin;
        boolean user;
    }
}
