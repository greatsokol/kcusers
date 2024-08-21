package org.gs.kcusers.controller;

import org.gs.kcusers.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@EnableWebSecurity
@Controller
@RequestMapping("/")
public class UsersController extends CommonController {
    @Autowired
    protected UserRepository userRepository;

    @PreAuthorize("hasAnyAuthority(@getUserRoles)")
    @GetMapping
    public String usersPage(Map<String, Object> model, @PageableDefault Pageable pagable) {
        model.put("page", userRepository.findAllByOrderByRealmNameAscUserNameAsc(pagable));
        model.put("realms", userRepository.finaAllRealmNames());
        model.put("authorizedusername", getAuthorizedUserName());

        return "userspage";
    }

    @PreAuthorize("hasAnyAuthority(@getUserRoles)")
    @GetMapping(params = "filter")
    public String usersPageFiltered(@RequestParam String filter,
                                    Map<String, Object> model,
                                    @PageableDefault Pageable pagable) {
        if (filter.isEmpty()) {
            return usersPage(model, pagable);
        }

        if (filter.length() > 20) {
            filter = filter.substring(0, 20);
        }

        model.put("filter", filter);
        model.put("page", userRepository.findByUserNameContainingOrderByRealmNameAscUserNameAsc(filter, pagable));
        model.put("realms", userRepository.finaAllRealmNamesContaining(filter));
        model.put("authorizedusername", getAuthorizedUserName());

        return "userspage";
    }
}
