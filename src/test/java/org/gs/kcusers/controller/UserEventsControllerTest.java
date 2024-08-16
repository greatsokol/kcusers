/*
 * Created by Eugene Sokolov 09.08.2024, 11:00.
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserEventsController.class)
class UserEventsControllerTest {
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
                anyString(), anyString(), any(Pageable.class))).
                thenReturn(new PageImpl<>(Collections.nCopies(10, event)));
    }

    @Test
    @WithMockUser(username = authorizedUserName)
    void eventsPage() throws Exception {
        mockMvc.perform(get("/events/" + realmName + "/" + userName))
                .andExpect(status().isOk())
                .andExpect(view().name("eventspage"))
                .andExpect(model().attributeExists("page"))
                .andExpect(model().attribute("userName", userName))
                .andExpect(model().attribute("realmName", realmName))
                .andExpect(model().attribute("authorizedusername", authorizedUserName));
    }
}
