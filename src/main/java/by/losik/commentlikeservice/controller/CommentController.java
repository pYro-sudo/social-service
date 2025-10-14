package by.losik.commentlikeservice.controller;

import by.losik.commentlikeservice.annotation.Loggable;
import by.losik.commentlikeservice.dto.ApiResponse;
import by.losik.commentlikeservice.dto.CommentCountResponse;
import by.losik.commentlikeservice.dto.CommentRequest;
import by.losik.commentlikeservice.dto.CommentResponse;
import by.losik.commentlikeservice.dto.UpdateContentRequest;
import by.losik.commentlikeservice.entity.Comment;
import by.losik.commentlikeservice.mapping.CommentMapper;
import by.losik.commentlikeservice.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/comments")
@Loggable(level = Loggable.Level.DEBUG, logResult = true)
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final CommentMapper commentMapper;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<List<CommentResponse>>> getAllComments() {
        return commentService.findAll()
                .map(commentMapper::toResponse)
                .collectList()
                .map(ApiResponse::success);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<CommentResponse>>> getCommentById(@PathVariable Long id) {
        return commentService.findById(id)
                .map(commentMapper::toResponse)
                .map(commentResponse -> ResponseEntity.ok(ApiResponse.success(commentResponse)))
                .onErrorResume(Exception.class, error ->
                        Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error(error.getMessage()))));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ResponseEntity<ApiResponse<CommentResponse>>> createComment(
            @Valid @RequestBody CommentRequest commentRequest) {

        Comment comment = commentMapper.toEntity(commentRequest);
        return commentService.save(comment)
                .map(commentMapper::toResponse)
                .map(commentResponse -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("Comment created successfully", commentResponse)));
    }

    @PostMapping("/user/{userId}/image/{imageId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ResponseEntity<ApiResponse<CommentResponse>>> createCommentForImage(
            @PathVariable Long userId,
            @PathVariable Long imageId,
            @RequestParam String content) {

        return commentService.createComment(userId, imageId, content)
                .map(commentMapper::toResponse)
                .map(commentResponse -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("Comment created successfully", commentResponse)));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<CommentResponse>>> updateComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentRequest commentRequest) {

        return commentService.findById(id)
                .flatMap(existingComment -> {
                    commentMapper.updateEntityFromRequest(commentRequest, existingComment);
                    return commentService.update(id, existingComment);
                })
                .map(commentMapper::toResponse)
                .map(commentResponse -> ResponseEntity.ok(
                        ApiResponse.success("Comment updated successfully", commentResponse)))
                .onErrorResume(Exception.class, error ->
                        Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error(error.getMessage()))));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteComment(@PathVariable Long id) {
        return commentService.deleteById(id)
                .then(Mono.just(ResponseEntity.ok(
                        ApiResponse.success("Comment deleted successfully"))))
                .onErrorResume(Exception.class, error ->
                        Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error(error.getMessage()))));
    }

    @GetMapping("/user/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<List<CommentResponse>>> getCommentsByUser(@PathVariable Long userId) {
        return commentService.findByUserId(userId)
                .map(commentMapper::toResponse)
                .collectList()
                .map(ApiResponse::success)
                .onErrorResume(Exception.class, error ->
                        Mono.just(ApiResponse.error(error.getMessage())));
    }

    @GetMapping("/image/{imageId}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<List<CommentResponse>>> getCommentsByImage(@PathVariable Long imageId) {
        return commentService.findByImageId(imageId)
                .map(commentMapper::toResponse)
                .collectList()
                .map(ApiResponse::success)
                .onErrorResume(Exception.class, error ->
                        Mono.just(ApiResponse.error(error.getMessage())));
    }

    @GetMapping("/user/{userId}/image/{imageId}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<List<CommentResponse>>> getCommentsByUserAndImage(
            @PathVariable Long userId,
            @PathVariable Long imageId) {

        return commentService.findByUserIdAndImageId(userId, imageId)
                .map(commentMapper::toResponse)
                .collectList()
                .map(ApiResponse::success)
                .onErrorResume(Exception.class, error ->
                        Mono.just(ApiResponse.error(error.getMessage())));
    }

    @DeleteMapping("/image/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteAllCommentsByImage(@PathVariable Long imageId) {
        return commentService.deleteByImageId(imageId)
                .then(Mono.just(ResponseEntity.ok(
                        ApiResponse.success("All comments for image deleted successfully"))))
                .onErrorResume(Exception.class, error ->
                        Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(error.getMessage()))));
    }

    @DeleteMapping("/user/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteAllCommentsByUser(@PathVariable Long userId) {
        return commentService.deleteByUserId(userId)
                .then(Mono.just(ResponseEntity.ok(
                        ApiResponse.success("All comments by user deleted successfully"))))
                .onErrorResume(Exception.class, error ->
                        Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(error.getMessage()))));
    }

    @DeleteMapping("/user/{userId}/image/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteAllCommentsByUserAndImage(
            @PathVariable Long userId,
            @PathVariable Long imageId) {

        return commentService.deleteByUserIdAndImageId(userId, imageId)
                .then(Mono.just(ResponseEntity.ok(
                        ApiResponse.success("All comments by user for image deleted successfully"))))
                .onErrorResume(Exception.class, error ->
                        Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(error.getMessage()))));
    }

    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<List<CommentResponse>>> searchComments(@RequestParam String keyword) {
        return commentService.findByContentContaining(keyword)
                .map(commentMapper::toResponse)
                .collectList()
                .map(ApiResponse::success)
                .onErrorResume(Exception.class, error ->
                        Mono.just(ApiResponse.error(error.getMessage())));
    }

    @GetMapping("/after/{date}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<List<CommentResponse>>> getCommentsAfterDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {

        return commentService.findByCreatedAtAfter(date)
                .map(commentMapper::toResponse)
                .collectList()
                .map(ApiResponse::success)
                .onErrorResume(Exception.class, error ->
                        Mono.just(ApiResponse.error(error.getMessage())));
    }

    @GetMapping("/before/{date}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<List<CommentResponse>>> getCommentsBeforeDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {

        return commentService.findByCreatedAtBefore(date)
                .map(commentMapper::toResponse)
                .collectList()
                .map(ApiResponse::success)
                .onErrorResume(Exception.class, error ->
                        Mono.just(ApiResponse.error(error.getMessage())));
    }

    @GetMapping("/between")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<List<CommentResponse>>> getCommentsBetweenDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        return commentService.findByCreatedAtBetween(start, end)
                .map(commentMapper::toResponse)
                .collectList()
                .map(ApiResponse::success)
                .onErrorResume(Exception.class, error ->
                        Mono.just(ApiResponse.error(error.getMessage())));
    }

    @GetMapping("/count/image/{imageId}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<CommentCountResponse>> getImageCommentCount(@PathVariable Long imageId) {
        return commentService.countByImageId(imageId)
                .map(CommentCountResponse::new)
                .map(ApiResponse::success)
                .onErrorResume(Exception.class, error ->
                        Mono.just(ApiResponse.error(error.getMessage())));
    }

    @GetMapping("/count/user/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<CommentCountResponse>> getUserCommentCount(@PathVariable Long userId) {
        return commentService.countByUserId(userId)
                .map(CommentCountResponse::new)
                .map(ApiResponse::success)
                .onErrorResume(Exception.class, error ->
                        Mono.just(ApiResponse.error(error.getMessage())));
    }

    @PatchMapping("/{id}/content")
    public Mono<ResponseEntity<ApiResponse<Boolean>>> updateCommentContent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateContentRequest request) {

        return commentService.updateContent(id, request)
                .map(updated -> ResponseEntity.ok(
                        ApiResponse.success("Content updated successfully", updated)))
                .onErrorResume(Exception.class, error ->
                        Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error(error.getMessage()))));
    }

    @GetMapping("/user/{userId}/recent")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<List<CommentResponse>>> getUserRecentComments(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") Integer limit) {

        return commentService.findUserRecentComments(userId, limit)
                .map(commentMapper::toResponse)
                .collectList()
                .map(ApiResponse::success)
                .onErrorResume(Exception.class, error ->
                        Mono.just(ApiResponse.error(error.getMessage())));
    }

    @GetMapping("/image/{imageId}/recent")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<List<CommentResponse>>> getImageRecentComments(
            @PathVariable Long imageId,
            @RequestParam(defaultValue = "10") Integer limit) {

        return commentService.findImageRecentComments(imageId, limit)
                .map(commentMapper::toResponse)
                .collectList()
                .map(ApiResponse::success)
                .onErrorResume(Exception.class, error ->
                        Mono.just(ApiResponse.error(error.getMessage())));
    }

    @GetMapping("/images/{imageId}/comments")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<List<CommentResponse>>> getCommentsForImage(@PathVariable Long imageId) {
        return commentService.findByImageId(imageId)
                .map(commentMapper::toResponse)
                .collectList()
                .map(ApiResponse::success)
                .onErrorResume(Exception.class, error ->
                        Mono.just(ApiResponse.error(error.getMessage())));
    }

    @PutMapping("/images/{imageId}/comments/{commentId}")
    public Mono<ResponseEntity<ApiResponse<CommentResponse>>> updateCommentForImage(
            @PathVariable Long imageId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest commentRequest) {

        return commentService.findById(commentId)
                .filter(comment -> comment.getImageId().equals(imageId))
                .flatMap(existingComment -> {
                    commentMapper.updateEntityFromRequest(commentRequest, existingComment);
                    return commentService.update(commentId, existingComment);
                })
                .map(commentMapper::toResponse)
                .map(commentResponse -> ResponseEntity.ok(
                        ApiResponse.success("Comment updated successfully", commentResponse)))
                .onErrorResume(Exception.class, error ->
                        Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error(error.getMessage()))));
    }

    @DeleteMapping("/images/{imageId}/comments/{commentId}")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteCommentForImage(
            @PathVariable Long imageId,
            @PathVariable Long commentId) {

        return commentService.findById(commentId)
                .filter(comment -> comment.getImageId().equals(imageId))
                .flatMap(comment -> commentService.deleteById(commentId)
                        .then(Mono.just(ResponseEntity.ok(
                                ApiResponse.success("Comment deleted successfully")))))
                .onErrorResume(Exception.class, error ->
                        Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error(error.getMessage()))));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteAll() {
        return commentService.deleteAll()
                .then(Mono.just(ResponseEntity.ok(
                        ApiResponse.success("All comments deleted successfully"))))
                .onErrorResume(Exception.class, error ->
                        Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.error(error.getMessage()))));
    }
}