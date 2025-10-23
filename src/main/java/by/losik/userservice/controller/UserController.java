package by.losik.userservice.controller;

import by.losik.userservice.annotation.Loggable;
import by.losik.userservice.dto.CreateUserDTO;
import by.losik.userservice.dto.UpdateUserDTO;
import by.losik.userservice.dto.UserDTO;
import by.losik.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Loggable(level = Loggable.Level.DEBUG, logResult = true)
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for managing users and user accounts")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Get all users",
            description = "Retrieve a list of all users in the system"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved all users",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))
    )
    @GetMapping
    public Flux<UserDTO> getAllUsers() {
        return userService.findAll();
    }

    @Operation(
            summary = "Get user by ID",
            description = "Retrieve a specific user by their unique identifier"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved user",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content
            )
    })
    @GetMapping("/{id}")
    public Mono<ResponseEntity<UserDTO>> getUserById(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable Long id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Create a new user",
            description = "Create a new user account with the provided information"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "User successfully created",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content
            )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UserDTO> createUser(
            @Valid @RequestBody CreateUserDTO createUserDTO) {
        return userService.save(createUserDTO);
    }

    @Operation(
            summary = "Update user information",
            description = "Update an existing user's information"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User successfully updated",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @PutMapping("/{id}")
    public Mono<ResponseEntity<UserDTO>> updateUser(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserDTO updateUserDTO) {
        return userService.update(id, updateUserDTO)
                .map(ResponseEntity::ok)
                .onErrorResume(RuntimeException.class, error ->
                        error.getMessage().contains("not found")
                                ? Mono.just(ResponseEntity.notFound().build())
                                : Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
                );
    }

    @Operation(
            summary = "Delete a user",
            description = "Permanently delete a user account"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "User successfully deleted"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content
            )
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<ResponseEntity<Void>> deleteUser(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable Long id) {
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

    @Operation(
            summary = "Get user by username",
            description = "Retrieve a user by their username"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved user",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content
            )
    })
    @GetMapping("/username/{username}")
    public Mono<ResponseEntity<UserDTO>> getUserByUsername(
            @Parameter(description = "Username", required = true, example = "john_doe")
            @PathVariable String username) {
        return userService.findByUsername(username)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Get user by email",
            description = "Retrieve a user by their email address"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved user",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content
            )
    })
    @GetMapping("/email/{email}")
    public Mono<ResponseEntity<UserDTO>> getUserByEmail(
            @Parameter(description = "Email address", required = true, example = "user@example.com")
            @PathVariable String email) {
        return userService.findByEmail(email)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Check username availability",
            description = "Check if a username is available for registration"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Username availability status",
            content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"available\": true}"))
    )
    @GetMapping("/check/username/{username}")
    public Mono<ResponseEntity<Map<String, Boolean>>> checkUsernameAvailability(
            @Parameter(description = "Username to check", required = true, example = "john_doe")
            @PathVariable String username) {
        return userService.isUsernameAvailable(username)
                .map(available -> ResponseEntity.ok(Map.of("available", available)));
    }

    @Operation(
            summary = "Check email availability",
            description = "Check if an email address is available for registration"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Email availability status",
            content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"available\": true}"))
    )
    @GetMapping("/check/email/{email}")
    public Mono<ResponseEntity<Map<String, Boolean>>> checkEmailAvailability(
            @Parameter(description = "Email address to check", required = true, example = "user@example.com")
            @PathVariable String email) {
        return userService.isEmailAvailable(email)
                .map(available -> ResponseEntity.ok(Map.of("available", available)));
    }

    @Operation(
            summary = "Check if username exists",
            description = "Check if a username already exists in the system"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Username existence status",
            content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"exists\": true}"))
    )
    @GetMapping("/exists/username/{username}")
    public Mono<ResponseEntity<Map<String, Boolean>>> checkUsernameExists(
            @Parameter(description = "Username to check", required = true, example = "john_doe")
            @PathVariable String username) {
        return userService.existsByUsername(username)
                .map(exists -> ResponseEntity.ok(Map.of("exists", exists)));
    }

    @Operation(
            summary = "Check if email exists",
            description = "Check if an email address already exists in the system"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Email existence status",
            content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"exists\": true}"))
    )
    @GetMapping("/exists/email/{email}")
    public Mono<ResponseEntity<Map<String, Boolean>>> checkEmailExists(
            @Parameter(description = "Email address to check", required = true, example = "user@example.com")
            @PathVariable String email) {
        return userService.existsByEmail(email)
                .map(exists -> ResponseEntity.ok(Map.of("exists", exists)));
    }

    @Operation(
            summary = "Get users by role",
            description = "Retrieve all users with a specific role"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved users by role",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))
    )
    @GetMapping("/role/{role}")
    public Flux<UserDTO> getUsersByRole(
            @Parameter(description = "User role", required = true, example = "USER")
            @PathVariable String role) {
        return userService.findByUserRole(role);
    }

    @Operation(
            summary = "Get total user count",
            description = "Retrieve the total number of users in the system"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Total user count",
            content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"totalUsers\": 150}"))
    )
    @GetMapping("/count")
    public Mono<ResponseEntity<Map<String, Long>>> getTotalUserCount() {
        return userService.countAll()
                .map(count -> ResponseEntity.ok(Map.of("totalUsers", count)));
    }

    @Operation(
            summary = "Get user count by role",
            description = "Retrieve the number of users with a specific role"
    )
    @ApiResponse(
            responseCode = "200",
            description = "User count by role",
            content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"count\": 120}"))
    )
    @GetMapping("/count/role/{role}")
    public Mono<ResponseEntity<Map<String, Long>>> getUserCountByRole(
            @Parameter(description = "User role", required = true, example = "USER")
            @PathVariable String role) {
        return userService.countByRole(role)
                .map(count -> ResponseEntity.ok(Map.of("count", count)));
    }

    @Operation(
            summary = "Check if user exists",
            description = "Check if a user with the specified ID exists in the system"
    )
    @ApiResponse(
            responseCode = "200",
            description = "User existence status",
            content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"exists\": true}"))
    )
    @GetMapping("/exists/{id}")
    public Mono<ResponseEntity<Map<String, Boolean>>> checkUserExists(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable Long id) {
        return userService.existsById(id)
                .map(exists -> ResponseEntity.ok(Map.of("exists", exists)));
    }

    @Operation(
            summary = "Delete all users",
            description = "Permanently delete all users from the system (Administrative function)"
    )
    @ApiResponse(
            responseCode = "204",
            description = "All users successfully deleted"
    )
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteAllUsers() {
        return userService.deleteAll();
    }
}