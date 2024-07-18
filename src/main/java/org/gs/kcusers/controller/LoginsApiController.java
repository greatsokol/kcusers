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
//        logger.info("Authentication success {} ({})",
//                principal.getPreferredUsername(), authentication.getAuthorities());
        WebAuthenticationDetails authDetails = getAuthDetails();

        loginRepository.save(new Login(
                userName,
                Instant.now().toEpochMilli(),
                formData.getFirst("sessionid"),
                authDetails.getRemoteAddress())
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
