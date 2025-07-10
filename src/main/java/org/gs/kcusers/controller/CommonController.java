package org.gs.kcusers.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.gs.kcusers.domain.Audit;
import org.gs.kcusers.domain.Login;
import org.gs.kcusers.repositories.AuditRepository;
import org.gs.kcusers.repositories.LoginRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.gs.kcusers.utils.Utils.*;

public class CommonController {
    @Value("${front.adminroles}")
    protected String adminRoles;
    @Value("${front.userroles}")
    protected String userRoles;

    @Autowired
    protected LoginRepository loginRepository;
    @Autowired
    protected AuditRepository auditRepository;

    protected CommonController() {

    }

    CommonController(String adminRoles, String userRoles) {
        this.adminRoles = adminRoles;
        this.userRoles = userRoles;
    }

    protected void saveLoginEvent() {

        String userName = getAuthorizedUserName();
        String sessionId = getAuthorizedUserJwtSessionId();
        if (!loginRepository.existsBySessionEqualsIgnoreCaseAndUserNameEqualsIgnoreCase(sessionId, userName)) {
            auditRepository.save(new Audit(
                    Audit.ENT_API,
                    Audit.SUBTYPE_SUCCESS,
                    HttpStatus.OK.value(),
                    "login success")
            );

            WebAuthenticationDetails authDetails = getAuthDetails();
            loginRepository.save(new Login(
                    userName,
                    Instant.now().toEpochMilli(),
                    sessionId,
                    authDetails == null ? "" : authDetails.getRemoteAddress())
            );
        }
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


    protected Boolean userRolesGranted() {
        var list = grantedAuthoritiesList();
        if (list == null) return false;
        return list.stream().anyMatch(getUserRoles()::contains);
    }

    protected Boolean adminRolesGranted() {
        var list = grantedAuthoritiesList();
        if (list == null) return false;
        return list.stream().anyMatch(getAdminRoles()::contains);
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
