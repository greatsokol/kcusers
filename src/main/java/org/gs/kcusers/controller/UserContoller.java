package org.gs.kcusers.controller;

import org.gs.kcusers.domain.Event;
import org.gs.kcusers.domain.User;
import org.gs.kcusers.service.KeycloakClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@EnableWebSecurity
@Controller
@RequestMapping("/user")
public class UserContoller extends CommonController {
    KeycloakClient keycloakClient;

    @Autowired
    public UserContoller(KeycloakClient keycloakClient) {
        this.keycloakClient = keycloakClient;
    }

    @PreAuthorize("hasAnyAuthority(@getUserRoles)")
    @GetMapping(path = "/{realmName}/{userName}")
    public String userPage(@PathVariable String realmName, @PathVariable String userName, Map<String, Object> model) {
        var user = userRepository.findByUserNameAndRealmName(userName, realmName);
        model.put("user", user);
        model.put("authorizedusername", getAuthorizedUserName());
        return "userpage";
    }

    @PreAuthorize("hasAnyAuthority(@getAdminRoles)")
    @PostMapping(path = "/{realmName}/{userName}")
    public String putUser(@PathVariable String realmName,
                          @PathVariable String userName,
                          @RequestBody MultiValueMap<String, String> formData,
                          Map<String, Object> model) {
        User user = userRepository.findByUserNameAndRealmName(userName, realmName);
        boolean wantedEnabled = formData.getFirst("enabled") != null;
        user.setUserStatusFromController(wantedEnabled, getAuthorizedUserName());
        eventRepository.save(new Event(user.getUserName(), user.getRealmName(), Instant.now().toEpochMilli(), getAuthorizedUserName(),
                user.getComment(), user.getEnabled(), false));
        keycloakClient.updateUserFromController(user);
        model.put("user", user);
        model.put("authorizedusername", getAuthorizedUserName());

        return "userpage";
    }
}
