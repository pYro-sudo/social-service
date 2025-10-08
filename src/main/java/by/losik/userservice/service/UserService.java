package by.losik.userservice.service;

import by.losik.userservice.annotation.Loggable;
import by.losik.userservice.entity.User;
import by.losik.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Loggable(level = Loggable.Level.DEBUG, logResult = true)
@EnableCaching
public class UserService {
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Flux<User> findAll() {
        return userRepository.findAll();
    }

    @Cacheable(value = "users", key = "#id")
    public Mono<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Cacheable(value = "users", key = "#username")
    public Mono<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Cacheable(value = "users", key = "#email")
    public Mono<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @CacheEvict(value = "users", key = "#user.id")
    public Mono<User> save(User user) {
        return userRepository.save(user);
    }

    @CacheEvict(value = "users", key = "#id")
    public Mono<User> update(Long id, User user) {
        user.setId(id);
        return userRepository.save(user);
    }

    @CacheEvict(value = "users", key = "#id")
    public Mono<Void> deleteById(Long id) {
        return userRepository.deleteById(id);
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

    public Flux<User> findByUserRole(String role) {
        return userRepository.findAll()
                .filter(user -> user.getUserRole().name().equalsIgnoreCase(role));
    }

    public Mono<Long> countAll() {
        return userRepository.count();
    }

    public Mono<Long> countByRole(String role) {
        return userRepository.findAll()
                .filter(user -> user.getUserRole().name().equalsIgnoreCase(role))
                .count();
    }
}