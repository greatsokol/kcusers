/*
 * Created by Eugene Sokolov 09.08.2024, 14:16.
 */

package org.gs.kcusers.controller;

import org.gs.kcusers.controller.api.LoginsApiController;
import org.gs.kcusers.domain.Login;
import org.gs.kcusers.repositories.LoginRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoginsApiController.class)
class LoginsApiControllerTest {
    final int total = 10;
    private final String authorizedUserName = "Admin Name";
    @MockBean
    LoginRepository loginRepository;

    @MockBean
    Login login;

    @Autowired
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(loginRepository.findByUserNameOrderByAuthTimeDesc(
                anyString(), any(Pageable.class))
        ).thenAnswer(invocationOnMock -> new PageImpl<>(
                Collections.nCopies(total, login),
                invocationOnMock.getArgument(1, Pageable.class), total));
    }

    @Test
    @WithMockUser(username = authorizedUserName)
    void logins() throws Exception {
        mockMvc.perform(get("/api/logins/username"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.content").isArray())
                .andExpect(jsonPath("$.payload.content.length()").value(total))
                .andExpect(jsonPath("$.payload.totalElements").value(total))
                .andExpect(jsonPath("$.principal.userName").value(authorizedUserName));
    }

    @Test
    @WithMockUser(username = authorizedUserName)
    void addLogin() throws Exception {
        mockMvc.perform(post("/api/logins/username").with(csrf())
                        .contentType("application/x-www-form-urlencoded")
                        .content("sessionid=testsessionid"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }
}
