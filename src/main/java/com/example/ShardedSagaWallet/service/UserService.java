package com.example.ShardedSagaWallet.service;

import com.example.ShardedSagaWallet.dto.UserRequestDTO;
import com.example.ShardedSagaWallet.dto.UserResponseDTO;
import com.example.ShardedSagaWallet.entities.User;
import com.example.ShardedSagaWallet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserResponseDTO createUser(UserRequestDTO userRequestDTO){
        User user = User.builder()
                .name(userRequestDTO.getName())
                .email(userRequestDTO.getEmail())
                .build();

        User userResult = userRepository.save(user);

        return UserResponseDTO.builder()
                .name(userResult.getName())
                .email(userResult.getEmail())
                .id(userResult.getId())
                .build();
    }
}
