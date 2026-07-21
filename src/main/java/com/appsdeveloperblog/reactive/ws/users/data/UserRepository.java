package com.appsdeveloperblog.reactive.ws.users.data;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserRepository extends ReactiveCrudRepository<UserEntity, UUID> {

    Flux<UserEntity> findAllBy(Pageable pageable);
    Mono<UserEntity> findByEmail(String email);
}
