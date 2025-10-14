package by.losik.userservice.controller;

import by.losik.userservice.annotation.Loggable;
import by.losik.userservice.dto.CreateUserDTO;
import by.losik.userservice.dto.UpdateUserDTO;
import by.losik.userservice.dto.UserDTO;
import by.losik.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Loggable(level = Loggable.Level.DEBUG, logResult = true)
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public Flux<UserDTO> getAllUsers() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<UserDTO>> getUserById(@PathVariable Long id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UserDTO> createUser(@Valid @RequestBody CreateUserDTO createUserDTO) {
        return userService.save(createUserDTO);
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<UserDTO>> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserDTO updateUserDTO) {
        return userService.update(id, updateUserDTO)
                .map(ResponseEntity::ok)
                .onErrorResume(RuntimeException.class, error ->
                        error.getMessage().contains("not found")
                                ? Mono.just(ResponseEntity.notFound().build())
                                : Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
                );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<ResponseEntity<Void>> deleteUser(@PathVariable Long id) {
        return userService.existsById(id)
                .flatMap(exists -> {
                    if (exists) {
                        return userService.deleteById(id)
                                .then(Mono.just(ResponseEntity.noContent().build()));
                    } else {
                        return Mono.just(ResponseEntity.notFound().build());
                    }
                });
    }

    @GetMapping("/username/{username}")
    public Mono<ResponseEntity<UserDTO>> getUserByUsername(@PathVariable String username) {
        return userService.findByUsername(username)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    public Mono<ResponseEntity<UserDTO>> getUserByEmail(@PathVariable String email) {
        return userService.findByEmail(email)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/check/username/{username}")
    public Mono<ResponseEntity<Map<String, Boolean>>> checkUsernameAvailability(@PathVariable String username) {
        return userService.isUsernameAvailable(username)
                .map(available -> ResponseEntity.ok(Map.of("available", available)));
    }

    @GetMapping("/check/email/{email}")
    public Mono<ResponseEntity<Map<String, Boolean>>> checkEmailAvailability(@PathVariable String email) {
        return userService.isEmailAvailable(email)
                .map(available -> ResponseEntity.ok(Map.of("available", available)));
    }

    @GetMapping("/exists/username/{username}")
    public Mono<ResponseEntity<Map<String, Boolean>>> checkUsernameExists(@PathVariable String username) {
        return userService.existsByUsername(username)
                .map(exists -> ResponseEntity.ok(Map.of("exists", exists)));
    }

    @GetMapping("/exists/email/{email}")
    public Mono<ResponseEntity<Map<String, Boolean>>> checkEmailExists(@PathVariable String email) {
        return userService.existsByEmail(email)
                .map(exists -> ResponseEntity.ok(Map.of("exists", exists)));
    }

    @GetMapping("/role/{role}")
    public Flux<UserDTO> getUsersByRole(@PathVariable String role) {
        return userService.findByUserRole(role);
    }

    @GetMapping("/count")
    public Mono<ResponseEntity<Map<String, Long>>> getTotalUserCount() {
        return userService.countAll()
                .map(count -> ResponseEntity.ok(Map.of("totalUsers", count)));
    }

    @GetMapping("/count/role/{role}")
    public Mono<ResponseEntity<Map<String, Long>>> getUserCountByRole(@PathVariable String role) {
        return userService.countByRole(role)
                .map(count -> ResponseEntity.ok(Map.of("count", count)));
    }

    @GetMapping("/exists/{id}")
    public Mono<ResponseEntity<Map<String, Boolean>>> checkUserExists(@PathVariable Long id) {
        return userService.existsById(id)
                .map(exists -> ResponseEntity.ok(Map.of("exists", exists)));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteAllUsers() {
        return userService.deleteAll();
    }
}