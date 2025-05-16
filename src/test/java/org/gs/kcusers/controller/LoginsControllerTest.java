/*
 * Created by Eugene Sokolov 07.08.2024, 15:56.
 */

package org.gs.kcusers.controller;

import org.gs.kcusers.controller.servlet.LoginsController;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoginsController.class)
class LoginsControllerTest {
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
        ).thenReturn(new PageImpl<>(Collections.nCopies(10, login)));
    }

    @Test
    @WithMockUser(username = authorizedUserName)
    void logins() throws Exception {
        mockMvc.perform(get("/logins/username"))
                .andExpect(status().isOk())
                .andExpect(view().name("loginspage"))
                .andExpect(model().attributeExists("page"))
                .andExpect(model().attribute("authorizedusername", authorizedUserName));
    }
}
