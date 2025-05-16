/*
 * Created by Eugene Sokolov 07.08.2024, 17:04.
 */

package org.gs.kcusers.controller;

import org.gs.kcusers.controller.servlet.UsersController;
import org.gs.kcusers.domain.User;
import org.gs.kcusers.repositories.UserRepository;
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

@WebMvcTest(UsersController.class)
class UsersControllerTest {
    private final String authorizedUserName = "Admin Name";

    @MockBean
    UserRepository userRepository;

    @MockBean
    User user;

    @Autowired
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(userRepository.findAllByOrderByRealmNameAscUserNameAsc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.nCopies(10, user)));

        when(userRepository.findByUserNameContainingOrderByRealmNameAscUserNameAsc(
                anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.nCopies(10, user)));
    }

    @Test
    @WithMockUser(username = authorizedUserName)
    void usersPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("userspage"))
                .andExpect(model().attributeExists("page"))
                .andExpect(model().attribute("authorizedusername", authorizedUserName))
        ;
    }

    @Test
    @WithMockUser(username = authorizedUserName)
    void usersPageFiltered() throws Exception {
        final String userName = "username";
        mockMvc.perform(get("/?filter="+userName))
                .andExpect(status().isOk())
                .andExpect(view().name("userspage"))
                .andExpect(model().attribute("filter", userName))
                .andExpect(model().attributeExists("page"))
                .andExpect(model().attribute("authorizedusername", authorizedUserName))
        ;
    }
}
