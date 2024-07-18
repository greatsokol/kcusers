/*
 * Created by Eugene Sokolov 21.06.2024, 12:08.
 */

package org.gs.kcusers.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.gs.kcusers.domain.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@EnableWebSecurity
@RestController
@RequestMapping("/api/events")
public class UserEventsApiController extends CommonController {

    @PreAuthorize("hasAnyAuthority(@getUserRoles)")
    @GetMapping("/{realmName}/{userName}")
    public String eventsPage(@PathVariable String realmName, @PathVariable String userName,
                             @PageableDefault Pageable pagable) {

        UserEventsApiResponse response = new UserEventsApiResponse(
                getPrincipal(),
                eventRepository.findByUserNameAndRealmNameOrderByCreatedDesc(
                        userName,
                        realmName,
                        pagable)
        );

        ObjectWriter ow = new ObjectMapper().writer();
        try {
            return ow.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Data
    @AllArgsConstructor
    static private class UserEventsApiResponse {
        Principal principal;
        Page<Event> payload;
    }
}
