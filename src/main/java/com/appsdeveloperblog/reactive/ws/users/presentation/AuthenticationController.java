package com.appsdeveloperblog.reactive.ws.users.presentation;

import com.appsdeveloperblog.reactive.ws.users.presentation.model.AuthenticationRequest;
import com.appsdeveloperblog.reactive.ws.users.service.AuthenticationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<Void>> login(@RequestBody Mono<AuthenticationRequest> authenticationRequestMono) {
        return authenticationRequestMono
                .flatMap(authRequestObject -> authenticationService
                        .authenticate(authRequestObject.getEmail(),
                                authRequestObject.getPassword()))
                        .map(authResponseMap -> ResponseEntity.ok()
                                .header(HttpHeaders.AUTHORIZATION,
                                        "Bearer " + authResponseMap.get("token"))
//                                .header("UserId" + authResponseMap.get("userId")) // Not working in Postman as header
                                .header(HttpHeaders.FROM, authResponseMap.get("userId"))
                                .build());
    }
}
