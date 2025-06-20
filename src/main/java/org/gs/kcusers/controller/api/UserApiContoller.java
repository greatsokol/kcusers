package org.gs.kcusers.controller.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.gs.kcusers.controller.CommonController;
import org.gs.kcusers.domain.User;
import org.gs.kcusers.repositories.UserRepository;
import org.gs.kcusers.service.KeycloakClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserApiContoller extends CommonController {
    @Autowired
    protected UserRepository userRepository;
    KeycloakClient keycloakClient;

    @Autowired
    public UserApiContoller(KeycloakClient keycloakClient) {
        this.keycloakClient = keycloakClient;
    }

    @PreAuthorize("hasAnyAuthority(@getUserRoles)")
    @GetMapping(path = "/{realmName}/{userName}")
    public String userPage(@PathVariable String realmName, @PathVariable String userName) {
        saveLoginEvent();
        UserApiResponse response = new UserApiResponse(
                getPrincipal(),
                userRepository.findByUserNameAndRealmName(userName, realmName)
        );

        ObjectWriter ow = new ObjectMapper().writer();
        try {
            return ow.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority(@getAdminRoles)")
    @PostMapping(path = "/{realmName}/{userName}")
    public String putUser(@PathVariable String realmName,
                          @PathVariable String userName,
                          @RequestBody MultiValueMap<String, String> formData) {
        saveLoginEvent();
        User user = userRepository.findByUserNameAndRealmName(userName, realmName);

        String wantedEnabled = formData.getFirst("enabled");
        boolean enabled = wantedEnabled != null && wantedEnabled.equals("true");
        user.setUserStatusFromController(enabled, getAuthorizedUserName());
        keycloakClient.updateUserFromController(user, getAuthorizedUserName());

        return userPage(realmName, userName);
    }

    @Data
    @AllArgsConstructor
    static private class UserApiResponse {
        Principal principal;
        User payload;
    }
}
