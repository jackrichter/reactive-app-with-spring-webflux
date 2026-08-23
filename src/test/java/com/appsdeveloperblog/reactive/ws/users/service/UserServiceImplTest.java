package com.appsdeveloperblog.reactive.ws.users.service;

import com.appsdeveloperblog.reactive.ws.users.data.UserEntity;
import com.appsdeveloperblog.reactive.ws.users.data.UserRepository;
import com.appsdeveloperblog.reactive.ws.users.presentation.model.AlbumRest;
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
import java.util.function.Function;

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

    @Test
    public void testGetUserById_withExistingUser_returnsUserRestWithoutAlbumsInfo() {

        // given - arrange
        UUID userId = UUID.randomUUID();

        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setFirstName("Sergey");
        userEntity.setLastName("Kargopolov");
        userEntity.setEmail("test@test.com");

        when(userRepository.findById(userId)).thenReturn(Mono.just(userEntity));

        // when - act
        Mono<UserRest> result = userService.getUserById(userId, null, "jwtToken");

        // then - assert and verify -> We use StepVerifier to check the UserRest object
        StepVerifier.create(result)
                .expectNextMatches(userRest -> userRest.getId().equals(userEntity.getId()) &&
                        userRest.getFirstName().equals(userEntity.getFirstName()) &&
                        userRest.getLastName().equals(userEntity.getLastName()) &&
                        userRest.getEmail().equals(userEntity.getEmail()))
                .verifyComplete();

        verify(userRepository, times(1)).findById(userId);
        verify(webClient, never()).get();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testGetUserById_WithIncludeAlbums_ReturnsAlbums() {

        // given - arrange
        UUID userId = UUID.randomUUID();
        String jwt = "valid-jwt";

        // 1. Set up UserEntity
        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setFirstName("Sergey");
        userEntity.setLastName("Kargopolov");
        userEntity.setEmail("test@test.com");
        userEntity.setPassword("encodedPass");

        // 2. Mock repository response
        when(userRepository.findById(userId)).thenReturn(Mono.just(userEntity));

        // 3. Mock WebClient response with albums
        WebClient.RequestHeadersUriSpec getSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(getSpec);
        when(getSpec.uri(any(Function.class))).thenReturn(headersSpec);
        when(headersSpec.header(eq("Authorization"), eq(jwt))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

        // 4. Explicitly return test albums
        AlbumRest album1 = new AlbumRest("album1", "Summer Vacation");
        AlbumRest album2 = new AlbumRest("album2", "Family Reunion");

        when(responseSpec.bodyToFlux(AlbumRest.class)).thenReturn(Flux.just(album1, album2));

        // when - act
        Mono<UserRest> result = userService.getUserById(userId, "albums", jwt);

        // then

        // 1. assert user details and that the albums are present
        StepVerifier.create(result)
                .expectNextMatches(userRest -> {
                    // 1. Verify user details
                    assertEquals(userEntity.getId(), userRest.getId(), "User ID mismatch");
                    assertEquals(userEntity.getFirstName(), userRest.getFirstName(), "First name mismatch");
                    assertEquals(userEntity.getLastName(), userRest.getLastName(), "Last name mismatch");
                    assertEquals(userEntity.getEmail(), userRest.getEmail(), "Email mismatch");

                    // 2. Verify albums
                    assertNotNull(userRest.getAlbums(), "Albums should not be null");
                    assertEquals(2, userRest.getAlbums().size(), "Incorrect number of albums");
                    assertEquals("Summer Vacation", userRest.getAlbums().get(0).getTitle());
                    assertEquals("Family Reunion", userRest.getAlbums().get(1).getTitle());

                    return true;
                })
                .verifyComplete();

        // Verify repository call
        verify(userRepository, times(1)).findById(userId);
    }
}