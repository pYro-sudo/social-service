package by.losik.commentlikeservice.controller;

import by.losik.commentlikeservice.annotation.Loggable;
import by.losik.commentlikeservice.entity.Like;
import by.losik.commentlikeservice.service.LikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/likes")
@PreAuthorize("isAuthenticated()")
@Loggable(level = Loggable.Level.DEBUG, logResult = true)
public class LikeController {

    private final LikeService likeService;

    @Autowired
    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @GetMapping
    public Flux<Like> getAllLikes() {
        return likeService.findAll();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Like>> getLikeById(@PathVariable Long id) {
        return likeService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Like> createLike(@RequestBody Like like) {
        return likeService.save(like);
    }

    @PostMapping("/toggle")
    public Mono<ResponseEntity<Map<String, String>>> toggleLike(
            @RequestParam Long userId,
            @RequestParam Long imageId) {
        return likeService.toggleLike(userId, imageId)
                .map(like -> ResponseEntity.ok(Map.of("action", "liked")))
                .defaultIfEmpty(ResponseEntity.ok(Map.of("action", "unliked")));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteLike(@PathVariable Long id) {
        return likeService.findById(id)
                .flatMap(like -> likeService.deleteById(id)
                        .then(Mono.just(ResponseEntity.noContent().<Void>build())))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteLikeByUserAndImage(
            @RequestParam Long userId,
            @RequestParam Long imageId) {
        return likeService.deleteByUserIdAndImageId(userId, imageId);
    }

    @GetMapping("/user/{userId}")
    public Flux<Like> getLikesByUser(@PathVariable Long userId) {
        return likeService.findByUserId(userId);
    }

    @GetMapping("/image/{imageId}")
    public Flux<Like> getLikesByImage(@PathVariable Long imageId) {
        return likeService.findByImageId(imageId);
    }

    @GetMapping("/image/{imageId}/count")
    public Mono<ResponseEntity<Map<String, Long>>> getLikeCountByImage(@PathVariable Long imageId) {
        return likeService.countByImageId(imageId)
                .map(count -> ResponseEntity.ok(Map.of("count", count)));
    }

    @GetMapping("/user/{userId}/count")
    public Mono<ResponseEntity<Map<String, Long>>> getLikeCountByUser(@PathVariable Long userId) {
        return likeService.countByUserId(userId)
                .map(count -> ResponseEntity.ok(Map.of("count", count)));
    }

    @GetMapping("/check")
    public Mono<ResponseEntity<Map<String, Boolean>>> checkIfLiked(
            @RequestParam Long userId,
            @RequestParam Long imageId) {
        return likeService.isImageLikedByUser(userId, imageId)
                .map(liked -> ResponseEntity.ok(Map.of("liked", liked)));
    }

    @DeleteMapping("/image/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteAllLikesByImage(@PathVariable Long imageId) {
        return likeService.deleteByImageId(imageId);
    }

    @DeleteMapping("/user/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteAllLikesByUser(@PathVariable Long userId) {
        return likeService.deleteByUserId(userId);
    }

    @GetMapping("/after/{date}")
    public Flux<Like> getLikesAfterDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        return likeService.findByCreatedAtAfter(date);
    }

    @GetMapping("/before/{date}")
    public Flux<Like> getLikesBeforeDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        return likeService.findByCreatedAtBefore(date);
    }

    @GetMapping("/between")
    public Flux<Like> getLikesBetweenDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return likeService.findByCreatedAtBetween(start, end);
    }

    @PostMapping("/images/{imageId}/likes")
    public Mono<ResponseEntity<Map<String, String>>> toggleLikeForImage(
            @PathVariable Long imageId,
            @RequestHeader("X-User-Id") Long userId) {
        return likeService.toggleLike(userId, imageId)
                .map(like -> ResponseEntity.ok(Map.of("action", "liked")))
                .defaultIfEmpty(ResponseEntity.ok(Map.of("action", "unliked")));
    }

    @GetMapping("/images/{imageId}/likes")
    public Flux<Like> getLikesForImage(@PathVariable Long imageId) {
        return likeService.findByImageId(imageId);
    }

    @GetMapping("/images/{imageId}/likes/count")
    public Mono<ResponseEntity<Map<String, Long>>> getLikeCountForImage(@PathVariable Long imageId) {
        return likeService.countByImageId(imageId)
                .map(count -> ResponseEntity.ok(Map.of("count", count)));
    }
}