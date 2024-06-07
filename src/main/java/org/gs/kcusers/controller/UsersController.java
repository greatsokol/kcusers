package org.gs.kcusers.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
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
    @Value("${front.pagesize}")
    private int pageSize;

    @PreAuthorize("hasAnyAuthority(@getUserRoles)")
    @GetMapping
    public String usersPage(Map<String, Object> model, @PageableDefault Pageable pagable) {
        Pageable pageable2 = PageRequest.of(pagable.getPageNumber(), pageSize, pagable.getSort());
        model.put("page", userRepository.findAllByOrderByRealmNameAscUserNameAsc(pageable2));
        model.put("authorizedusername", getAuthorizedUserName());

        return "userspage";
    }

    @PreAuthorize("hasAnyAuthority(@getUserRoles)")
    @GetMapping(params = "filter")
    public String usersPageFiltered(@RequestParam String filter,
                                    Map<String, Object> model,
                                    @PageableDefault Pageable pagable) {
        Pageable pageable2 = PageRequest.of(pagable.getPageNumber(), pageSize, pagable.getSort());
        if (filter.isEmpty()) {
            model.put("page", userRepository.findAllByOrderByRealmNameAscUserNameAsc(pageable2));
        } else {
            model.put("filter", filter);
            model.put("page", userRepository.findByUserNameContainingOrderByRealmNameAscUserNameAsc(filter, pageable2));
        }
        model.put("authorizedusername", getAuthorizedUserName());

        return "userspage";
    }
}
