/*
 * Created by Eugene Sokolov 12.08.2024, 13:54.
 */

package org.gs.kcusers.controller;

import org.gs.kcusers.domain.Event;
import org.gs.kcusers.repositories.EventRepository;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserEventsApiController.class)
class UserEventsApiControllerTest {
    final int total = 10;
    private final String userName = "UserName";
    private final String authorizedUserName = "Admin Name";
    private final String realmName = "realmName";
    @MockBean
    EventRepository eventRepository;

    @MockBean
    Event event;

    @Autowired
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {

        when(eventRepository.findByUserNameAndRealmNameOrderByCreatedDesc(
                any(String.class), any(String.class), any(Pageable.class))
        ).thenAnswer(invocationOnMock -> new PageImpl<>(
                Collections.nCopies(total, event),
                invocationOnMock.getArgument(2, Pageable.class), total));
    }

    @Test
    @WithMockUser(username = authorizedUserName)
    void eventsPage() throws Exception {
        mockMvc.perform(get("/api/events/" + realmName + "/" + userName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.content").isArray())
                .andExpect(jsonPath("$.payload.content.length()").value(total))
                .andExpect(jsonPath("$.payload.totalElements").value(total))
                .andExpect(jsonPath("$.principal.userName").value(authorizedUserName));
    }
}
