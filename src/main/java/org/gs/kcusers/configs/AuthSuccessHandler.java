/*
 * Created by Eugene Sokolov 25.03.2025, 16:47.
 */

package org.gs.kcusers.configs;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.gs.kcusers.domain.Login;
import org.gs.kcusers.repositories.LoginRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.io.IOException;

public class AuthSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
    private final LoginRepository loginRepository;
    private Logger logger = LoggerFactory.getLogger(AuthSuccessHandler.class.getName());

    AuthSuccessHandler(LoginRepository loginRepository) {
        this.loginRepository = loginRepository;
    }


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {
        DefaultOidcUser principal = (DefaultOidcUser) authentication.getPrincipal();
        WebAuthenticationDetails authDetails = (WebAuthenticationDetails) authentication.getDetails();

        logger.info("Authentication success {} ({})",
                principal.getPreferredUsername(), authentication.getAuthorities());
        try {
            loginRepository.save(new Login(
                    principal.getPreferredUsername(),
                    principal.getAuthenticatedAt().toEpochMilli(),
                    authDetails.getSessionId(),
                    authDetails.getRemoteAddress() + " (s)")
            );
        } catch (Exception e) {
            logger.error("Can not save login event: {}", e.getMessage());
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
