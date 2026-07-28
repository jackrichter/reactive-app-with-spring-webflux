package com.appsdeveloperblog.reactive.ws.users.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtServiceImpl implements JwtService {

    private final Environment environment;

    public JwtServiceImpl(Environment environment) {
        this.environment = environment;
    }

    /**
     * In our application subject will be the user's id.
     * This method will be called in AuthenticationServiceImpl->createAuth>Response to generate the JWT token,
     * and then it will be sent to the client, as part of the response header, in the 'login' REST endpoint
     * in the AuthenticationController.
     * @param subject
     * @return JWT token base64 URL encoded
     */
    @Override
    public String generateJwt(String subject) {
        return Jwts
                .builder()
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(getSigningKey())
                .compact();
    }

    @Override
    public Mono<Boolean> validateJwt(String token) {
        return Mono.just(token)
                // Parse the JWT token
                .map(jwt -> parseToken(jwt))
                // Additional validation
                .map(claims -> !claims.getExpiration().before(new Date()))
                .onErrorReturn(false);
    }

    @Override
    public String extractTokenSubject(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * Parse the JWT token and return the extracted Claims
     * @param token
     * @return
     */
    private Claims parseToken(String token) {
        return Jwts
                .parser()
                .verifyWith(getSigningKey())
                .build()
                // Parse the provided JWT into its component parts: Header, Payload and Signature, and verify if valid
                .parseSignedClaims(token)
                // Return the Claims
                .getPayload();
    }

    private SecretKey getSigningKey() {
//        String tokenSecret = environment.getProperty("token.secret");

        return Optional.ofNullable(environment.getProperty("token.secret"))
                .map(tokenSecret -> tokenSecret.getBytes())
                .map(tokenSecretBytes -> Keys.hmacShaKeyFor(tokenSecretBytes))
                .orElseThrow(() ->
                        new IllegalArgumentException("token.secret must be configured in application.properties file"));
    }
}
