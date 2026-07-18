package com.appsdeveloperblog.reactive.ws.users.service;

import com.appsdeveloperblog.reactive.ws.users.presentation.CreateUserRequest;
import com.appsdeveloperblog.reactive.ws.users.presentation.UserRest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserService {

    Mono<UserRest> createUser(Mono<CreateUserRequest> createUserRequestMono);
    Mono<UserRest> getUserById(UUID id);
    Flux<UserRest> findAll(int page, int limit);
}
