package com.appsdeveloperblog.reactive.ws.users.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class WebSecurity {

    @Bean
    public SecurityWebFilterChain httpSecurityFilterChain(ServerHttpSecurity http,
                                                          ReactiveAuthenticationManager authenticationManager) {

        return http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.POST, "/users").permitAll()
                        .pathMatchers(HttpMethod.POST, "/login").permitAll()
                        .anyExchange()
                        .authenticated())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)   // Disable Basic Authentication
                .authenticationManager(authenticationManager)           // Register which AuthenticationManager to use to check user credentials.
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
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
