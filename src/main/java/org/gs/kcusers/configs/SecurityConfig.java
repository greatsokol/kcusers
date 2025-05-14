package org.gs.kcusers.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;

import static org.gs.kcusers.configs.Configurations.ROLES_TOKEN_CLAIM_NAME;


@Configuration
//@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {
    Logger logger = LoggerFactory.getLogger(SecurityConfig.class.getName());

    //ClientRegistrationRepository clientRegistrationRepository;
    LoginRepository loginRepository;

    @Autowired
    SecurityConfig(/*ClientRegistrationRepository clientRegistrationRepository,*/ LoginRepository loginRepository) {
        //this.clientRegistrationRepository = clientRegistrationRepository;
        this.loginRepository = loginRepository;
    }

    private static List<String> getRolesFromStringJwt(String jwt, String claimFieldName) {
        String[] chunks = jwt.split("\\.");
        Base64.Decoder decoder = Base64.getUrlDecoder();
        String payload = new String(decoder.decode(chunks[1]));
        var objectMapper = new ObjectMapper();
        try {
            var claimObject = objectMapper.readValue(payload, Object.class);
            var claimPath = claimFieldName.split("\\.");
            for (var fieldName : claimPath) {
                claimObject = ((LinkedHashMap<String, Object>) claimObject).get(fieldName);
            }
            return (List<String>) claimObject;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        //http.oauth2Login(Customizer.withDefaults());

        return http
                .authorizeHttpRequests(req -> req
                        .requestMatchers("/metrics").permitAll()
                        .anyRequest().authenticated()
                )
//                .x509(configurer -> {
//                    //var filter = new X509AuthenticationFilter();
////                    var extractor = new X509PrincipalExtractor();
////                    filter.setPrincipalExtractor(new X509PrincipalExtractor() {
////                        @Override
////                        public Object extractPrincipal(X509Certificate cert) {
////                            return cert.get;
////                        }
////                    });
//                    //configurer.x509AuthenticationFilter(filter);
//                    //configurer.subjectPrincipalRegex("CN=(.*?)(?:,|$)");
//                    configurer.authenticationUserDetailsService(new AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken>() {
//                        @Override
//                        public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) throws UsernameNotFoundException {
//                            if(token.getPrincipal().equals("localhost")) {
//                                return token.getDetails()
//                            }
//                            return null;
//                        }
//                    });
//                    //configurer.x509PrincipalExtractor(new SubjectDnX509PrincipalExtractor());
//                })
                //.oauth2Login(login -> login.successHandler(new AuthSuccessHandler(loginRepository)))
                .cors(corsCustomizer -> corsCustomizer.configurationSource(
                                request -> new CorsConfiguration().applyPermitDefaultValues()
                        )
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout")) //skip logout confirmation
                        //.logoutSuccessHandler(oidcLogoutSuccessHandler())
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true))
                .build();
    }

//    @Bean
//    public OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler() {
//        var handler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
//        handler.setPostLogoutRedirectUri("{baseScheme}://{baseHost}");
//        return handler;
//    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        var jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        converter.setPrincipalClaimName("preferred_username");
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var authorities = jwtGrantedAuthoritiesConverter.convert(jwt);
            var roles = (List<String>) jwt.getClaimAsMap("realm_access").get("roles");
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

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oAuth2UserService() {
        var oidcUserService = new OidcUserService();
        return userRequest -> {
            var realm_roles = getRolesFromStringJwt(
                    userRequest.getAccessToken().getTokenValue(),
                    ROLES_TOKEN_CLAIM_NAME
            );

            var oidcUser = oidcUserService.loadUser(userRequest);
            if (realm_roles == null) {
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
