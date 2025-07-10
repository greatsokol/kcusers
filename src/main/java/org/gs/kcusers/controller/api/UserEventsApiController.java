/*
 * Created by Eugene Sokolov 21.06.2024, 12:08.
 */

package org.gs.kcusers.controller.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.gs.kcusers.controller.CommonController;
import org.gs.kcusers.domain.Event;
import org.gs.kcusers.repositories.EventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class UserEventsApiController extends CommonController {
    protected EventRepository eventRepository;

    UserEventsApiController(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @PreAuthorize("hasAnyAuthority(@getUserRoles)")
    @GetMapping("/{realmName}/{userName}")
    public UserEventsApiResponse eventsPage(@PathVariable String realmName, @PathVariable String userName,
                                            @PageableDefault Pageable pagable) {
        saveLoginEvent();
        return new UserEventsApiResponse(
                getPrincipal(),
                eventRepository.findByUserNameAndRealmNameOrderByCreatedDesc(
                        userName,
                        realmName,
                        pagable)
        );
    }

    @Data
    @AllArgsConstructor
    static public class UserEventsApiResponse {
        Principal principal;
        Page<Event> payload;
    }
}
