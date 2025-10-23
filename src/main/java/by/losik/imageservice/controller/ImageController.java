package by.losik.imageservice.controller;

import by.losik.imageservice.annotation.Loggable;
import by.losik.imageservice.dto.ImageCreateDTO;
import by.losik.imageservice.dto.ImageResponseDTO;
import by.losik.imageservice.dto.ImageStatsDTO;
import by.losik.imageservice.dto.ImageUpdateDTO;
import by.losik.imageservice.entity.Image;
import by.losik.imageservice.mapping.ImageMapper;
import by.losik.imageservice.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@Loggable(level = Loggable.Level.DEBUG, logResult = true)
@RequiredArgsConstructor
@Tag(name = "Images", description = "API для управления изображениями и загрузки файлов")
public class ImageController {

    private final ImageService imageService;
    private final ImageMapper imageMapper;

    @Operation(
            summary = "Загрузить изображение",
            description = "Загружает одно изображение с описанием и привязывает к пользователю"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Изображение успешно загружено",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ImageResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверный формат файла или данные",
                    content = @Content
            )
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ImageResponseDTO> uploadImage(
            @Parameter(description = "Файл изображения", required = true)
            @RequestPart("file") FilePart file,
            @Parameter(description = "Описание изображения", required = true, example = "Красивый закат")
            @RequestPart("description") String description,
            @Parameter(description = "ID пользователя", required = true, example = "123")
            @RequestParam("userId") Long userId) {

        return imageService.uploadImage(file, description, userId)
                .map(imageMapper::toResponseDTO);
    }

    @Operation(
            summary = "Загрузить несколько изображений",
            description = "Загружает несколько изображений с одним описанием и привязывает к пользователю"
    )
    @ApiResponse(
            responseCode = "201",
            description = "Изображения успешно загружены",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ImageResponseDTO.class))
    )
    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Flux<ImageResponseDTO> uploadMultipleImages(
            @Parameter(description = "Файлы изображений", required = true)
            @RequestPart("files") Flux<FilePart> files,
            @Parameter(description = "Общее описание для всех изображений", required = true, example = "Фотографии с отпуска")
            @RequestPart("description") String description,
            @Parameter(description = "ID пользователя", required = true, example = "123")
            @RequestParam("userId") Long userId) {

        return imageService.uploadMultipleImages(files, description, userId)
                .map(imageMapper::toResponseDTO);
    }

    @Operation(
            summary = "Обновить изображение с файлом",
            description = "Обновляет изображение, заменяя файл и описание"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Изображение успешно обновлено",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ImageResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Изображение не найдено",
                    content = @Content
            )
    })
    @PutMapping(value = "/{id}/with-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ImageResponseDTO>> updateImageWithFile(
            @Parameter(description = "ID изображения", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Новый файл изображения", required = true)
            @RequestPart("file") FilePart file,
            @Parameter(description = "Новое описание", required = true, example = "Обновленное описание")
            @RequestPart("description") String description) {

        return imageService.updateImageWithFile(id, file, description)
                .map(image -> ResponseEntity.ok(imageMapper.toResponseDTO(image)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Получить изображение по ID",
            description = "Возвращает информацию об изображении по его идентификатору"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Изображение найдено",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ImageResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Изображение не найдено",
                    content = @Content
            )
    })
    @GetMapping("/{id}")
    public Mono<ResponseEntity<ImageResponseDTO>> getImageById(
            @Parameter(description = "ID изображения", required = true, example = "1")
            @PathVariable Long id) {
        return imageService.findById(id)
                .map(image -> ResponseEntity.ok(imageMapper.toResponseDTO(image)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Создать изображение",
            description = "Создает новую запись об изображении без загрузки файла"
    )
    @ApiResponse(
            responseCode = "201",
            description = "Изображение успешно создано",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ImageResponseDTO.class))
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ImageResponseDTO> createImage(
            @RequestBody ImageCreateDTO imageCreateDTO) {
        Image image = imageMapper.toEntity(imageCreateDTO);
        return imageService.save(image)
                .map(imageMapper::toResponseDTO);
    }

    @Operation(
            summary = "Обновить изображение",
            description = "Обновляет информацию об изображении"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Изображение успешно обновлено",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ImageResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Изображение не найдено",
                    content = @Content
            )
    })
    @PutMapping("/{id}")
    public Mono<ResponseEntity<ImageResponseDTO>> updateImage(
            @Parameter(description = "ID изображения", required = true, example = "1")
            @PathVariable Long id,
            @RequestBody ImageUpdateDTO imageUpdateDTO) {

        return imageService.findById(id)
                .flatMap(existingImage -> {
                    imageMapper.updateEntityFromDTO(imageUpdateDTO, existingImage);
                    return imageService.save(existingImage);
                })
                .map(updatedImage -> ResponseEntity.ok(imageMapper.toResponseDTO(updatedImage)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Удалить изображение",
            description = "Удаляет изображение по идентификатору"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Изображение успешно удалено"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Изображение не найдено"
            )
    })
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteImage(
            @Parameter(description = "ID изображения", required = true, example = "1")
            @PathVariable Long id) {
        return imageService.deleteById(id)
                .thenReturn(ResponseEntity.noContent().<Void>build())
                .onErrorResume(e -> Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(
            summary = "Получить последние изображения пользователя",
            description = "Возвращает указанное количество последних изображений пользователя"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Успешное получение изображений",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ImageResponseDTO.class))
    )
    @GetMapping("/user/{userId}/recent")
    public Flux<ImageResponseDTO> getUserRecentImages(
            @Parameter(description = "ID пользователя", required = true, example = "123")
            @PathVariable Long userId,
            @Parameter(description = "Количество изображений", required = false, example = "10")
            @RequestParam(defaultValue = "10") Integer limit) {
        return imageService.findUserRecentImages(userId, limit)
                .map(imageMapper::toResponseDTO);
    }

    @Operation(
            summary = "Удалить все изображения пользователя",
            description = "Удаляет все изображения, принадлежащие указанному пользователю"
    )
    @ApiResponse(
            responseCode = "204",
            description = "Все изображения пользователя успешно удалены"
    )
    @DeleteMapping("/user/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteAllUserImages(
            @Parameter(description = "ID пользователя", required = true, example = "123")
            @PathVariable Long userId) {
        return imageService.deleteByUserId(userId);
    }

    @Operation(
            summary = "Поиск изображений",
            description = "Ищет изображения по ключевому слову в описании"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Успешный поиск изображений",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ImageResponseDTO.class))
    )
    @GetMapping("/search")
    public Flux<ImageResponseDTO> searchImages(
            @Parameter(description = "Ключевое слово для поиска", required = true, example = "природа")
            @RequestParam String keyword) {
        return imageService.findByDescriptionContaining(keyword)
                .map(imageMapper::toResponseDTO);
    }

    @Operation(
            summary = "Получить изображения после даты",
            description = "Возвращает изображения, загруженные после указанной даты"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Успешное получение изображений",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ImageResponseDTO.class))
    )
    @GetMapping("/after/{date}")
    public Flux<ImageResponseDTO> getImagesAfterDate(
            @Parameter(description = "Начальная дата (формат: YYYY-MM-DD)", required = true, example = "2024-01-01")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return imageService.findByUploadedAtAfter(date)
                .map(imageMapper::toResponseDTO);
    }

    @Operation(
            summary = "Получить изображения до даты",
            description = "Возвращает изображения, загруженные до указанной даты"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Успешное получение изображений",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ImageResponseDTO.class))
    )
    @GetMapping("/before/{date}")
    public Flux<ImageResponseDTO> getImagesBeforeDate(
            @Parameter(description = "Конечная дата (формат: YYYY-MM-DD)", required = true, example = "2024-12-31")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return imageService.findByUploadedAtBefore(date)
                .map(imageMapper::toResponseDTO);
    }

    @Operation(
            summary = "Получить изображения за период",
            description = "Возвращает изображения, загруженные в указанном временном периоде"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Успешное получение изображений",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ImageResponseDTO.class))
    )
    @GetMapping("/between")
    public Flux<ImageResponseDTO> getImagesBetweenDates(
            @Parameter(description = "Начальная дата периода", required = true, example = "2024-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "Конечная дата периода", required = true, example = "2024-12-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return imageService.findByUploadedAtBetween(start, end)
                .map(imageMapper::toResponseDTO);
    }

    @Operation(
            summary = "Получить статистику изображений пользователя",
            description = "Возвращает статистику по изображениям пользователя"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешное получение статистики",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ImageStatsDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден",
                    content = @Content
            )
    })
    @GetMapping("/user/{userId}/stats")
    public Mono<ResponseEntity<ImageStatsDTO>> getUserImageStats(
            @Parameter(description = "ID пользователя", required = true, example = "123")
            @PathVariable Long userId) {
        return imageService.getUserImageStats(userId)
                .map(statsMap -> {
                    ImageStatsDTO statsDTO = new ImageStatsDTO();
                    statsDTO.setTotalImages((Long) statsMap.get("totalImages"));

                    @SuppressWarnings("unchecked")
                    List<Image> recentImages = (List<Image>) statsMap.get("recentUploads");
                    statsDTO.setRecentUploads(imageMapper.toResponseDTOList(recentImages));

                    return ResponseEntity.ok(statsDTO);
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Получить все изображения с пагинацией",
            description = "Возвращает все изображения с поддержкой пагинации"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Успешное получение изображений",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ImageResponseDTO.class))
    )
    @GetMapping
    public Flux<ImageResponseDTO> getAllImagesPaged(
            @Parameter(description = "Номер страницы", required = false, example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы", required = false, example = "20")
            @RequestParam(defaultValue = "20") int size) {
        return imageService.findAll(page, size)
                .map(imageMapper::toResponseDTO);
    }

    @Operation(
            summary = "Получить изображения пользователя с пагинацией",
            description = "Возвращает изображения пользователя с поддержкой пагинации"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Успешное получение изображений",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ImageResponseDTO.class))
    )
    @GetMapping("/user/{userId}")
    public Flux<ImageResponseDTO> getUserImagesPaged(
            @Parameter(description = "ID пользователя", required = true, example = "123")
            @PathVariable Long userId,
            @Parameter(description = "Номер страницы", required = false, example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы", required = false, example = "20")
            @RequestParam(defaultValue = "20") int size) {
        return imageService.findByUserId(userId, page, size)
                .map(imageMapper::toResponseDTO);
    }

    @Operation(
            summary = "Получить изображение по URL",
            description = "Возвращает информацию об изображении по его URL"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Изображение найдено",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ImageResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Изображение не найдено",
                    content = @Content
            )
    })
    @GetMapping("/url")
    public Mono<ResponseEntity<ImageResponseDTO>> getImageByUrl(
            @Parameter(description = "URL изображения", required = true, example = "https://example.com/image.jpg")
            @RequestParam String url) {
        return imageService.findByUrl(url)
                .map(image -> ResponseEntity.ok(imageMapper.toResponseDTO(image)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Проверить доступность URL",
            description = "Проверяет, доступен ли URL для использования"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Результат проверки доступности",
            content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"available\": true}"))
    )
    @GetMapping("/check/url")
    public Mono<ResponseEntity<Map<String, Boolean>>> checkUrlAvailability(
            @Parameter(description = "URL для проверки", required = true, example = "https://example.com/image.jpg")
            @RequestParam String url) {
        return imageService.isUrlAvailable(url)
                .map(available -> ResponseEntity.ok(Map.of("available", available)));
    }

    @Operation(
            summary = "Получить количество изображений пользователя",
            description = "Возвращает количество изображений, принадлежащих пользователю"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Успешное получение количества",
            content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"count\": 15}"))
    )
    @GetMapping("/count/user/{userId}")
    public Mono<ResponseEntity<Map<String, Long>>> getUserImageCount(
            @Parameter(description = "ID пользователя", required = true, example = "123")
            @PathVariable Long userId) {
        return imageService.countByUserId(userId)
                .map(count -> ResponseEntity.ok(Map.of("count", count)));
    }

    @Operation(
            summary = "Обновить описание изображения",
            description = "Обновляет только описание изображения"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Описание успешно обновлено",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"updated\": true}"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Изображение не найдено",
                    content = @Content
            )
    })
    @PatchMapping("/{id}/description")
    public Mono<ResponseEntity<Map<String, Boolean>>> updateImageDescription(
            @Parameter(description = "ID изображения", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Новое описание", required = true, example = "Новое описание изображения")
            @RequestParam String description) {
        return imageService.updateDescription(id, description)
                .map(success -> ResponseEntity.ok(Map.of("updated", success)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Проверить подключение к S3",
            description = "Проверяет соединение с Amazon S3 хранилищем"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Результат проверки подключения",
            content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"connected\": true}"))
    )
    @GetMapping("/health/s3")
    public Mono<ResponseEntity<Map<String, Boolean>>> checkS3Connection() {
        return imageService.checkS3Connection()
                .map(connected -> ResponseEntity.ok(Map.of("connected", connected)));
    }

    @Operation(
            summary = "Загрузить изображение в байтовом формате",
            description = "Альтернативный метод загрузки изображения с обработкой байтов"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Изображение успешно загружено",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ImageResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ошибка при загрузке",
                    content = @Content
            )
    })
    @PostMapping(value = "/upload-bytes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ImageResponseDTO>> uploadImageWithBytes(
            @Parameter(description = "Файл изображения", required = true)
            @RequestPart("file") FilePart filePart,
            @Parameter(description = "Описание изображения", required = false, example = "Описание изображения")
            @RequestPart(value = "description", required = false) String description,
            @Parameter(description = "ID пользователя", required = true, example = "123")
            @RequestParam("userId") Long userId) {

        return imageService.uploadImageWithBytes(filePart,
                        description != null ? description : "",
                        userId)
                .map(image -> ResponseEntity.ok().body(imageMapper.toResponseDTO(image)))
                .onErrorResume(error -> Mono.just(ResponseEntity.badRequest().build()));
    }

}
