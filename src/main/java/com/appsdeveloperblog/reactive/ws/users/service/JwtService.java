package com.appsdeveloperblog.reactive.ws.users.service;

import reactor.core.publisher.Mono;

public interface JwtService {
    String generateJwt(String subject);
    Mono<Boolean> validateJwt(String token);
    String extractTokenSubject(String token);
}
