package com.appsdeveloperblog.reactive.ws.users.service;

import com.appsdeveloperblog.reactive.ws.users.data.UserEntity;
import com.appsdeveloperblog.reactive.ws.users.data.UserRepository;
import com.appsdeveloperblog.reactive.ws.users.presentation.CreateUserRequest;
import com.appsdeveloperblog.reactive.ws.users.presentation.UserRest;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Mono<UserRest> createUser(Mono<CreateUserRequest> createUserRequestMono) {

//        return createUserRequestMono
//                .map(item -> convertToUserEntity(item))
//                .flatMap(item -> userRepository.save(item))
//                .map(item -> convertToUserRest(item));

//        return createUserRequestMono
//                .map(this::convertToUserEntity)
//                .flatMap(userRepository::save)
//                .map(this::convertToUserRest);

        return createUserRequestMono
//                .mapNotNull(this::convertToUserEntity)
                .flatMap(this::convertToUserEntity)
                // mapNotNull returns Mono and ConvertToUserEntity also returns Mono
                // -> Mono of Mono -> Nested Mono -> Needs flattening -> Use flatMap!
                .flatMap(userRepository::save)
                .mapNotNull(this::convertToUserRest);

                /* Reactive Exception Handling locally to this method only */

                // Exception handling: Single Exception
//                .onErrorMap(DuplicateKeyException.class,
//                        exception ->
//                                new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage()))

                // Exception handling: Multiple Exceptions IMPORTANT!!!
//                .onErrorMap(throwable -> {
//                    if (throwable instanceof DuplicateKeyException) {
//                        return new ResponseStatusException(HttpStatus.CONFLICT, throwable.getMessage());
//                    } else if(throwable instanceof DataIntegrityViolationException) {
//                        return new ResponseStatusException(HttpStatus.BAD_REQUEST, throwable.getMessage());
//                    } else {
////                        return throwable;
//                        return  new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, throwable.getMessage());
//                    }
//                });

                /* A better approach is to handle Reactive exceptions Globally!!! */
    }

    @Override
    public Mono<UserRest> getUserById(UUID id) {

        return userRepository.findById(id)
                .mapNotNull(userEntity -> convertToUserRest(userEntity));
    }

    @Override
    public Flux<UserRest> findAll(int page, int limit) {

        if (page > 0) page = page - 1;

        // Create a Pageable object
        Pageable pageable = PageRequest.of(page, limit);

        return userRepository.findAllBy(pageable)
                .map(userEntity -> convertToUserRest(userEntity));
    }

    private Mono<UserEntity> convertToUserEntity(CreateUserRequest createUserRequest) {

        return Mono.fromCallable(() -> {
            UserEntity userEntity = new UserEntity();
            BeanUtils.copyProperties(createUserRequest, userEntity);
            userEntity.setPassword(passwordEncoder.encode(createUserRequest.getPassword()));    // Encryption is a blocking operation!

            return userEntity;
        }).subscribeOn(Schedulers.boundedElastic());    // fromCallable and subscribeOn should be used together!

    }

    private UserRest convertToUserRest(UserEntity userEntity) {

        UserRest userRest = new UserRest();
        BeanUtils.copyProperties(userEntity, userRest);

        return userRest;
    }
}
