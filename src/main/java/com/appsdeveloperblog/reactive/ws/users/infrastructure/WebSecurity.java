package com.appsdeveloperblog.reactive.ws.users.infrastructure;

import com.appsdeveloperblog.reactive.ws.users.service.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity       // Enable Reactive Method Security globally in the application.
public class WebSecurity {

    @Bean
    public SecurityWebFilterChain httpSecurityFilterChain(ServerHttpSecurity http,
                                                          ReactiveAuthenticationManager authenticationManager,
                                                          JwtService jwtService) {

        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService);

        return http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.POST, "/users").permitAll()
                        .pathMatchers(HttpMethod.POST, "/login").permitAll()
                        .pathMatchers(HttpMethod.GET, "/users/stream").permitAll()
                        .anyExchange()
                        .authenticated())
                .cors(corsSpec -> corsSpec.configurationSource(corsConfigurationSource()))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)   // Disable Basic Authentication.
                .authenticationManager(authenticationManager)           // Register which AuthenticationManager to use to check user credentials.
                .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)   // Add JWT Filter to the chain at position 'AUTHENTICATION'.
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())   // Make application stateless, thus improving scalability.
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Cross-Origin Resource Sharing (CORS).
     * This is a relevant specification with the emergence of HTML5 and JS clients that consume data via REST APIs.
     * CORS enables cross-domain communication.
     * For this application the CorsConfigurationSource has to come from the Spring Reactive package.
     */
    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("*"));        // Allowed HTTP methods
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Apply this CORS configuration to all URL paths and return it
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();     // In our case, this has to come from the Spring Reactive package.
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * The Flow of this WebFlux Security implementation:
     * 1. The SecurityWebFilterChain registers the AuthenticationManager (Bean ReactiveAuthenticationManager).
     *    We create this Manager using the UserDetailsRepositoryReactiveAuthenticationManager class, as implementation,
     *    attach to it the PasswordEncoder used to encode user password, and we want Spring Security to use it.
     * 2. This AuthenticationManager uses a UserDetailsService through interface ReactiveUserDetailsService, which we
     *    extend the existing UserService with.
     * 3. UserServiceImpl then implements the method 'findByUserName' of the ReactiveUserDetailsService interface,
     *    which is then called by the AuthenticationManager automatically.
     * 4. We create an AuthenticationService interface to implement a method 'authenticate' that will perform the actual authentication.
     *    In it, we use the AuthenticationManager to compare the provided password with the stored encrypted password.
     * 5. The login endpoint in the AuthenticationController calls this 'authenticate' method to start the authentication process.
     * OBS!
     *    We use the email as a username in this example.
     */
}
