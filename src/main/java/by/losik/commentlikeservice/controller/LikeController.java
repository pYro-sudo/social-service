package by.losik.commentlikeservice.controller;

import by.losik.commentlikeservice.annotation.Loggable;
import by.losik.commentlikeservice.dto.ApiResponse;
import by.losik.commentlikeservice.dto.LikeCountResponse;
import by.losik.commentlikeservice.dto.LikeRequest;
import by.losik.commentlikeservice.dto.LikeResponse;
import by.losik.commentlikeservice.mapping.LikeMapper;
import by.losik.commentlikeservice.service.LikeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/likes")
@Loggable(level = Loggable.Level.DEBUG, logResult = true)
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;
    private final LikeMapper likeMapper;

    @GetMapping
    public Mono<ApiResponse<Flux<LikeResponse>>> getAllLikes() {
        return Mono.just(
                ApiResponse.success(
                        "Likes retrieved successfully",
                        likeService.findAll()
                                .map(likeMapper::toResponse)
                )
        );
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<LikeResponse>> getLikeById(@PathVariable Long id) {
        return likeService.findById(id)
                .map(likeMapper::toResponse)
                .map(like -> ApiResponse.success("Like retrieved successfully", like))
                .defaultIfEmpty(ApiResponse.error("Like not found with id: " + id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<LikeResponse>> createLike(@Valid @RequestBody LikeRequest likeRequest) {
        return likeService.save(likeMapper.toEntity(likeRequest))
                .map(like -> ApiResponse.success("Like saved successfully", likeMapper.toResponse(like)));
    }

    @PostMapping("/toggle")
    public Mono<ApiResponse<String>> toggleLike(
            @RequestParam Long userId,
            @RequestParam Long imageId) {
        return likeService.toggleLike(userId, imageId)
                .map(wasLiked -> wasLiked ? "liked" : "unliked")
                .map(action -> ApiResponse.success("Like toggled successfully", action));
    }

    @PostMapping("/images/{imageId}/likes")
    public Mono<ApiResponse<String>> toggleLikeForImage(
            @PathVariable Long imageId,
            @RequestHeader("X-User-Id") Long userId) {
        return likeService.toggleLike(userId, imageId)
                .map(wasLiked -> wasLiked ? "liked" : "unliked")
                .map(action -> ApiResponse.success("Like toggled successfully", action));
    }

    @DeleteMapping("/{id}")
    public Mono<ApiResponse<Void>> deleteLike(@PathVariable Long id) {
        return likeService.findById(id)
                .flatMap(like -> likeService.deleteById(id)
                        .then(Mono.just(ApiResponse.success("Like deleted successfully"))))
                .defaultIfEmpty(ApiResponse.error("Like not found with id: " + id));
    }

    @DeleteMapping("/user/{userId}/image/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<ApiResponse<Void>> deleteLikeByUserAndImage(
            @PathVariable Long userId,
            @PathVariable Long imageId) {
        return likeService.deleteByUserIdAndImageId(userId, imageId)
                .then(Mono.just(ApiResponse.success("Like deleted successfully")));
    }

    @GetMapping("/user/{userId}")
    public Mono<ApiResponse<Flux<LikeResponse>>> getLikesByUser(@PathVariable Long userId) {
        return Mono.just(
                ApiResponse.success(
                        "User likes retrieved successfully",
                        likeService.findByUserId(userId)
                                .map(likeMapper::toResponse)
                )
        );
    }

    @GetMapping("/image/{imageId}")
    public Mono<ApiResponse<Flux<LikeResponse>>> getLikesByImage(@PathVariable Long imageId) {
        return Mono.just(
                ApiResponse.success(
                        "Image likes retrieved successfully",
                        likeService.findByImageId(imageId)
                                .map(likeMapper::toResponse)
                )
        );
    }

    @GetMapping("/image/{imageId}/count")
    public Mono<ApiResponse<LikeCountResponse>> getLikeCountByImage(@PathVariable Long imageId) {
        return likeService.countByImageId(imageId)
                .map(likeMapper::toCountResponse)
                .map(count -> ApiResponse.success("Like count retrieved successfully", count))
                .defaultIfEmpty(ApiResponse.error("Could not retrieve like count for image: " + imageId));
    }

    @GetMapping("/user/{userId}/count")
    public Mono<ApiResponse<LikeCountResponse>> getLikeCountByUser(@PathVariable Long userId) {
        return likeService.countByUserId(userId)
                .map(likeMapper::toCountResponse)
                .map(count -> ApiResponse.success("Like count retrieved successfully", count))
                .defaultIfEmpty(ApiResponse.error("Could not retrieve like count for user: " + userId));
    }

    @GetMapping("/check")
    public Mono<ApiResponse<Boolean>> checkIfLiked(
            @RequestParam Long userId,
            @RequestParam Long imageId) {
        return likeService.isImageLikedByUser(userId, imageId)
                .map(liked -> ApiResponse.success("Like status checked successfully", liked));
    }

    @DeleteMapping("/image/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<ApiResponse<Void>> deleteAllLikesByImage(@PathVariable Long imageId) {
        return likeService.deleteByImageId(imageId)
                .then(Mono.just(ApiResponse.success("All image likes deleted successfully")));
    }

    @DeleteMapping("/user/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<ApiResponse<Void>> deleteAllLikesByUser(@PathVariable Long userId) {
        return likeService.deleteByUserId(userId)
                .then(Mono.just(ApiResponse.success("All user likes deleted successfully")));
    }

    @GetMapping("/after/{date}")
    public Mono<ApiResponse<Flux<LikeResponse>>> getLikesAfterDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        return Mono.just(
                ApiResponse.success(
                        "Likes after date retrieved successfully",
                        likeService.findByCreatedAtAfter(date)
                                .map(likeMapper::toResponse)
                )
        );
    }

    @GetMapping("/before/{date}")
    public Mono<ApiResponse<Flux<LikeResponse>>> getLikesBeforeDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        return Mono.just(
                ApiResponse.success(
                        "Likes before date retrieved successfully",
                        likeService.findByCreatedAtBefore(date)
                                .map(likeMapper::toResponse)
                )
        );
    }

    @GetMapping("/between")
    public Mono<ApiResponse<Flux<LikeResponse>>> getLikesBetweenDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return Mono.just(
                ApiResponse.success(
                        "Likes between dates retrieved successfully",
                        likeService.findByCreatedAtBetween(start, end)
                                .map(likeMapper::toResponse)
                )
        );
    }

    @GetMapping("/images/{imageId}/likes")
    public Mono<ApiResponse<Flux<LikeResponse>>> getLikesForImage(@PathVariable Long imageId) {
        return Mono.just(
                ApiResponse.success(
                        "Image likes retrieved successfully",
                        likeService.findByImageId(imageId)
                                .map(likeMapper::toResponse)
                )
        );
    }

    @GetMapping("/images/{imageId}/likes/count")
    public Mono<ApiResponse<LikeCountResponse>> getLikeCountForImage(@PathVariable Long imageId) {
        return likeService.countByImageId(imageId)
                .map(likeMapper::toCountResponse)
                .map(count -> ApiResponse.success("Image like count retrieved successfully", count))
                .defaultIfEmpty(ApiResponse.error("Could not retrieve like count for image: " + imageId));
    }

    @DeleteMapping
    public Mono<ApiResponse<Void>> deleteAllLikes() {
        return likeService.deleteAll()
                .then(Mono.just(ApiResponse.success("All likes deleted successfully")));
    }
}