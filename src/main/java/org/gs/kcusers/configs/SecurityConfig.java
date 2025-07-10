package org.gs.kcusers.configs;

import org.gs.kcusers.configs.exceptionhandlers.DelegatedBearerTokenAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import java.util.ArrayList;
import java.util.stream.Stream;

import static org.gs.kcusers.configs.yamlobjects.Configurations.ROLES_TOKEN_CLAIM_NAME;


@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {
    @Autowired
    @Qualifier("delegatedAuthenticationEntryPoint")
    AuthenticationEntryPoint authEntryPoint;


    @Autowired
    @Qualifier("delegatedBearerTokenAuthenticationEntryPoint")
    DelegatedBearerTokenAuthenticationEntryPoint bearerTokenAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .oauth2ResourceServer(configurer -> configurer.jwt(Customizer.withDefaults()).authenticationEntryPoint(bearerTokenAuthenticationEntryPoint))
                .exceptionHandling(configurer -> configurer.authenticationEntryPoint(authEntryPoint))
                .authorizeHttpRequests(req -> req
                        .requestMatchers("/health").permitAll()
                        .anyRequest().authenticated()
                )
                .cors(corsCustomizer -> corsCustomizer.configurationSource(
                                request -> new CorsConfiguration().applyPermitDefaultValues()
                        )
                )
                .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        var jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        converter.setPrincipalClaimName("preferred_username");
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var authorities = jwtGrantedAuthoritiesConverter.convert(jwt);
            var roles = jwt.getClaimAsStringList(ROLES_TOKEN_CLAIM_NAME); //getClaimAsMap("realm_access").get("roles");
            if (roles == null) roles = new ArrayList<>();

            return Stream.concat(authorities.stream(),
                            roles.stream()
                                    //.filter(role -> role.startsWith("ROLE_"))
                                    .map(SimpleGrantedAuthority::new)
                                    .map(GrantedAuthority.class::cast))
                    .toList();
        });

        return converter;
    }
}
