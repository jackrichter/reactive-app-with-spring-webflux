package com.appsdeveloperblog.reactive.ws.users.data;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.UUID;

@DataR2dbcTest
@TestInstance( TestInstance.Lifecycle.PER_CLASS)        // Only one instance of this class is created for all tests
class UserRepositoryTest {

    @Autowired
    private DatabaseClient databaseClient;

    @Autowired
    private UserRepository userRepository;

    @BeforeAll          // Runs once before all tests
    void setUp() {
        UserEntity user1 = new UserEntity(UUID.randomUUID(),
                "John",
                "Doe",
                "john.doe@example.com",
                "123456789");

        UserEntity user2 = new UserEntity(UUID.randomUUID(),
                "Jane",
                "Doe",
                "jane.doe@example.com",
                "123456789");

        String insertSQL = "INSERT INTO users (id, first_name, last_name, email, password) VALUES (:id, " +
                ":firstName, :lastName, :email, :password)";

        Flux.just(user1, user2)
                .concatMap(userEntity -> databaseClient.sql(insertSQL)      // Runs it sequentially but non-blocking
                        .bind("id", userEntity.getId())
                        .bind("firstName", userEntity.getFirstName())
                        .bind("lastName", userEntity.getLastName())
                        .bind("email", userEntity.getEmail())
                        .bind("password", userEntity.getPassword())
                        .fetch()            // Runs the SQL query in database
                        .rowsUpdated()      // Returns the number of rows affected by the query
                )
                .then()                     // Makes the Reactive stream wait until all the inserts are completed
                .as(StepVerifier::create)   // This converts the Reactive stream into a testable form
                .verifyComplete();          // Checks that everything was su
    }

    @AfterAll           // Runs once after all tests
    void tearDown() {
        databaseClient.sql("TRUNCATE TABLE users")
                .then()
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    void testFindByEmail_withValidEmail_returnsUserEntity() {

        // given
        String emailToFind = "john.doe@example.com";

        // when and then
        StepVerifier.create(userRepository.findByEmail(emailToFind))
                .expectNextMatches(userEntity -> userEntity.getEmail().equals(emailToFind))
                .verifyComplete();
    }

    @Test
    public void testFindByEmail_withNonExistentEmail_returnsEmptyMono() {

        // given
        String nonExistentEmail = "nonexistent@example.com";

        // when and then
        StepVerifier.create(userRepository.findByEmail(nonExistentEmail))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    public void testFindAllBy_withValidPagable_retursPaginatedResults() {

        // given
        Pageable pageable = PageRequest.of(0, 2);    // First page, page size = 2

        // when and then
        StepVerifier.create(userRepository.findAllBy(pageable))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    public void testFindAllBy_withNoExistentPagable_returnsEmptyFlux() {

        // given
        Pageable pageable = PageRequest.of(1, 2);   // Second page, page size = 2 (non-existent data)

        // when and then
        StepVerifier.create(userRepository.findAllBy(pageable))
                .expectNextCount(0)
                .expectComplete()
                .verify();
    }
}