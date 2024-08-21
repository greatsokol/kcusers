package org.gs.kcusers.controller;

import org.gs.kcusers.domain.User;
import org.gs.kcusers.repositories.UserRepository;
import org.gs.kcusers.service.KeycloakClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@EnableWebSecurity
@Controller
@RequestMapping("/user")
public class UserContoller extends CommonController {
    @Autowired
    protected UserRepository userRepository;
    KeycloakClient keycloakClient;

    @Autowired
    public UserContoller(KeycloakClient keycloakClient) {
        this.keycloakClient = keycloakClient;
    }

    private void fillModel(Map<String, Object> model, User user) {
        model.put("user", user);
        model.put("isAdmin", adminRolesGranted());
        model.put("authorizedusername", getAuthorizedUserName());
    }

    @PreAuthorize("hasAnyAuthority(@getUserRoles)")
    @GetMapping(path = "/{realmName}/{userName}")
    public String userPage(@PathVariable String realmName, @PathVariable String userName, Map<String, Object> model) {
        User user = userRepository.findByUserNameAndRealmName(userName, realmName);
        fillModel(model, user);
        return "userpage";
    }

    @PreAuthorize("hasAnyAuthority(@getAdminRoles)")
    @PostMapping(path = "/{realmName}/{userName}")
    public String putUser(@PathVariable String realmName,
                          @PathVariable String userName,
                          @RequestBody MultiValueMap<String, String> formData,
                          Map<String, Object> model) {
        User user = userRepository.findByUserNameAndRealmName(userName, realmName);
        String wantedEnabled = formData.getFirst("enabled");
        boolean enabled = wantedEnabled != null && wantedEnabled.equals("true");
        if (!user.getEnabled().equals(enabled)) {
            user.setUserStatusFromController(enabled, getAuthorizedUserName());
            keycloakClient.updateUserFromController(user, getAuthorizedUserName());
        }
        fillModel(model, user);

        return "userpage";
    }
}
