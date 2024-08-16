/*
 * Created by Eugene Sokolov 09.07.2024, 11:02.
 */

package org.gs.kcusers.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.gs.kcusers.domain.User;
import org.gs.kcusers.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@EnableWebSecurity
@RestController
@RequestMapping("/api")
public class UsersApiController extends CommonController {
    @Autowired
    protected UserRepository userRepository;

    private String responseToJson(UsersApiResponse response) {
        ObjectWriter ow = new ObjectMapper().writer();//.withDefaultPrettyPrinter();
        try {
            return ow.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority(@getUserRoles)")
    @GetMapping("users")
    public String getUsers(@PageableDefault Pageable pagable) {
        return responseToJson(new UsersApiResponse(
                getPrincipal(),
                userRepository.findAllByOrderByRealmNameAscUserNameAsc(pagable),
                null
        ));
    }

    @PreAuthorize("hasAnyAuthority(@getUserRoles)")
    @GetMapping(path = "users", params = "filter")
    public String getUsers(@RequestParam String filter, @PageableDefault Pageable pagable) {

        if (filter.isEmpty()) {
            return getUsers(pagable);
        }

        if(filter.length() > 20) {
            filter = filter.substring(0, 20);
        }

        return responseToJson(new UsersApiResponse(
                getPrincipal(),
                userRepository.findByUserNameContainingOrderByRealmNameAscUserNameAsc(filter, pagable),
                filter
        ));
    }


    @Data
    @AllArgsConstructor
    static private class UsersApiResponse {
        Principal principal;
        Page<User> payload;
        String filter;
    }
}
