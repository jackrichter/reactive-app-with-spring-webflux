package com.appsdeveloperblog.reactive.ws.users.presentation;

import com.appsdeveloperblog.reactive.ws.users.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/users") //   http://localhost:8080/users
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
//    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ResponseEntity<UserRest>> createUser(@RequestBody @Valid Mono<CreateUserRequest> createUserRequest) {

//        UserRest userRest = new UserRest();
//        return Mono.just(userRest);

//        return createUserRequest
//                .map(requestObj -> new UserRest(UUID.randomUUID(),
//                            requestObj.getFirstName(),
//                            requestObj.getLastName(),
//                            requestObj.getEmail())
//                )
//                .map(userRest -> ResponseEntity
//                        .status(HttpStatus.CREATED)
//                        .location(URI.create("/users/" + userRest.getId()))
//                        .body(userRest));

        return userService.createUser(createUserRequest)
                .map(userRest -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .location(URI.create("/users/" + userRest.getId()))
                        .body(userRest));
    }

    @GetMapping("/{userId}")
    public Mono<ResponseEntity<UserRest>> getUser(@PathVariable("userId") UUID userId) {
//        return Mono.just(new UserRest(
//                userId,
//                "Sergey",
//                "Kargopolov",
//                "test@test.com"));

        return userService.getUserById(userId)
                .map(userRest -> ResponseEntity.status(HttpStatus.OK).body(userRest))
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build()));
    }

    @GetMapping
    public Flux<UserRest> getUsers(@RequestParam(value = "page", defaultValue = "0") int page,
                                   @RequestParam(value = "limit", defaultValue = "50") int limit) {
//        return Flux.just(
//                new UserRest(UUID.randomUUID(), "Sergey", "Kargopolov", "test@test.com"),
//                new UserRest(UUID.randomUUID(), "Alice", "Smith", "alice@test.com"),
//                new UserRest(UUID.randomUUID(), "Bob", "Johnson", "bob@test.com")
//        );

        return userService.findAll(page, limit);
    }
}
