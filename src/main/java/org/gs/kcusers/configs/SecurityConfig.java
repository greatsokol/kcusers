package org.gs.kcusers.configs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.util.List;
import java.util.stream.Stream;

@EnableMethodSecurity(securedEnabled = true)
@Configuration
public class SecurityConfig {
    Logger logger = LoggerFactory.getLogger(SecurityConfig.class.getName());

    @Autowired
    ClientRegistrationRepository clientRegistrationRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        http.oauth2Login(Customizer.withDefaults());

        return http
//                .authorizeHttpRequests(c -> c.requestMatchers("/error").permitAll()
//                        .requestMatchers("/manager.html").hasAuthority("admin")
//                .anyRequest().authenticated())
                .authorizeHttpRequests(req -> req.anyRequest().authenticated())
                .oauth2Login(login -> login.successHandler(authenticationSuccessHandler()))
                .logout(logout -> logout
                        .logoutSuccessHandler(oidcLogoutSuccessHandler())
                        .logoutSuccessUrl("/")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true))
                .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"))
                .build();
    }

    private AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            DefaultOidcUser principal = (DefaultOidcUser) authentication.getPrincipal();
            logger.info("Authentication success {} ({})",
                    principal.getPreferredUsername(), authentication.getAuthorities());
            response.sendRedirect("/");
        };
    }

    @Bean
    public OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler() {
        return new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
    }

//    @Bean
//    public JwtAuthenticationConverter jwtAuthenticationConverter() {
//        var converter = new JwtAuthenticationConverter();
//        var jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
//        converter.setPrincipalClaimName("preferred_username");
//        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
//            var authorities = jwtGrantedAuthoritiesConverter.convert(jwt);
//            var roles = (List<String>) jwt.getClaimAsMap("realm_access").get("roles");
//
//            return Stream.concat(authorities.stream(),
//                            roles.stream()
//                                    //.filter(role -> role.startsWith("ROLE_"))
//                                    .map(SimpleGrantedAuthority::new)
//                                    .map(GrantedAuthority.class::cast))
//                    .toList();
//        });
//
//        return converter;
//    }

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oAuth2UserService() {
        var oidcUserService = new OidcUserService();
        return userRequest -> {
            var oidcUser = oidcUserService.loadUser(userRequest);

            var realm_access = oidcUser.getIdToken().getClaimAsMap("realm_access");
            if (realm_access == null) throw new AuthenticationServiceException("IdToken must contain \"realm_access\"");
            List<?> realm_roles = (List<?>) realm_access.get("roles");
            if (realm_roles == null)
                throw new AuthenticationServiceException("IdToken must contain \"realm_access/roles\"");

            var authorities = Stream.concat(
                            oidcUser.getAuthorities().stream(),
                            realm_roles
                                    .stream()
                                    .map(String.class::cast)
                                    .map(SimpleGrantedAuthority::new)
                                    .map(GrantedAuthority.class::cast))
                    .toList();


            return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
        };
    }
}
