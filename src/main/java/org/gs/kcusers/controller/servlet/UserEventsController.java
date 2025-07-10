/*
 * Created by Eugene Sokolov 21.06.2024, 12:08.
 */

package org.gs.kcusers.controller.servlet;

import org.gs.kcusers.controller.CommonController;
import org.gs.kcusers.repositories.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

import static org.gs.kcusers.utils.Utils.getAuthorizedUserName;

@Controller
@RequestMapping("/events")
public class UserEventsController extends CommonController {
    @Autowired
    protected EventRepository eventRepository;

    @PreAuthorize("hasAnyAuthority(@getUserRoles)")
    @GetMapping("/{realmName}/{userName}")
    public String eventsPage(@PathVariable String realmName, @PathVariable String userName,
                             Map<String, Object> model, @PageableDefault Pageable pagable) {
        model.put("page", eventRepository.findByUserNameAndRealmNameOrderByCreatedDesc(
                userName,
                realmName,
                pagable));
        model.put("userName", userName);
        model.put("realmName", realmName);
        model.put("authorizedusername", getAuthorizedUserName());

        return "eventspage";
    }
}
