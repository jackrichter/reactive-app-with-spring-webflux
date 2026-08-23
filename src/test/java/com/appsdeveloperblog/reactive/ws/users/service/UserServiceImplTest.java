package com.appsdeveloperblog.reactive.ws.users.service;

import com.appsdeveloperblog.reactive.ws.users.data.UserEntity;
import com.appsdeveloperblog.reactive.ws.users.data.UserRepository;
import com.appsdeveloperblog.reactive.ws.users.presentation.model.CreateUserRequest;
import com.appsdeveloperblog.reactive.ws.users.presentation.model.UserRest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Enables Mockito Extension for JUnit 5. Thus allowing the Mockito annotations we are using.
class UserServiceImplTest {

    @Mock   // No Spring Framework Context involved. Specific for Unit Testing, vs. @MocitoBean which is for Integration Testing.
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private WebClient webClient;

    private Sinks.Many<UserRest> userSink;
    private UserServiceImpl userService;

    @BeforeEach
    public void setUp() {
        userSink = Sinks.many().multicast().onBackpressureBuffer();
        userService = new UserServiceImpl(userRepository, passwordEncoder, userSink, webClient);
    }

    @Test
    void testCreateUser_withValidRequest_returnsCreatedUserDetails() {

        // given - arrange
        CreateUserRequest request = new CreateUserRequest(
                "Sergey",
                "Kargopolov",
                "test@test.com",
                "123456789"
        );

        UserEntity savedEntity = new UserEntity();
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setFirstName(request.getFirstName());
        savedEntity.setLastName(request.getLastName());
        savedEntity.setEmail(request.getEmail());
        savedEntity.setPassword(request.getPassword());

        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.just(savedEntity));

        // when - act
        Mono<UserRest> result = userService.createUser(Mono.just(request));

        // then - assert
        StepVerifier.create(result)
                .expectNextMatches(userRest ->
                        userRest.getId().equals(savedEntity.getId()) &&
                        userRest.getFirstName().equals(savedEntity.getFirstName()) &&
                        userRest.getLastName().equals(savedEntity.getLastName()) &&
                        userRest.getEmail().equals(savedEntity.getEmail()))
                .verifyComplete();

        verify(userRepository, times(1)).save(any(UserEntity.class));

//        UserRest user = result.block();
//        assertEquals(savedEntity.getId(), user.getId());
//        assertEquals(savedEntity.getFirstName(), user.getFirstName());
    }

    @Test
    public void testCreateUser_withInvalidRequest_emitsEventsToSink() {

        // given - arrange
        CreateUserRequest request = new CreateUserRequest("John", "Doe", "john@example.com",
                "password123");

        UserEntity savedEntity = new UserEntity();
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setFirstName("John");
        savedEntity.setLastName("Doe");
        savedEntity.setEmail("john@example.com");
        savedEntity.setPassword("encodedPassword");

        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.just(savedEntity));

        // Subscribe to the sink before triggering the service call.
        Flux<UserRest> sinkFlux = userSink.asFlux();

        // when - act & then - assert
        StepVerifier.create(
                        userService.createUser(Mono.just(request))
                                .thenMany(sinkFlux.take(1))     // Tells the stream to wait for an event from the sink
                )
                .expectNextMatches(userRest -> userRest.getId().equals(savedEntity.getId()) &&
                        userRest.getFirstName().equals(savedEntity.getFirstName()) &&
                        userRest.getLastName().equals(savedEntity.getLastName()) &&
                        userRest.getEmail().equals(savedEntity.getEmail()))
                .verifyComplete();
    }
}