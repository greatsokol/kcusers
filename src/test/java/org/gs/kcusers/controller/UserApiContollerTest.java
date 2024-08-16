/*
 * Created by Eugene Sokolov 12.08.2024, 11:24.
 */

package org.gs.kcusers.controller;

import org.gs.kcusers.domain.User;
import org.gs.kcusers.repositories.UserRepository;
import org.gs.kcusers.service.KeycloakClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserApiContoller.class)
class UserApiContollerTest {
    private final String userName = "UserName";
    private final String authorizedUserName = "Admin Name";
    private final String realmName = "realmName";

    @MockBean
    UserRepository userRepository;

    @MockBean
    KeycloakClient keycloakClient;

    @MockBean
    User user;

    @Autowired
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        //ReflectionTestUtils.setField(user, "userName", userName);
        //ReflectionTestUtils.setField(user, "realmName", realmName);
        when(user.getUserName()).thenReturn(userName);
        when(user.getRealmName()).thenReturn(realmName);
        when(userRepository.findByUserNameAndRealmName(anyString(), anyString())).thenReturn(user);
        doNothing().when(keycloakClient).startPolling();
    }

    @Test
    @WithMockUser(username = authorizedUserName)
    void userPage() throws Exception {
        mockMvc.perform(get("/api/user/" + realmName + "/" + userName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.principal.userName").value(authorizedUserName))
                .andExpect(jsonPath("$.payload.userName").value(userName))
                .andExpect(jsonPath("$.payload.realmName").value(realmName));
    }

    @Test
    @WithMockUser(username = authorizedUserName)
    void putUser() throws Exception {
        when(keycloakClient.updateUserFromController(any(User.class), anyString())).thenReturn(true);
        doNothing().when(user).setUserStatusFromController(anyBoolean(), anyString());
        mockMvc.perform(post("/api/user/" + realmName + "/" + userName)
                        .with(csrf()).contentType("application/x-www-form-urlencoded")
                        .content("enabled=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.principal.userName").value(authorizedUserName))
                .andExpect(jsonPath("$.payload.userName").value(userName))
                .andExpect(jsonPath("$.payload.realmName").value(realmName));
    }
}
