/*
 * Created by Eugene Sokolov 12.07.2024, 10:33.
 */

package org.gs.kcusers.configs;

import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

// should handle exceptions for all the other controllers
@ControllerAdvice(annotations = Controller.class)
@Order(2)  // NOTE: order 2 here
public class AuthorizationDeniedExceptionControllerExceptionHandler {
    @ExceptionHandler(AuthorizationDeniedException.class)
    public String handleUnexpectedException(AuthorizationDeniedException e) {
        return "forward:/access-denied";
    }
}
