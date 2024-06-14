/*
 * Created by Eugene Sokolov 14.06.2024, 10:20.
 */

package org.gs.kcusers.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/access-denied")
public class AccessDeniedController extends CommonController {
    @GetMapping
    public String accessDenied(Map<String, Object> model) {
        model.put("authorizedusername", getAuthorizedUserName());
        model.put("authorities", getAuthorities());
        return "access-denied";
    }
}
