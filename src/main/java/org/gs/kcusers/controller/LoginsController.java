/*
 * Created by Eugene Sokolov 27.06.2024, 10:26.
 */

package org.gs.kcusers.controller;

import org.gs.kcusers.repositories.LoginRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/logins")
public class LoginsController extends CommonController {
    @Autowired
    protected LoginRepository loginRepository;

    @PreAuthorize("hasAnyAuthority(@getUserRoles)")
    @GetMapping("/{userName}")
    public String logins(@PathVariable String userName, Map<String, Object> model, @PageableDefault Pageable pagable) {
        model.put("page", loginRepository.findByUserNameOrderByAuthTimeDesc(userName, pagable));
        model.put("authorizedusername", getAuthorizedUserName());
        return "loginspage";
    }
}
