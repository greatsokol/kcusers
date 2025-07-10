/*
 * Created by Eugene Sokolov 27.06.2024, 10:26.
 */

package org.gs.kcusers.controller.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.gs.kcusers.controller.CommonController;
import org.gs.kcusers.domain.Login;
import org.gs.kcusers.repositories.LoginRepository;
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
    protected LoginRepository loginRepository;

    LoginsApiController(LoginRepository loginRepository) {
        this.loginRepository = loginRepository;
    }

    @PreAuthorize("hasAnyAuthority(@getUserRoles)")
    @GetMapping("/{userName}")
    public LoginsApiResponse eventsPage(@PathVariable String userName,
                                        @PageableDefault Pageable pagable) {
        saveLoginEvent();
        return new LoginsApiResponse(
                getPrincipal(),
                loginRepository.findByUserNameOrderByAuthTimeDesc(userName, pagable)
        );
    }

    @Data
    @AllArgsConstructor
    static public class LoginsApiResponse {
        Principal principal;
        Page<Login> payload;
    }
}
