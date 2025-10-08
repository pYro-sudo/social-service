package by.losik.userservice.repository;

import by.losik.userservice.entity.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends R2dbcRepository<User, Long> {

    @Query("SELECT * FROM user_schema.users WHERE username = :username")
    Mono<User> findByUsername(@NonNull String username);

    @Query("SELECT * FROM user_schema.users WHERE email = :email")
    Mono<User> findByEmail(@NonNull String email);

    @Query("SELECT COUNT(*) > 0 FROM user_schema.users WHERE username = :username")
    Mono<Boolean> existsByUsername(@NonNull String username);

    @Query("SELECT COUNT(*) > 0 FROM user_schema.users WHERE email = :email")
    Mono<Boolean> existsByEmail(@NonNull String email);

    @Query("SELECT COUNT(*) > 0 FROM user_schema.users WHERE id = :id")
    Mono<Boolean> existsById(@NonNull Long id);
}