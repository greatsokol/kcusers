/*
 * Created by Eugene Sokolov 12.07.2024, 10:33.
 */

package org.gs.kcusers.configs;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.ws.rs.ServerErrorException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ConnectException;

// should handle exceptions for all the other controllers
@ControllerAdvice(annotations = Controller.class)
@Order(2)  // NOTE: order 2 here
public class AuthorizationDeniedExceptionControllerExceptionHandler {
    @ExceptionHandler(AuthorizationDeniedException.class)
    public String handleAuthorizationDeniedException(AuthorizationDeniedException e) {
        return "forward:/access-denied";
    }


    @ExceptionHandler(ConnectException.class)
    public void handleConnectException(ConnectException e, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        PrintWriter out = response.getWriter();
        out.write("No connection to database: " + e.getMessage());
    }

    @ExceptionHandler(ServerErrorException.class)
    public void handleServerErrorException(ServerErrorException e, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        PrintWriter out = response.getWriter();
        out.write("No connection to keycloak: " + e.getMessage());
    }
}
