/*
 * Created by Eugene Sokolov 05.08.2024, 13:10.
 */

package org.gs.kcusers.controller;

import org.gs.kcusers.controller.servlet.AccessDeniedController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccessDeniedController.class)
class AccessDeniedControllerTest {
    private final String authorizedUserName = "Admin Name";

    @Autowired
    MockMvc mockMvc;

    @Test
    @WithMockUser(username = authorizedUserName)
    void accessDenied() throws Exception {
        mockMvc.perform(get("/access-denied"))
                .andExpect(status().isOk())
                .andExpect(view().name("access-denied"))
                .andExpect(model().attribute("authorizedusername", authorizedUserName));
    }
}
