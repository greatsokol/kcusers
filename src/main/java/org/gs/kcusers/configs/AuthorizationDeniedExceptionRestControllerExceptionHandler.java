/*
 * Created by Eugene Sokolov 12.07.2024, 10:30.
 */

package org.gs.kcusers.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.ServerErrorException;
import java.io.IOException;
import java.net.ConnectException;

// should handle all exception for classes annotated with
@ControllerAdvice(annotations = RestController.class)
@Order(1) // NOTE: order 1 here
public class AuthorizationDeniedExceptionRestControllerExceptionHandler {
    @Value("${front.adminroles}")
    protected String adminRoles;

    @Value("${front.userroles}")
    protected String userRoles;

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<String> handleAuthorizationDeniedException(AuthorizationDeniedException e) {
        return new ResponseEntity<>("{\"message\":\"FORBIDDEN. User must have user (" + userRoles +
                ") or admin (" + adminRoles + ") roles.\"}", HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ConnectException.class)
    public ResponseEntity<String> handleConnectException(ConnectException e) throws IOException {
        String message = "No connection to database: " + e.getMessage();
        return new ResponseEntity<>("{\"message\":\"" + message + "\"}", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ServerErrorException.class)
    public ResponseEntity<String> handleServerErrorException(ServerErrorException e) throws IOException {
        String message = "No connection to keycloak: " + e.getMessage();
        return new ResponseEntity<>("{\"message\":\"" + message + "\"}", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
