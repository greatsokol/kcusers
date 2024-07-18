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

// should handle all exception for classes annotated with
@ControllerAdvice(annotations = RestController.class)
@Order(1) // NOTE: order 1 here
public class AuthorizationDeniedExceptionRestControllerExceptionHandler {
    @Value("${front.adminroles}")
    protected String adminRoles;

    @Value("${front.userroles}")
    protected String userRoles;

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<String> handleUnexpectedException(AuthorizationDeniedException e) {

//        // below object should be serialized to json
//        ErrorResponse errorResponse = new ErrorResponse() {
//            @Override
//            public HttpStatusCode getStatusCode() {
//                return HttpStatus.FORBIDDEN;
//            }
//
//            @Override
//            public ProblemDetail getBody() {
//                return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
//            }
//        };

        return new ResponseEntity<String>("{\"message\":\"FORBIDDEN. User must have user (" + userRoles +
                ") or admin (" + adminRoles + ") roles.\"}", HttpStatus.FORBIDDEN);
    }
}
