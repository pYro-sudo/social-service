package by.losik.commentlikeservice.controller;

import by.losik.commentlikeservice.annotation.Loggable;
import by.losik.commentlikeservice.entity.Comment;
import by.losik.commentlikeservice.service.CommentService;
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
@RequestMapping("/api/comments")
@PreAuthorize("isAuthenticated()")
@Loggable(level = Loggable.Level.DEBUG, logResult = true)
public class CommentController {

    private final CommentService commentService;

    @Autowired
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    @ResponseStatus(code = HttpStatus.ACCEPTED)
    public Flux<Comment> getAllComments() {
        return commentService.findAll();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Comment>> getCommentById(@PathVariable Long id) {
        return commentService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Comment> createComment(@RequestBody Comment comment) {
        return commentService.save(comment);
    }

    @PostMapping("/user/{userId}/image/{imageId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Comment> createCommentForImage(
            @PathVariable Long userId,
            @PathVariable Long imageId,
            @RequestParam String content) {
        return commentService.createComment(userId, imageId, content);
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Comment>> updateComment(@PathVariable Long id, @RequestBody Comment comment) {
        return commentService.update(id, comment)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteComment(@PathVariable Long id) {
        return commentService.findById(id)
                .flatMap(comment -> commentService.deleteById(id)
                        .then(Mono.just(ResponseEntity.noContent().<Void>build())))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public Flux<Comment> getCommentsByUser(@PathVariable Long userId) {
        return commentService.findByUserId(userId);
    }

    @GetMapping("/image/{imageId}")
    public Flux<Comment> getCommentsByImage(@PathVariable Long imageId) {
        return commentService.findByImageId(imageId);
    }

    @GetMapping("/user/{userId}/image/{imageId}")
    public Flux<Comment> getCommentsByUserAndImage(
            @PathVariable Long userId,
            @PathVariable Long imageId) {
        return commentService.findByUserIdAndImageId(userId, imageId);
    }

    @DeleteMapping("/image/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteAllCommentsByImage(@PathVariable Long imageId) {
        return commentService.deleteByImageId(imageId);
    }

    @DeleteMapping("/user/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteAllCommentsByUser(@PathVariable Long userId) {
        return commentService.deleteByUserId(userId);
    }

    @DeleteMapping("/user/{userId}/image/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteAllCommentsByUserAndImage(
            @PathVariable Long userId,
            @PathVariable Long imageId) {
        return commentService.deleteByUserIdAndImageId(userId, imageId);
    }

    @GetMapping("/search")
    public Flux<Comment> searchComments(@RequestParam String keyword) {
        return commentService.findByContentContaining(keyword);
    }

    @GetMapping("/after/{date}")
    public Flux<Comment> getCommentsAfterDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        return commentService.findByCreatedAtAfter(date);
    }

    @GetMapping("/before/{date}")
    public Flux<Comment> getCommentsBeforeDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        return commentService.findByCreatedAtBefore(date);
    }

    @GetMapping("/between")
    public Flux<Comment> getCommentsBetweenDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return commentService.findByCreatedAtBetween(start, end);
    }

    @GetMapping("/count/image/{imageId}")
    public Mono<ResponseEntity<Map<String, Long>>> getImageCommentCount(@PathVariable Long imageId) {
        return commentService.countByImageId(imageId)
                .map(count -> ResponseEntity.ok(Map.of("count", count)));
    }

    @GetMapping("/count/user/{userId}")
    public Mono<ResponseEntity<Map<String, Long>>> getUserCommentCount(@PathVariable Long userId) {
        return commentService.countByUserId(userId)
                .map(count -> ResponseEntity.ok(Map.of("count", count)));
    }

    @PatchMapping("/{id}/content")
    public Mono<ResponseEntity<Map<String, Boolean>>> updateCommentContent(
            @PathVariable Long id,
            @RequestParam String content) {
        return commentService.updateContent(id, content)
                .map(updated -> ResponseEntity.ok(Map.of("updated", updated)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}/recent")
    public Flux<Comment> getUserRecentComments(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") Integer limit) {
        return commentService.findUserRecentComments(userId, limit);
    }

    @GetMapping("/image/{imageId}/recent")
    public Flux<Comment> getImageRecentComments(
            @PathVariable Long imageId,
            @RequestParam(defaultValue = "10") Integer limit) {
        return commentService.findImageRecentComments(imageId, limit);
    }

    @GetMapping("/images/{imageId}/comments")
    public Flux<Comment> getCommentsForImage(@PathVariable Long imageId) {
        return commentService.findByImageId(imageId);
    }

    @PutMapping("/images/{imageId}/comments/{commentId}")
    public Mono<ResponseEntity<Comment>> updateCommentForImage(
            @PathVariable Long imageId,
            @PathVariable Long commentId,
            @RequestBody Comment comment) {
        // Проверка что comment принадлежит imageId
        return commentService.findById(commentId)
                .filter(c -> c.getImageId().equals(imageId))
                .flatMap(c -> commentService.update(commentId, comment))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/images/{imageId}/comments/{commentId}")
    public Mono<ResponseEntity<Void>> deleteCommentForImage(
            @PathVariable Long imageId,
            @PathVariable Long commentId) {
        return commentService.findById(commentId)
                .filter(c -> c.getImageId().equals(imageId))
                .flatMap(c -> commentService.deleteById(commentId)
                        .then(Mono.just(ResponseEntity.noContent().<Void>build())))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}