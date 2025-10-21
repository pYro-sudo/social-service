package by.losik.userservice.service;

import by.losik.userservice.annotation.Loggable;
import by.losik.userservice.dto.CreateUserDTO;
import by.losik.userservice.dto.UpdateUserDTO;
import by.losik.userservice.dto.UserDTO;
import by.losik.userservice.entity.User;
import by.losik.userservice.exception.UserAlreadyExistsException;
import by.losik.userservice.exception.UserNotFoundException;
import by.losik.userservice.mapping.UserMapper;
import by.losik.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Loggable(level = Loggable.Level.DEBUG, logResult = true)
@EnableCaching
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public Flux<UserDTO> findAll() {
        return userRepository.findAll()
                .map(userMapper::toDTO);
    }

    @Cacheable(value = "users", key = "#id", unless = "#result == null")
    public Mono<UserDTO> findById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDTO)
                .switchIfEmpty(Mono.error(new UserNotFoundException(id)));
    }

    @Cacheable(value = "users", key = "#username", unless = "#result == null")
    public Mono<UserDTO> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(userMapper::toDTO)
                .switchIfEmpty(Mono.error(new UserNotFoundException("username", username)));
    }

    @Cacheable(value = "users", key = "#email", unless = "#result == null")
    public Mono<UserDTO> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toDTO)
                .switchIfEmpty(Mono.error(new UserNotFoundException("email", email)));
    }

    @CacheEvict(value = "users", allEntries = true)
    public Mono<UserDTO> save(CreateUserDTO createUserDTO) {
        return checkUserExists(createUserDTO.getUsername(), createUserDTO.getEmail())
                .then(passwordEncoder.encode(createUserDTO.getPassword()))
                .flatMap(encodedPassword -> {
                    User user = userMapper.toEntity(createUserDTO);
                    user.setPassword(encodedPassword);
                    return userRepository.save(user)
                            .map(userMapper::toDTO);
                });
    }

    @CacheEvict(value = "users", key = "#id")
    public Mono<UserDTO> update(Long id, UpdateUserDTO updateUserDTO) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException(id)))
                .flatMap(existingUser -> {
                    if (updateUserDTO.getUsername() != null &&
                            !existingUser.getUsername().equals(updateUserDTO.getUsername())) {
                        return userRepository.existsByUsername(updateUserDTO.getUsername())
                                .flatMap(exists -> {
                                    if (exists) {
                                        return Mono.error(new UserAlreadyExistsException("username", updateUserDTO.getUsername()));
                                    }
                                    return updateUserWithPassword(existingUser, updateUserDTO);
                                });
                    }

                    if (updateUserDTO.getEmail() != null &&
                            !existingUser.getEmail().equals(updateUserDTO.getEmail())) {
                        return userRepository.existsByEmail(updateUserDTO.getEmail())
                                .flatMap(exists -> {
                                    if (exists) {
                                        return Mono.error(new UserAlreadyExistsException("email", updateUserDTO.getEmail()));
                                    }
                                    return updateUserWithPassword(existingUser, updateUserDTO);
                                });
                    }

                    return updateUserWithPassword(existingUser, updateUserDTO);
                })
                .map(userMapper::toDTO);
    }

    private Mono<User> updateUserWithPassword(User existingUser, UpdateUserDTO updateUserDTO) {
        if (updateUserDTO.getPassword() != null && !updateUserDTO.getPassword().isEmpty()) {
            return passwordEncoder.encode(updateUserDTO.getPassword())
                    .flatMap(encodedPassword -> {
                        userMapper.updateUserFromDTO(updateUserDTO, existingUser);
                        existingUser.setPassword(encodedPassword);
                        return userRepository.save(existingUser);
                    });
        } else {
            userMapper.updateUserFromDTO(updateUserDTO, existingUser);
            return userRepository.save(existingUser);
        }
    }

    @CacheEvict(value = "users", key = "#id")
    public Mono<Void> deleteById(Long id) {
        return userRepository.existsById(id)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new UserNotFoundException(id));
                    }
                    return userRepository.deleteById(id);
                });
    }

    public Mono<Boolean> existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public Mono<Boolean> existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public Mono<Boolean> existsById(Long id) {
        return userRepository.existsById(id);
    }

    public Mono<Boolean> isUsernameAvailable(String username) {
        return existsByUsername(username).map(exists -> !exists);
    }

    public Mono<Boolean> isEmailAvailable(String email) {
        return existsByEmail(email).map(exists -> !exists);
    }

    public Flux<UserDTO> findByUserRole(String role) {
        return userRepository.findAll()
                .filter(user -> user.getUserRole().name().equalsIgnoreCase(role))
                .map(userMapper::toDTO);
    }

    public Mono<Long> countAll() {
        return userRepository.count();
    }

    public Mono<Long> countByRole(String role) {
        return userRepository.findAll()
                .filter(user -> user.getUserRole().name().equalsIgnoreCase(role))
                .count();
    }

    public Mono<Void> deleteAll() {
        return userRepository.deleteAll();
    }

    private Mono<Void> checkUserExists(String username, String email) {
        return Mono.zip(
                userRepository.existsByUsername(username),
                userRepository.existsByEmail(email)
        ).flatMap(tuple -> {
            boolean usernameExists = tuple.getT1();
            boolean emailExists = tuple.getT2();

            if (usernameExists && emailExists) {
                return Mono.error(new UserAlreadyExistsException("username and email", username + ", " + email));
            } else if (usernameExists) {
                return Mono.error(new UserAlreadyExistsException("username", username));
            } else if (emailExists) {
                return Mono.error(new UserAlreadyExistsException("email", email));
            }

            return Mono.empty();
        });
    }
}
