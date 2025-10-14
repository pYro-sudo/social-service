package by.losik.imageservice.controller;

import by.losik.imageservice.annotation.Loggable;
import by.losik.imageservice.dto.ImageCreateDTO;
import by.losik.imageservice.dto.ImageResponseDTO;
import by.losik.imageservice.dto.ImageStatsDTO;
import by.losik.imageservice.dto.ImageUpdateDTO;
import by.losik.imageservice.entity.Image;
import by.losik.imageservice.mapping.ImageMapper;
import by.losik.imageservice.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@Loggable(level = Loggable.Level.DEBUG, logResult = true)
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;
    private final ImageMapper imageMapper;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ImageResponseDTO> uploadImage(
            @RequestPart("file") FilePart file,
            @RequestPart("description") String description,
            @RequestParam("userId") Long userId) {

        return imageService.uploadImage(file, description, userId)
                .map(imageMapper::toResponseDTO);
    }

    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Flux<ImageResponseDTO> uploadMultipleImages(
            @RequestPart("files") Flux<FilePart> files,
            @RequestPart("description") String description,
            @RequestParam("userId") Long userId) {

        return imageService.uploadMultipleImages(files, description, userId)
                .map(imageMapper::toResponseDTO);
    }

    @PutMapping(value = "/{id}/with-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ImageResponseDTO>> updateImageWithFile(
            @PathVariable Long id,
            @RequestPart("file") FilePart file,
            @RequestPart("description") String description) {

        return imageService.updateImageWithFile(id, file, description)
                .map(image -> ResponseEntity.ok(imageMapper.toResponseDTO(image)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ImageResponseDTO>> getImageById(@PathVariable Long id) {
        return imageService.findById(id)
                .map(image -> ResponseEntity.ok(imageMapper.toResponseDTO(image)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ImageResponseDTO> createImage(@RequestBody ImageCreateDTO imageCreateDTO) {
        Image image = imageMapper.toEntity(imageCreateDTO);
        return imageService.save(image)
                .map(imageMapper::toResponseDTO);
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ImageResponseDTO>> updateImage(
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

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteImage(@PathVariable Long id) {
        return imageService.deleteById(id)
                .thenReturn(ResponseEntity.noContent().<Void>build())
                .onErrorResume(e -> Mono.just(ResponseEntity.notFound().build()));
    }

    @GetMapping("/user/{userId}/recent")
    public Flux<ImageResponseDTO> getUserRecentImages(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") Integer limit) {
        return imageService.findUserRecentImages(userId, limit)
                .map(imageMapper::toResponseDTO);
    }

    @DeleteMapping("/user/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteAllUserImages(@PathVariable Long userId) {
        return imageService.deleteByUserId(userId);
    }

    @GetMapping("/search")
    public Flux<ImageResponseDTO> searchImages(@RequestParam String keyword) {
        return imageService.findByDescriptionContaining(keyword)
                .map(imageMapper::toResponseDTO);
    }

    @GetMapping("/after/{date}")
    public Flux<ImageResponseDTO> getImagesAfterDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return imageService.findByUploadedAtAfter(date)
                .map(imageMapper::toResponseDTO);
    }

    @GetMapping("/before/{date}")
    public Flux<ImageResponseDTO> getImagesBeforeDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return imageService.findByUploadedAtBefore(date)
                .map(imageMapper::toResponseDTO);
    }

    @GetMapping("/between")
    public Flux<ImageResponseDTO> getImagesBetweenDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return imageService.findByUploadedAtBetween(start, end)
                .map(imageMapper::toResponseDTO);
    }

    @GetMapping("/user/{userId}/stats")
    public Mono<ResponseEntity<ImageStatsDTO>> getUserImageStats(@PathVariable Long userId) {
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

    @GetMapping
    public Flux<ImageResponseDTO> getAllImagesPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return imageService.findAll(page, size)
                .map(imageMapper::toResponseDTO);
    }

    @GetMapping("/user/{userId}")
    public Flux<ImageResponseDTO> getUserImagesPaged(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return imageService.findByUserId(userId, page, size)
                .map(imageMapper::toResponseDTO);
    }

    @GetMapping("/url")
    public Mono<ResponseEntity<ImageResponseDTO>> getImageByUrl(@RequestParam String url) {
        return imageService.findByUrl(url)
                .map(image -> ResponseEntity.ok(imageMapper.toResponseDTO(image)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/check/url")
    public Mono<ResponseEntity<Map<String, Boolean>>> checkUrlAvailability(@RequestParam String url) {
        return imageService.isUrlAvailable(url)
                .map(available -> ResponseEntity.ok(Map.of("available", available)));
    }

    @GetMapping("/count/user/{userId}")
    public Mono<ResponseEntity<Map<String, Long>>> getUserImageCount(@PathVariable Long userId) {
        return imageService.countByUserId(userId)
                .map(count -> ResponseEntity.ok(Map.of("count", count)));
    }

    @PatchMapping("/{id}/description")
    public Mono<ResponseEntity<Map<String, Boolean>>> updateImageDescription(
            @PathVariable Long id,
            @RequestParam String description) {
        return imageService.updateDescription(id, description)
                .map(success -> ResponseEntity.ok(Map.of("updated", success)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/health/s3")
    public Mono<ResponseEntity<Map<String, Boolean>>> checkS3Connection() {
        return imageService.checkS3Connection()
                .map(connected -> ResponseEntity.ok(Map.of("connected", connected)));
    }

    @PostMapping(value = "/upload-bytes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ImageResponseDTO>> uploadImageWithBytes(
            @RequestPart("file") FilePart filePart,
            @RequestPart(value = "description", required = false) String description,
            @RequestParam("userId") Long userId) {

        return imageService.uploadImageWithBytes(filePart,
                        description != null ? description : "",
                        userId)
                .map(image -> ResponseEntity.ok().body(imageMapper.toResponseDTO(image)))
                .onErrorResume(error -> Mono.just(ResponseEntity.badRequest().build()));
    }
}