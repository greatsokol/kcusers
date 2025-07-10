package org.gs.kcusers.utils;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.gs.kcusers.configs.yamlobjects.Configurations.KCUSERS_SCHEDULED_SERVICE;
import static org.gs.kcusers.configs.yamlobjects.Configurations.ROLES_TOKEN_CLAIM_NAME;

public class Utils {
    public static String getAuthorizedUserName(String defaultName) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            Object principalobject = auth.getPrincipal();
            if (principalobject instanceof DefaultOidcUser principal) {
                return principal.getPreferredUsername();
            } else if (principalobject instanceof Jwt jwt) {
                return jwt.getClaimAsString("preferred_username");
            } else if (principalobject instanceof User user) {
                return user.getUsername();
            }
        }
        return defaultName;
    }

    public static String getAuthorizedUserName() {
        return getAuthorizedUserName(null);
    }

    public static String getAuthorizedUserOrServiceName() {
        return getAuthorizedUserName(KCUSERS_SCHEDULED_SERVICE);
    }

    public static String getAuthorizedUserJwtSessionId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth == null) return null;
        var principalobject = auth.getPrincipal();
        if (principalobject instanceof Jwt jwt) {
            return jwt.getClaimAsString("sid");
        }
        return null;
    }

    public static List<String> grantedAuthoritiesList() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth == null) return null;
        var principalobject = auth.getPrincipal();
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

    public static String grantedAuthoritiesListAsString() {
        var list = grantedAuthoritiesList();
        if (list == null) return null;
        return String.join(", ", list);
    }

    public static Long getAuthorizedUserJwtIat() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth == null) return null;
        var principalobject = auth.getPrincipal();
        if (principalobject instanceof Jwt jwt) {
            return jwt.getClaimAsInstant("iat").toEpochMilli();
        }
        return null;
    }

    public static Long getAuthorizedUserJwtExp() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth == null) return null;
        var principalobject = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principalobject instanceof Jwt jwt) {
            return jwt.getClaimAsInstant("exp").toEpochMilli();
        }
        return null;
    }
}
