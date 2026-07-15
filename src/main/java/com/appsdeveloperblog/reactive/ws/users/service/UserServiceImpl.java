package com.appsdeveloperblog.reactive.ws.users.service;

import com.appsdeveloperblog.reactive.ws.users.data.UserEntity;
import com.appsdeveloperblog.reactive.ws.users.data.UserRepository;
import com.appsdeveloperblog.reactive.ws.users.presentation.CreateUserRequest;
import com.appsdeveloperblog.reactive.ws.users.presentation.UserRest;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
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
                .mapNotNull(this::convertToUserEntity)
                .flatMap(userRepository::save)
                .mapNotNull(this::convertToUserRest);
    }

    @Override
    public Mono<UserRest> getUserById(UUID id) {

        return userRepository.findById(id)
                .mapNotNull(userEntity -> convertToUserRest(userEntity));
    }

    private UserEntity convertToUserEntity(CreateUserRequest createUserRequest) {

        UserEntity userEntity = new UserEntity();
        BeanUtils.copyProperties(createUserRequest, userEntity);

        return userEntity;
    }

    private UserRest convertToUserRest(UserEntity userEntity) {

        UserRest userRest = new UserRest();
        BeanUtils.copyProperties(userEntity, userRest);

        return userRest;
    }
}
