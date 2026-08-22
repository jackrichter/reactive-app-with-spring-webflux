package com.appsdeveloperblog.reactive.ws.users.presentation;

import com.appsdeveloperblog.reactive.ws.users.infrastructre.TestSecurityConfig;
import com.appsdeveloperblog.reactive.ws.users.presentation.model.CreateUserRequest;
import com.appsdeveloperblog.reactive.ws.users.presentation.model.UserRest;
import com.appsdeveloperblog.reactive.ws.users.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@WebFluxTest(UserController.class)
@Import(TestSecurityConfig.class)           // <- Attention !
class UserControllerTest {

    @MockitoBean
    private UserService userService;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void testCreateUser_withValidRequest_returnsCreatedStatusAndUserDetails() {

        // given
        CreateUserRequest createUserRequest = new CreateUserRequest(
                "Sergey",
                "Kargopolov",
                "user@example.com",
                "123456789"
        );

        UUID userId = UUID.randomUUID();
        UserRest expectedUserRest = new UserRest(
                userId,
                createUserRequest.getFirstName(),
                createUserRequest.getLastName(),
                createUserRequest.getEmail(),
                null
        );

        String expectedLocation = "/users/" + userId;

        // Stub the createUser method's responce
        when(userService.createUser(Mockito.<Mono<CreateUserRequest>>any()))
                .thenReturn(Mono.just(expectedUserRest));

        // when
        webTestClient
                .post()
                .uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createUserRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().location(expectedLocation)
                .expectBody(UserRest.class)
                .consumeWith(System.out::println)
                .value(response -> {
                    assertEquals(expectedUserRest.getId(), response.getId());
                    assertEquals(expectedUserRest.getFirstName(), response.getFirstName());
                    assertEquals(expectedUserRest.getLastName(), response.getLastName());
                    assertEquals(expectedUserRest.getEmail(), response.getEmail());
                });

        // then
        verify(userService, times(1)).createUser(Mockito.<Mono<CreateUserRequest>>any());
    }

    @Test
    public void testCreateUser_withInvalidRequest_returnsBadRequestStatus() {

        // given
        CreateUserRequest invalidRequest = new CreateUserRequest(
                "Sergey",
                "Kargopolov",
                "user@example.com",
                "123"                       // Password is too short
        );

        // when
        webTestClient
                .post()
                .uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();

        // then
        verify(userService, never()).createUser(Mockito.<Mono<CreateUserRequest>>any());
    }

    @Test
    public void testCreateUser_withEmptyFirstName_returnsBadRequestStatus() {

        // given
        CreateUserRequest invalidRequest = new CreateUserRequest(
                "",             // Empty firstName
                "Kargopolov",
                "user@example.com",
                "123456789"
        );

        // when
        webTestClient
                .post()
                .uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();

        // then
        verify(userService, never()).createUser(any());
    }

    @Test
    public void testCreateUser_withLastNameTooShort_returnsBadRequestStatus() {

        // given
        CreateUserRequest invalidRequest = new CreateUserRequest(
                "Sergey",
                "K",                // lastName is too short
                "user@example.com",
                "123456789"
        );

        // when
        webTestClient
                .post()
                .uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();

        // then
        verify(userService, never()).createUser(any());
    }

    @Test
    public void testCreateUser_whenServiceThrowsException_returnsInternalServerError() {

        // given
        CreateUserRequest validRequest = new CreateUserRequest(
                "Sergey",
                "Kargopolov",
                "user@example.com",
                "123456789"
        );

        when(userService.createUser(any()))
                .thenReturn(Mono.error(new RuntimeException("Service error")));

        // when
        webTestClient
                .post()
                .uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validRequest)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody()
                .jsonPath("$.instance").isEqualTo("/users")
                .jsonPath("$.status").isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .jsonPath("$.detail").isEqualTo("Service error");

        // then
        verify(userService, times(1)).createUser(any());
    }
}