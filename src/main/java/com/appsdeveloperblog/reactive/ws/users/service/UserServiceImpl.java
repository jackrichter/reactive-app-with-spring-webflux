package com.appsdeveloperblog.reactive.ws.users.service;

import com.appsdeveloperblog.reactive.ws.users.data.UserEntity;
import com.appsdeveloperblog.reactive.ws.users.data.UserRepository;
import com.appsdeveloperblog.reactive.ws.users.presentation.model.AlbumRest;
import com.appsdeveloperblog.reactive.ws.users.presentation.model.CreateUserRequest;
import com.appsdeveloperblog.reactive.ws.users.presentation.model.UserRest;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Sinks.Many<UserRest> userSink;
    private final WebClient webClient;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           Sinks.Many<UserRest> userSink, WebClient webClient) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userSink = userSink;
        this.webClient = webClient;
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
                // mapNotNull returns Mono, and ConvertToUserEntity also returns Mono
                // -> Mono of Mono -> Nested Mono -> Needs flattening -> Use flatMap!
                .flatMap(userRepository::save)
                .mapNotNull(this::convertToUserRest)
                // Publish created user event to all subscribers (SSE) of the Sink
                .doOnSuccess(savedUserRest -> userSink.tryEmitNext(savedUserRest)); // Runs when a Mono runs successfully


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
//                        // return throwable;
//                        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, throwable.getMessage());
//                    }
//                });

                /* A better approach is to handle Reactive exceptions Globally!!! */
    }

    @Override
    public Mono<UserRest> getUserById(UUID id, String include) {

        return userRepository.findById(id)
                .mapNotNull(userEntity -> convertToUserRest(userEntity))
                .flatMap(user -> {
                    if (include != null && include.equals("albums")) {
                        // Fetch user's photo albums and add them to the user object
                        return includeUserAlbums(user);
                    }
                    return Mono.just(user);     // We need to wrap it into Mono because the return type is Mono<UserRest>!!!
                });
    }

    @Override
    public Flux<UserRest> findAll(int page, int limit) {

        if (page > 0) page = page - 1;

        // Create a Pageable object
        Pageable pageable = PageRequest.of(page, limit);

        return userRepository.findAllBy(pageable)
                .map(userEntity -> convertToUserRest(userEntity));
    }

    @Override
    public Flux<UserRest> streamUser() {
        // Return a Flux that can transmit a (or many) userRest object(s)
        return userSink.asFlux()
                // Behavior of how a client behaves when it connects and disconnects
                .publish()     // This turns the Flux into a 'Hot Source' that can have multiple subscribers and emit items to all of them at the same time.
                .autoConnect(1); // Starts emitting stream data immediately if there is at least one subscriber. Helps with reconnecting.
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

    /**
     * Spring Security will call this method when it needs to authenticate the user trying to log in.
     * @param username the username to look up. This username is taken from the login REST API endpoint by Spring Security.
     * @return
     */
    @Override
    public Mono<UserDetails> findByUsername(String username) {

        // Read and return the User Details stored in the database
        return userRepository.findByEmail(username)
                .map(userEntity -> User       // Spring Security’s internal representation for User's authentication
                        .withUsername(userEntity.getEmail())
                        .password(userEntity.getPassword())
                        .authorities(new ArrayList<>())
                        .build());
    }

    private Mono<UserRest> includeUserAlbums(UserRest user) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .port(8084)
                        .path("/albums")
                        .queryParam("userId",user.getId())
                        .build())
                .retrieve()         // It is used to start HTTP request and retrieve the response body, including statusor errors
                .bodyToFlux(AlbumRest.class)      // We can get in response more than one album. This will convert the response into a Flux of AlbumRest objects
                .collectList()                    // Transforms the Flux into a Mono of List<AlbumRest>
                .map(albums -> {
                    user.setAlbums(albums);
                    return user;
                });
    }
}
