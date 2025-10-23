package by.losik.commentlikeservice.controller;

import by.losik.commentlikeservice.annotation.Loggable;
import by.losik.commentlikeservice.dto.ApiResponse;
import by.losik.commentlikeservice.dto.LikeCountResponse;
import by.losik.commentlikeservice.dto.LikeRequest;
import by.losik.commentlikeservice.dto.LikeResponse;
import by.losik.commentlikeservice.mapping.LikeMapper;
import by.losik.commentlikeservice.service.LikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/likes")
@Loggable(level = Loggable.Level.DEBUG, logResult = true)
@RequiredArgsConstructor
@Tag(name = "Like Management", description = "APIs for managing image likes")
public class LikeController {

    private final LikeService likeService;
    private final LikeMapper likeMapper;

    @Operation(summary = "Get all likes", description = "Retrieve a list of all likes")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully retrieved likes",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
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

    @Operation(summary = "Get like by ID", description = "Retrieve a specific like by its ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Like found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Like not found")
    })
    @GetMapping("/{id}")
    public Mono<ApiResponse<LikeResponse>> getLikeById(
            @Parameter(description = "ID of the like to retrieve", required = true, example = "1")
            @PathVariable Long id) {
        return likeService.findById(id)
                .map(likeMapper::toResponse)
                .map(like -> ApiResponse.success("Like retrieved successfully", like))
                .defaultIfEmpty(ApiResponse.error("Like not found with id: " + id));
    }

    @Operation(summary = "Create a new like", description = "Create a new like record")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Like created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<LikeResponse>> createLike(
            @Parameter(description = "Like object to create", required = true)
            @Valid @RequestBody LikeRequest likeRequest) {
        return likeService.save(likeMapper.toEntity(likeRequest))
                .map(like -> ApiResponse.success("Like saved successfully", likeMapper.toResponse(like)));
    }

    @Operation(summary = "Toggle like", description = "Toggle like status for a user and image")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Like toggled successfully")
    })
    @PostMapping("/toggle")
    public Mono<ApiResponse<String>> toggleLike(
            @Parameter(description = "User ID", required = true, example = "123")
            @RequestParam Long userId,
            @Parameter(description = "Image ID", required = true, example = "456")
            @RequestParam Long imageId) {
        return likeService.toggleLike(userId, imageId)
                .map(wasLiked -> wasLiked ? "liked" : "unliked")
                .map(action -> ApiResponse.success("Like toggled successfully", action));
    }

    @Operation(summary = "Toggle like for image", description = "Toggle like status for current user and image")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Like toggled successfully")
    })
    @PostMapping("/images/{imageId}/likes")
    public Mono<ApiResponse<String>> toggleLikeForImage(
            @Parameter(description = "Image ID", required = true, example = "456")
            @PathVariable Long imageId,
            @Parameter(description = "User ID from header", required = true, example = "123")
            @RequestHeader("X-User-Id") Long userId) {
        return likeService.toggleLike(userId, imageId)
                .map(wasLiked -> wasLiked ? "liked" : "unliked")
                .map(action -> ApiResponse.success("Like toggled successfully", action));
    }

    @Operation(summary = "Delete like by ID", description = "Delete a specific like by its ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Like deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Like not found")
    })
    @DeleteMapping("/{id}")
    public Mono<ApiResponse<Void>> deleteLike(
            @Parameter(description = "ID of the like to delete", required = true, example = "1")
            @PathVariable Long id) {
        return likeService.findById(id)
                .flatMap(like -> likeService.deleteById(id)
                        .then(Mono.just(ApiResponse.success("Like deleted successfully"))))
                .defaultIfEmpty(ApiResponse.error("Like not found with id: " + id));
    }

    @Operation(summary = "Delete like by user and image", description = "Delete like for specific user and image")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Like deleted successfully")
    })
    @DeleteMapping("/user/{userId}/image/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<ApiResponse<Void>> deleteLikeByUserAndImage(
            @Parameter(description = "User ID", required = true, example = "123")
            @PathVariable Long userId,
            @Parameter(description = "Image ID", required = true, example = "456")
            @PathVariable Long imageId) {
        return likeService.deleteByUserIdAndImageId(userId, imageId)
                .then(Mono.just(ApiResponse.success("Like deleted successfully")));
    }

    @Operation(summary = "Get likes by user", description = "Retrieve all likes for a specific user")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User likes retrieved successfully")
    })
    @GetMapping("/user/{userId}")
    public Mono<ApiResponse<Flux<LikeResponse>>> getLikesByUser(
            @Parameter(description = "User ID", required = true, example = "123")
            @PathVariable Long userId) {
        return Mono.just(
                ApiResponse.success(
                        "User likes retrieved successfully",
                        likeService.findByUserId(userId)
                                .map(likeMapper::toResponse)
                )
        );
    }

    @Operation(summary = "Get likes by image", description = "Retrieve all likes for a specific image")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Image likes retrieved successfully")
    })
    @GetMapping("/image/{imageId}")
    public Mono<ApiResponse<Flux<LikeResponse>>> getLikesByImage(
            @Parameter(description = "Image ID", required = true, example = "456")
            @PathVariable Long imageId) {
        return Mono.just(
                ApiResponse.success(
                        "Image likes retrieved successfully",
                        likeService.findByImageId(imageId)
                                .map(likeMapper::toResponse)
                )
        );
    }

    @Operation(summary = "Get like count by image", description = "Get total number of likes for an image")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Like count retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Image not found")
    })
    @GetMapping("/image/{imageId}/count")
    public Mono<ApiResponse<LikeCountResponse>> getLikeCountByImage(
            @Parameter(description = "Image ID", required = true, example = "456")
            @PathVariable Long imageId) {
        return likeService.countByImageId(imageId)
                .map(likeMapper::toCountResponse)
                .map(count -> ApiResponse.success("Like count retrieved successfully", count))
                .defaultIfEmpty(ApiResponse.error("Could not retrieve like count for image: " + imageId));
    }

    @Operation(summary = "Get like count by user", description = "Get total number of likes by a user")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Like count retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/user/{userId}/count")
    public Mono<ApiResponse<LikeCountResponse>> getLikeCountByUser(
            @Parameter(description = "User ID", required = true, example = "123")
            @PathVariable Long userId) {
        return likeService.countByUserId(userId)
                .map(likeMapper::toCountResponse)
                .map(count -> ApiResponse.success("Like count retrieved successfully", count))
                .defaultIfEmpty(ApiResponse.error("Could not retrieve like count for user: " + userId));
    }

    @Operation(summary = "Check if user liked image", description = "Check if a specific user has liked a specific image")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Like status checked successfully")
    })
    @GetMapping("/check")
    public Mono<ApiResponse<Boolean>> checkIfLiked(
            @Parameter(description = "User ID", required = true, example = "123")
            @RequestParam Long userId,
            @Parameter(description = "Image ID", required = true, example = "456")
            @RequestParam Long imageId) {
        return likeService.isImageLikedByUser(userId, imageId)
                .map(liked -> ApiResponse.success("Like status checked successfully", liked));
    }

    @Operation(summary = "Delete all likes by image", description = "Delete all likes for a specific image")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "All image likes deleted successfully")
    })
    @DeleteMapping("/image/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<ApiResponse<Void>> deleteAllLikesByImage(
            @Parameter(description = "Image ID", required = true, example = "456")
            @PathVariable Long imageId) {
        return likeService.deleteByImageId(imageId)
                .then(Mono.just(ApiResponse.success("All image likes deleted successfully")));
    }

    @Operation(summary = "Delete all likes by user", description = "Delete all likes by a specific user")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "All user likes deleted successfully")
    })
    @DeleteMapping("/user/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<ApiResponse<Void>> deleteAllLikesByUser(
            @Parameter(description = "User ID", required = true, example = "123")
            @PathVariable Long userId) {
        return likeService.deleteByUserId(userId)
                .then(Mono.just(ApiResponse.success("All user likes deleted successfully")));
    }

    @Operation(summary = "Get likes after date", description = "Retrieve likes created after specified date")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Likes retrieved successfully")
    })
    @GetMapping("/after/{date}")
    public Mono<ApiResponse<Flux<LikeResponse>>> getLikesAfterDate(
            @Parameter(description = "Date in ISO format (yyyy-MM-ddTHH:mm:ss)", required = true, example = "2023-01-01T00:00:00")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        return Mono.just(
                ApiResponse.success(
                        "Likes after date retrieved successfully",
                        likeService.findByCreatedAtAfter(date)
                                .map(likeMapper::toResponse)
                )
        );
    }

    @Operation(summary = "Get likes before date", description = "Retrieve likes created before specified date")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Likes retrieved successfully")
    })
    @GetMapping("/before/{date}")
    public Mono<ApiResponse<Flux<LikeResponse>>> getLikesBeforeDate(
            @Parameter(description = "Date in ISO format (yyyy-MM-ddTHH:mm:ss)", required = true, example = "2023-12-31T23:59:59")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        return Mono.just(
                ApiResponse.success(
                        "Likes before date retrieved successfully",
                        likeService.findByCreatedAtBefore(date)
                                .map(likeMapper::toResponse)
                )
        );
    }

    @Operation(summary = "Get likes between dates", description = "Retrieve likes created between two dates")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Likes retrieved successfully")
    })
    @GetMapping("/between")
    public Mono<ApiResponse<Flux<LikeResponse>>> getLikesBetweenDates(
            @Parameter(description = "Start date in ISO format", required = true, example = "2023-01-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "End date in ISO format", required = true, example = "2023-12-31T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return Mono.just(
                ApiResponse.success(
                        "Likes between dates retrieved successfully",
                        likeService.findByCreatedAtBetween(start, end)
                                .map(likeMapper::toResponse)
                )
        );
    }

    @Operation(summary = "Get likes for image", description = "Retrieve all likes for a specific image")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Image likes retrieved successfully")
    })
    @GetMapping("/images/{imageId}/likes")
    public Mono<ApiResponse<Flux<LikeResponse>>> getLikesForImage(
            @Parameter(description = "Image ID", required = true, example = "456")
            @PathVariable Long imageId) {
        return Mono.just(
                ApiResponse.success(
                        "Image likes retrieved successfully",
                        likeService.findByImageId(imageId)
                                .map(likeMapper::toResponse)
                )
        );
    }

    @Operation(summary = "Get like count for image", description = "Get total number of likes for an image")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Like count retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Image not found")
    })
    @GetMapping("/images/{imageId}/likes/count")
    public Mono<ApiResponse<LikeCountResponse>> getLikeCountForImage(
            @Parameter(description = "Image ID", required = true, example = "456")
            @PathVariable Long imageId) {
        return likeService.countByImageId(imageId)
                .map(likeMapper::toCountResponse)
                .map(count -> ApiResponse.success("Image like count retrieved successfully", count))
                .defaultIfEmpty(ApiResponse.error("Could not retrieve like count for image: " + imageId));
    }

    @Operation(summary = "Delete all likes", description = "Delete all likes from the system")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All likes deleted successfully")
    })
    @DeleteMapping
    public Mono<ApiResponse<Void>> deleteAllLikes() {
        return likeService.deleteAll()
                .then(Mono.just(ApiResponse.success("All likes deleted successfully")));
    }

}
