package com.appsdeveloperblog.reactive.ws.users.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * This component is responsible for validating an Authentication request through its authenticate method.
 * It performs the actual authentication by comparing the provided password with the stored encrypted password.
 */
@Configuration
public class AuthenticationManagerConfig {

    // Create and configure the Reactive Authentication Manager
    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager (
            ReactiveUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        // This Authentication Manager will call the 'findByUsername' method through the userDetailsService
        // parameter to retrieve the UserDetails from db and then compare the provided password with the stored hashed password.
        UserDetailsRepositoryReactiveAuthenticationManager authenticationManager =
                new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);

        // Store the PasswordEncoder that was used to encrypt the original user's password
        authenticationManager.setPasswordEncoder(passwordEncoder);

        // Return the configured Authentication Manager, which will be available in the Spring Application Context
        return authenticationManager;
    }
}
