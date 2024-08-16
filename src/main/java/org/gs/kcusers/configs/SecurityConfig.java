package org.gs.kcusers.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gs.kcusers.domain.Login;
import org.gs.kcusers.repositories.LoginRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;

import static org.gs.kcusers.configs.Configurations.ROLES_TOKEN_CLAIM_NAME;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {
    Logger logger = LoggerFactory.getLogger(SecurityConfig.class.getName());

    @Autowired
    ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    LoginRepository loginRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        http.oauth2Login(Customizer.withDefaults());

        return http
                .authorizeHttpRequests(req -> req.anyRequest().authenticated())
                .oauth2Login(login -> login.successHandler(authenticationSuccessHandler()))
                .cors(corsCustomizer -> corsCustomizer.configurationSource(
                                request -> new CorsConfiguration().applyPermitDefaultValues()
                        )
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout")) //skip logout confirmation
                        .logoutSuccessHandler(oidcLogoutSuccessHandler())
                        .logoutSuccessUrl("/")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true))
                .build();
    }

    private AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            DefaultOidcUser principal = (DefaultOidcUser) authentication.getPrincipal();
            WebAuthenticationDetails authDetails = (WebAuthenticationDetails) authentication.getDetails();

            logger.info("Authentication success {} ({})",
                    principal.getPreferredUsername(), authentication.getAuthorities());
            loginRepository.save(new Login(
                    principal.getPreferredUsername(),
                    principal.getAuthenticatedAt().toEpochMilli(),
                    authDetails.getSessionId(),
                    authDetails.getRemoteAddress() + " (s)")
            );
            response.sendRedirect("/");
        };
    }

    @Bean
    public OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler() {
        return new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        var jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        converter.setPrincipalClaimName("preferred_username");
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var authorities = jwtGrantedAuthoritiesConverter.convert(jwt);
            //var roles = (List<String>) jwt.getClaimAsMap("realm_access").get("roles");
            var roles = jwt.getClaimAsStringList(ROLES_TOKEN_CLAIM_NAME);

            return Stream.concat(authorities.stream(),
                            roles.stream()
                                    //.filter(role -> role.startsWith("ROLE_"))
                                    .map(SimpleGrantedAuthority::new)
                                    .map(GrantedAuthority.class::cast))
                    .toList();
        });

        return converter;
    }

    private static List<String> getRolesFromJwt(String jwt, String claimFieldName) {
        String[] chunks = jwt.split("\\.");
        Base64.Decoder decoder = Base64.getUrlDecoder();
        //String header = new String(decoder.decode(chunks[0]));
        String payload = new String(decoder.decode(chunks[1]));
        var objectMapper = new ObjectMapper();
        try {
            var jsonObj = objectMapper.readValue(payload, Object.class);
            var claim = ((LinkedHashMap<String, Object>) jsonObj).get(claimFieldName);
            return (List<String>)claim;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oAuth2UserService() {
        var oidcUserService = new OidcUserService();
        return userRequest -> {
            var realm_roles = getRolesFromJwt(
                    userRequest.getAccessToken().getTokenValue(),
                    ROLES_TOKEN_CLAIM_NAME
            );

            var oidcUser = oidcUserService.loadUser(userRequest);
            if(realm_roles == null) {
                realm_roles = oidcUser.getIdToken().getClaimAsStringList(ROLES_TOKEN_CLAIM_NAME);
                if (realm_roles == null) {
                    throw new AuthenticationServiceException("IdToken must contain \"" + ROLES_TOKEN_CLAIM_NAME + "\"");
                }
            }
            //List<?> realm_roles = (List<?>) realm_access.get("roles");
            //if (realm_roles == null)
            //throw new AuthenticationServiceException("IdToken must contain \"realm_access/roles\"");

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
