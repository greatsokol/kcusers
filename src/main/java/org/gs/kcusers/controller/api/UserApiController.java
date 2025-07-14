package org.gs.kcusers.controller.api;

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

import static org.gs.kcusers.utils.Utils.getAuthorizedUserName;

@RestController
@RequestMapping("/api/user")
public class UserApiController extends CommonController {
    protected UserRepository userRepository;
    KeycloakClient keycloakClient;

    @Autowired
    public UserApiController(KeycloakClient keycloakClient, UserRepository userRepository) {
        this.keycloakClient = keycloakClient;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasAnyAuthority(@getUserRoles)")
    @GetMapping(path = "/{realmName}/{userName}")
    public UserApiResponse userPage(@PathVariable String realmName, @PathVariable String userName, boolean successResult) {
        saveLoginEvent();

        var response = new UserApiResponse(
                getPrincipal(),
                userRepository.findByUserNameAndRealmName(userName, realmName)
        );

        if (!successResult) {
            response.payload.setComment("Ошибка операции");
        }
        return response;
    }

    @PreAuthorize("hasAnyAuthority(@getAdminRoles)")
    @PostMapping(path = "/{realmName}/{userName}")
    public UserApiResponse putUser(@PathVariable String realmName,
                                   @PathVariable String userName,
                                   @RequestBody MultiValueMap<String, String> formData) {
        saveLoginEvent();
        User user = userRepository.findByUserNameAndRealmName(userName, realmName);
        User tmpUser = new User(user);


        String wantedEnabled = formData.getFirst("enabled");
        boolean enabled = wantedEnabled != null && wantedEnabled.equals("true");
        String adminName = getAuthorizedUserName();
        tmpUser.setUserStatusFromController(enabled, adminName);
        if (keycloakClient.updateUserFromController(tmpUser, adminName)) {
            user.setUserStatusFromController(enabled, adminName);
            return userPage(realmName, userName, true);
        }
        return userPage(realmName, userName, false);
    }

    @Data
    @AllArgsConstructor
    static public class UserApiResponse {
        Principal principal;
        User payload;
    }
}
