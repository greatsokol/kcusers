/*
 * Created by Eugene Sokolov 27.06.2024, 10:26.
 */

package org.gs.kcusers.controller.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.gs.kcusers.controller.CommonController;
import org.gs.kcusers.domain.Login;
import org.gs.kcusers.repositories.LoginRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logins")
public class LoginsApiController extends CommonController {
    @Autowired
    protected LoginRepository loginRepository;

    @PreAuthorize("hasAnyAuthority(@getUserRoles)")
    @GetMapping("/{userName}")
    public String eventsPage(@PathVariable String userName,
                             @PageableDefault Pageable pagable) {
        saveLoginEvent();
        LoginsApiResponse response = new LoginsApiResponse(
                getPrincipal(),
                loginRepository.findByUserNameOrderByAuthTimeDesc(userName, pagable)
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
    static private class LoginsApiResponse {
        Principal principal;
        Page<Login> payload;
    }
}
