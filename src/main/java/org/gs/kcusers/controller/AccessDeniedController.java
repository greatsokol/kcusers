/*
 * Created by Eugene Sokolov 14.06.2024, 10:20.
 */

package org.gs.kcusers.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

@Controller
@RequestMapping("/access-denied")
public class AccessDeniedController extends CommonController {
    @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.GET})
    public String accessDenied(Map<String, Object> model) {
        model.put("authorizedusername", getAuthorizedUserName());
        model.put("authorities", grantedAuthoritiesListAsString());
        model.put("userrolesgranted", userRolesGranted());
        model.put("userroles", userRoles);
        model.put("adminrolesgranted", adminRolesGranted());
        model.put("adminroles", adminRoles);
        return "access-denied";
    }
}
