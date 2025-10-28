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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/comments")
@Loggable(level = Loggable.Level.DEBUG, logResult = true)
@RequiredArgsConstructor
@Tag(name = "Comments", description = "API для управления комментариями к изображениям")
public class CommentController {

    private final CommentService commentService;
    private final CommentMapper commentMapper;

    @Operation(
            summary = "Получить все комментарии",
            description = "Возвращает список всех комментариев в системе"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Успешное получение списка комментариев",
            content = @Content(mediaType = "application/json")
    )
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<List<CommentResponse>>> getAllComments() {
        return commentService.findAll()
                .map(commentMapper::toResponse)
                .collectList()
                .map(ApiResponse::success);
    }

    @Operation(
            summary = "Получить комментарий по ID",
            description = "Возвращает комментарий по его идентификатору"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Комментарий найден",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Комментарий не найден",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<CommentResponse>>> getCommentById(
            @Parameter(description = "Идентификатор комментария", required = true, example = "1")
            @PathVariable Long id) {
        return commentService.findById(id)
                .map(commentMapper::toResponse)
                .map(commentResponse -> ResponseEntity.ok(ApiResponse.success(commentResponse)))
                .onErrorResume(Exception.class, error ->
                        Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error(error.getMessage()))));
    }

    @Operation(
            summary = "Создать новый комментарий",
            description = "Создает новый комментарий с предоставленными данными"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Комментарий успешно создан",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Неверные данные запроса",
                    content = @Content(mediaType = "application/json")
            )
    })
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

    @Operation(
            summary = "Создать комментарий для изображения",
            description = "Создает комментарий для конкретного пользователя и изображения"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Комментарий успешно создан",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PostMapping("/user/{userId}/image/{imageId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ResponseEntity<ApiResponse<CommentResponse>>> createCommentForImage(
            @Parameter(description = "Идентификатор пользователя", required = true, example = "123")
            @PathVariable Long userId,
            @Parameter(description = "Идентификатор изображения", required = true, example = "456")
            @PathVariable Long imageId,
            @Parameter(description = "Содержание комментария", required = true, example = "Отличное фото!")
            @RequestParam String content) {

        return commentService.createComment(userId, imageId, content)
                .map(commentMapper::toResponse)
                .map(commentResponse -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("Comment created successfully", commentResponse)));
    }

    @Operation(
            summary = "Обновить комментарий",
            description = "Обновляет существующий комментарий по идентификатору"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Комментарий успешно обновлен",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Комментарий не найден",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<CommentResponse>>> updateComment(
            @Parameter(description = "Идентификатор комментария", required = true, example = "1")
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

    @Operation(
            summary = "Удалить комментарий",
            description = "Удаляет комментарий по идентификатору"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Комментарий успешно удален",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Комментарий не найден",
                    content = @Content(mediaType = "application/json")
            )
    })
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteComment(
            @Parameter(description = "Идентификатор комментария", required = true, example = "1")
            @PathVariable Long id) {
        return commentService.deleteById(id)
                .then(Mono.just(ResponseEntity.ok(
                        ApiResponse.success("Comment deleted successfully"))))
                .onErrorResume(Exception.class, error ->
                        Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error(error.getMessage()))));
    }

    @Operation(
            summary = "Получить комментарии пользователя",
            description = "Возвращает все комментарии указанного пользователя"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Успешное получение комментариев",
            content = @Content(mediaType = "application/json")
    )
    @GetMapping("/user/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<List<CommentResponse>>> getCommentsByUser(
            @Parameter(description = "Идентификатор пользователя", required = true, example = "123")
            @PathVariable Long userId) {
        return commentService.findByUserId(userId)
                .map(commentMapper::toResponse)
                .collectList()
                .map(ApiResponse::success)
                .onErrorResume(Exception.class, error ->
                        Mono.just(ApiResponse.error(error.getMessage())));
    }

    @Operation(
            summary = "Получить комментарии изображения",
            description = "Возвращает все комментарии для указанного изображения"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Успешное получение комментариев",
            content = @Content(mediaType = "application/json")
    )
    @GetMapping("/image/{imageId}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<List<CommentResponse>>> getCommentsByImage(
            @Parameter(description = "Идентификатор изображения", required = true, example = "456")
            @PathVariable Long imageId) {
        return commentService.findByImageId(imageId)
                .map(commentMapper::toResponse)
                .collectList()
                .map(ApiResponse::success)
                .onErrorResume(Exception.class, error ->
                        Mono.just(ApiResponse.error(error.getMessage())));
    }

    @Operation(
            summary = "Получить комментарии пользователя для изображения",
            description = "Возвращает комментарии конкретного пользователя для конкретного изображения"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Успешное получение комментариев",
            content = @Content(mediaType = "application/json")
    )
    @GetMapping("/user/{userId}/image/{imageId}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<List<CommentResponse>>> getCommentsByUserAndImage(
            @Parameter(description = "Идентификатор пользователя", required = true, example = "123")
            @PathVariable Long userId,
            @Parameter(description = "Идентификатор изображения", required = true, example = "456")
            @PathVariable Long imageId) {

        return commentService.findByUserIdAndImageId(userId, imageId)
                .map(commentMapper::toResponse)
                .collectList()
                .map(ApiResponse::success)
                .onErrorResume(Exception.class, error ->
                        Mono.just(ApiResponse.error(error.getMessage())));
    }

    @Operation(
            summary = "Удалить все комментарии изображения",
            description = "Удаляет все комментарии для указанного изображения"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Комментарии успешно удалены",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Ошибка при удалении",
                    content = @Content(mediaType = "application/json")
            )
    })
    @DeleteMapping("/image/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteAllCommentsByImage(
            @Parameter(description = "Идентификатор изображения", required = true, example = "456")
            @PathVariable Long imageId) {
        return commentService.deleteByImageId(imageId)
                .then(Mono.just(ResponseEntity.ok(
                        ApiResponse.success("All comments for image deleted successfully"))))
                .onErrorResume(Exception.class, error ->
                        Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(error.getMessage()))));
    }

    @Operation(
            summary = "Удалить все комментарии пользователя",
            description = "Удаляет все комментарии указанного пользователя"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Комментарии успешно удалены",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Ошибка при удалении",
                    content = @Content(mediaType = "application/json")
            )
    })
    @DeleteMapping("/user/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteAllCommentsByUser(
            @Parameter(description = "Идентификатор пользователя", required = true, example = "123")
            @PathVariable Long userId) {
        return commentService.deleteByUserId(userId)
                .then(Mono.just(ResponseEntity.ok(
                        ApiResponse.success("All comments by user deleted successfully"))))
                .onErrorResume(Exception.class, error ->
                        Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(error.getMessage()))));
    }

    @Operation(
            summary = "Удалить все комментарии пользователя для изображения",
            description = "Удаляет все комментарии пользователя для конкретного изображения"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Комментарии успешно удалены",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Ошибка при удалении",
                    content = @Content(mediaType = "application/json")
            )
    })
    @DeleteMapping("/user/{userId}/image/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteAllCommentsByUserAndImage(
            @Parameter(description = "Идентификатор пользователя", required = true, example = "123")
            @PathVariable Long userId,
            @Parameter(description = "Идентификатор изображения", required = true, example = "456")
            @PathVariable Long imageId) {

        return commentService.deleteByUserIdAndImageId(userId, imageId)
                .then(Mono.just(ResponseEntity.ok(
                        ApiResponse.success("All comments by user for image deleted successfully"))))
                .onErrorResume(Exception.class, error ->
                        Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(error.getMessage()))));
    }

    @Operation(
            summary = "Поиск комментариев",
            description = "Ищет комментарии по ключевому слову в содержании"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Успешный поиск комментариев",
            content = @Content(mediaType = "application/json")
    )
    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<List<CommentResponse>>> searchComments(
            @Parameter(description = "Ключевое слово для поиска", required = true, example = "отличный")
            @RequestParam String keyword) {
        return commentService.findByContentContaining(keyword)
                .map(commentMapper::toResponse)
                .collectList()
                .map(ApiResponse::success)
                .onErrorResume(Exception.class, error ->
                        Mono.just(ApiResponse.error(error.getMessage())));
    }

    @Operation(
            summary = "Получить комментарии после даты",
            description = "Возвращает комментарии, созданные после указанной даты"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Успешное получение комментариев",
            content = @Content(mediaType = "application/json")
    )
    @GetMapping("/after/{date}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<List<CommentResponse>>> getCommentsAfterDate(
            @Parameter(description = "Начальная дата (формат: YYYY-MM-DDTHH:mm:ss)", required = true, example = "2024-01-01T00:00:00")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {

        return commentService.findByCreatedAtAfter(date)
                .map(commentMapper::toResponse)
                .collectList()
                .map(ApiResponse::success)
                .onErrorResume(Exception.class, error ->
                        Mono.just(ApiResponse.error(error.getMessage())));
    }

    @Operation(
            summary = "Получить комментарии до даты",
            description = "Возвращает комментарии, созданные до указанной даты"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Успешное получение комментариев",
            content = @Content(mediaType = "application/json")
    )
    @GetMapping("/before/{date}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<List<CommentResponse>>> getCommentsBeforeDate(
            @Parameter(description = "Конечная дата (формат: YYYY-MM-DDTHH:mm:ss)", required = true, example = "2024-12-31T23:59:59")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {

        return commentService.findByCreatedAtBefore(date)
                .map(commentMapper::toResponse)
                .collectList()
                .map(ApiResponse::success)
                .onErrorResume(Exception.class, error ->
                        Mono.just(ApiResponse.error(error.getMessage())));
    }

    @Operation(
            summary = "Получить комментарии за период",
            description = "Возвращает комментарии, созданные в указанном временном периоде"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Успешное получение комментариев",
            content = @Content(mediaType = "application/json")
    )
    @GetMapping("/between")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<List<CommentResponse>>> getCommentsBetweenDates(
            @Parameter(description = "Начальная дата периода", required = true, example = "2024-01-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "Конечная дата периода", required = true, example = "2024-12-31T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        return commentService.findByCreatedAtBetween(start, end)
                .map(commentMapper::toResponse)
                .collectList()
                .map(ApiResponse::success)
                .onErrorResume(Exception.class, error ->
                        Mono.just(ApiResponse.error(error.getMessage())));
    }

    @Operation(
            summary = "Получить количество комментариев изображения",
            description = "Возвращает количество комментариев для указанного изображения"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Успешное получение количества",
            content = @Content(mediaType = "application/json")
    )
    @GetMapping("/count/image/{imageId}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<CommentCountResponse>> getImageCommentCount(
            @Parameter(description = "Идентификатор изображения", required = true, example = "456")
            @PathVariable Long imageId) {
        return commentService.countByImageId(imageId)
                .map(CommentCountResponse::new)
                .map(ApiResponse::success)
                .onErrorResume(Exception.class, error ->
                        Mono.just(ApiResponse.error(error.getMessage())));
    }

    @Operation(
            summary = "Получить количество комментариев пользователя",
            description = "Возвращает количество комментариев указанного пользователя"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Успешное получение количества",
            content = @Content(mediaType = "application/json")
    )
    @GetMapping("/count/user/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<CommentCountResponse>> getUserCommentCount(
            @Parameter(description = "Идентификатор пользователя", required = true, example = "123")
            @PathVariable Long userId) {
        return commentService.countByUserId(userId)
                .map(CommentCountResponse::new)
                .map(ApiResponse::success)
                .onErrorResume(Exception.class, error ->
                        Mono.just(ApiResponse.error(error.getMessage())));
    }

    @Operation(
            summary = "Обновить содержание комментария",
            description = "Обновляет только содержание комментария"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Содержание успешно обновлено",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Комментарий не найден",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PatchMapping("/{id}/content")
    public Mono<ResponseEntity<ApiResponse<Boolean>>> updateCommentContent(
            @Parameter(description = "Идентификатор комментария", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody UpdateContentRequest request) {

        return commentService.updateContent(id, request)
                .map(updated -> ResponseEntity.ok(
                        ApiResponse.success("Content updated successfully", updated)))
                .onErrorResume(Exception.class, error ->
                        Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error(error.getMessage()))));
    }

    @Operation(
            summary = "Получить последние комментарии пользователя",
            description = "Возвращает указанное количество последних комментариев пользователя"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Успешное получение комментариев",
            content = @Content(mediaType = "application/json")
    )
    @GetMapping("/user/{userId}/recent")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<List<CommentResponse>>> getUserRecentComments(
            @Parameter(description = "Идентификатор пользователя", required = true, example = "123")
            @PathVariable Long userId,
            @Parameter(description = "Количество комментариев", required = false, example = "10")
            @RequestParam(defaultValue = "10") Integer limit) {

        return commentService.findUserRecentComments(userId, limit)
                .map(commentMapper::toResponse)
                .collectList()
                .map(ApiResponse::success)
                .onErrorResume(Exception.class, error ->
                        Mono.just(ApiResponse.error(error.getMessage())));
    }

    @Operation(
            summary = "Получить последние комментарии изображения",
            description = "Возвращает указанное количество последних комментариев для изображения"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Успешное получение комментариев",
            content = @Content(mediaType = "application/json")
    )
    @GetMapping("/image/{imageId}/recent")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<List<CommentResponse>>> getImageRecentComments(
            @Parameter(description = "Идентификатор изображения", required = true, example = "456")
            @PathVariable Long imageId,
            @Parameter(description = "Количество комментариев", required = false, example = "10")
            @RequestParam(defaultValue = "10") Integer limit) {

        return commentService.findImageRecentComments(imageId, limit)
                .map(commentMapper::toResponse)
                .collectList()
                .map(ApiResponse::success)
                .onErrorResume(Exception.class, error ->
                        Mono.just(ApiResponse.error(error.getMessage())));
    }

    @Operation(
            summary = "Получить комментарии для изображения",
            description = "Альтернативный endpoint для получения комментариев изображения"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Успешное получение комментариев",
            content = @Content(mediaType = "application/json")
    )
    @GetMapping("/images/{imageId}/comments")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<List<CommentResponse>>> getCommentsForImage(
            @Parameter(description = "Идентификатор изображения", required = true, example = "456")
            @PathVariable Long imageId) {
        return commentService.findByImageId(imageId)
                .map(commentMapper::toResponse)
                .collectList()
                .map(ApiResponse::success)
                .onErrorResume(Exception.class, error ->
                        Mono.just(ApiResponse.error(error.getMessage())));
    }

    @Operation(
            summary = "Обновить комментарий изображения",
            description = "Обновляет комментарий для конкретного изображения"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Комментарий успешно обновлен",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Комментарий не найден",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PutMapping("/images/{imageId}/comments/{commentId}")
    public Mono<ResponseEntity<ApiResponse<CommentResponse>>> updateCommentForImage(
            @Parameter(description = "Идентификатор изображения", required = true, example = "456")
            @PathVariable Long imageId,
            @Parameter(description = "Идентификатор комментария", required = true, example = "1")
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

    @Operation(
            summary = "Удалить комментарий изображения",
            description = "Удаляет комментарий для конкретного изображения"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Комментарий успешно удален",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Комментарий не найден",
                    content = @Content(mediaType = "application/json")
            )
    })
    @DeleteMapping("/images/{imageId}/comments/{commentId}")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteCommentForImage(
            @Parameter(description = "Идентификатор изображения", required = true, example = "456")
            @PathVariable Long imageId,
            @Parameter(description = "Идентификатор комментария", required = true, example = "1")
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

    @Operation(
            summary = "Удалить все комментарии",
            description = "Удаляет все комментарии в системе (административная функция)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Все комментарии успешно удалены",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(mediaType = "application/json")
            )
    })
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
