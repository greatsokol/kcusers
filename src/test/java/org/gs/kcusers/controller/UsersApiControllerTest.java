/*
 * Created by Eugene Sokolov 12.08.2024, 14:02.
 */

package org.gs.kcusers.controller;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsersApiController.class)
class UsersApiControllerTest {
    final int total = 10;
    private final String authorizedUserName = "Admin Name";
    @MockBean
    UserRepository userRepository;

    @MockBean
    User user;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(userRepository.findAllByOrderByRealmNameAscUserNameAsc(any(Pageable.class)))
                .thenAnswer(invocationOnMock -> new PageImpl<>(
                        Collections.nCopies(total, user),
                        invocationOnMock.getArgument(0, Pageable.class), total));

        when(userRepository.findByUserNameContainingOrderByRealmNameAscUserNameAsc(anyString(), any(Pageable.class)))
                .thenAnswer(invocationOnMock -> new PageImpl<>(
                        Collections.nCopies(total, user),
                        invocationOnMock.getArgument(1, Pageable.class), total));
    }

    @Test
    @WithMockUser(username = authorizedUserName)
    void getUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.content").isArray())
                .andExpect(jsonPath("$.payload.content.length()").value(total))
                .andExpect(jsonPath("$.payload.totalElements").value(total))
                .andExpect(jsonPath("$.principal.userName").value(authorizedUserName));
    }

    @Test
    @WithMockUser(username = authorizedUserName)
    void getUsersFiltered() throws Exception {
        final String userName = "username";
        mockMvc.perform(get("/api/users?filter="+userName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filter").value(userName))
                .andExpect(jsonPath("$.payload.content").isArray())
                .andExpect(jsonPath("$.payload.content.length()").value(total))
                .andExpect(jsonPath("$.payload.totalElements").value(total))
                .andExpect(jsonPath("$.principal.userName").value(authorizedUserName));
    }
}
