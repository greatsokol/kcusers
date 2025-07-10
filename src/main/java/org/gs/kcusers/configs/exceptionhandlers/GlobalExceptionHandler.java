/*
 * Created by Eugene Sokolov 12.07.2024, 10:30.
 */

package org.gs.kcusers.configs.exceptionhandlers;

import jakarta.servlet.http.HttpServletRequest;
import net.minidev.json.JSONObject;
import org.gs.kcusers.domain.Audit;
import org.gs.kcusers.repositories.AuditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice()
public class GlobalExceptionHandler {
    @Autowired
    AuditRepository auditRepository;

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<JSONObject> handleAuthenticationException(HttpServletRequest request, AuthenticationException e) { //
        auditRepository.save(new Audit(
                Audit.ENT_TOKEN,
                Audit.SUBTYPE_ERR,
                HttpStatus.UNAUTHORIZED.value(),
                request.getMethod(),
                request.getRequestURI(),
                e.getMessage())
        );
        var o = new JSONObject();
        o.put("message", e.getMessage());
        o.put("method", request.getMethod());
        o.put("uri", request.getRequestURI());
        return new ResponseEntity<>(o, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<JSONObject> handleAuthorizationDeniedException(HttpServletRequest request, AuthorizationDeniedException e) {
        auditRepository.save(new Audit(
                Audit.ENT_TOKEN,
                Audit.SUBTYPE_ERR,
                HttpStatus.FORBIDDEN.value(),
                request.getMethod(),
                request.getRequestURI(),
                e.getMessage())
        );
        var o = new JSONObject();
        o.put("message", e.getMessage());
        return new ResponseEntity<>(o, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoHandlerFoundException(HttpServletRequest request, NoHandlerFoundException e) {
        auditRepository.save(new Audit(
                Audit.ENT_API,
                Audit.SUBTYPE_ERR,
                HttpStatus.NOT_FOUND.value(),
                request.getMethod(),
                request.getRequestURI(),
                e.getMessage())
        );
        return new ResponseEntity<>(e.getBody(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleHttpRequestMethodNotSupportedException(HttpServletRequest request, HttpRequestMethodNotSupportedException e) {
        auditRepository.save(new Audit(
                Audit.ENT_API,
                Audit.SUBTYPE_ERR,
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                request.getMethod(),
                request.getRequestURI(),
                e.getMessage())
        );
        return new ResponseEntity<>(e.getBody(), HttpStatus.METHOD_NOT_ALLOWED);
    }
}
