package by.losik.imageservice.repository;


import by.losik.imageservice.entity.Image;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Repository
public interface ImageRepository extends R2dbcRepository<Image, Long> {

    @Query("SELECT * FROM gallery.images WHERE user_id = :userId ORDER BY uploaded_at DESC")
    Flux<Image> findByUserId(@NonNull Long userId);

    @Query("SELECT * FROM gallery.images WHERE description LIKE :description ORDER BY uploaded_at DESC")
    Flux<Image> findByDescriptionContaining(@NonNull String description);

    @Query("SELECT * FROM gallery.images WHERE uploaded_at > :date ORDER BY uploaded_at DESC")
    Flux<Image> findByUploadedAtAfter(@NonNull LocalDate date);

    @Query("SELECT * FROM gallery.images WHERE uploaded_at < :date ORDER BY uploaded_at DESC")
    Flux<Image> findByUploadedAtBefore(@NonNull LocalDate date);

    @Query("SELECT * FROM gallery.images WHERE uploaded_at BETWEEN :startDate AND :endDate ORDER BY uploaded_at DESC")
    Flux<Image> findByUploadedAtBetween(@NonNull LocalDate startDate, @NonNull LocalDate endDate);

    @Query("SELECT COUNT(*) > 0 FROM gallery.images WHERE url = :url")
    Mono<Boolean> existsByUrl(@NonNull String url);

    @Query("SELECT COUNT(*) FROM gallery.images WHERE user_id = :userId")
    Mono<Long> countByUserId(@NonNull Long userId);

    @Query("DELETE FROM gallery.images WHERE user_id = :userId")
    Mono<Void> deleteByUserId(@NonNull Long userId);

    @Query("UPDATE gallery.images SET description = :description WHERE id = :id")
    Mono<Integer> updateDescription(@NonNull Long id, @NonNull String description);

    @Query("SELECT * FROM gallery.images WHERE url = :url")
    Mono<Image> findByUrl(@NonNull String url);
}