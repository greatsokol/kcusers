/*
 * Created by Eugene Sokolov 27.06.2024, 10:26.
 */

package org.gs.kcusers.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.gs.kcusers.domain.Login;
import org.gs.kcusers.repositories.LoginRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@EnableWebSecurity
@RestController
@RequestMapping("/api/logins")
public class LoginsApiController extends CommonController {
    @Autowired
    protected LoginRepository loginRepository;
    Logger logger = LoggerFactory.getLogger(LoginsApiController.class);

    @PreAuthorize("hasAnyAuthority(@getUserRoles)")
    @GetMapping("/{userName}")
    public String eventsPage(@PathVariable String userName,
                             @PageableDefault Pageable pagable) {
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

    @PreAuthorize("hasAnyAuthority(@getUserRoles)")
    @PostMapping("/{userName}")
    public String addLogin(@PathVariable String userName,
                           @RequestBody MultiValueMap<String, String> formData) {
        WebAuthenticationDetails authDetails = getAuthDetails();

        loginRepository.save(new Login(
                userName,
                Instant.now().toEpochMilli(),
                formData.getFirst("sessionid"),
                authDetails == null ? "" : authDetails.getRemoteAddress())
        );
        return "";
    }

    @Data
    @AllArgsConstructor
    static private class LoginsApiResponse {
        Principal principal;
        Page<Login> payload;
    }
}
