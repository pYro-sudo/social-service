package by.losik.imageservice.controller;

import by.losik.imageservice.annotation.Loggable;
import by.losik.imageservice.entity.Image;
import by.losik.imageservice.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@Loggable(level = Loggable.Level.DEBUG, logResult = true)
public class ImageController {

    private final ImageService imageService;

    @Autowired
    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Image> uploadImage(
            @RequestPart("file") FilePart file,
            @RequestPart("description") String description,
            @RequestParam("userId") Long userId) {

        return imageService.uploadImage(file, description, userId);
    }

    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Flux<Image> uploadMultipleImages(
            @RequestPart("files") Flux<FilePart> files,
            @RequestPart("description") String description,
            @RequestParam("userId") Long userId) {

        return imageService.uploadMultipleImages(files, description, userId);
    }

    @PutMapping(value = "/{id}/with-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<Image>> updateImageWithFile(
            @PathVariable Long id,
            @RequestPart("file") FilePart file,
            @RequestPart("description") String description) {

        return imageService.updateImageWithFile(id, file, description)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Image>> getImageById(@PathVariable Long id) {
        return imageService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Image> createImage(@RequestBody Image image) {
        return imageService.save(image);
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Image>> updateImage(@PathVariable Long id, @RequestBody Image image) {
        return imageService.update(id, image)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteImage(@PathVariable Long id) {
        return imageService.deleteById(id)
                .thenReturn(ResponseEntity.noContent().<Void>build())
                .onErrorResume(e -> Mono.just(ResponseEntity.notFound().build()));
    }

    @GetMapping("/user/{userId}/recent")
    public Flux<Image> getUserRecentImages(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") Integer limit) {
        return imageService.findUserRecentImages(userId, limit);
    }

    @DeleteMapping("/user/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteAllUserImages(@PathVariable Long userId) {
        return imageService.deleteByUserId(userId);
    }

    @GetMapping("/search")
    public Flux<Image> searchImages(@RequestParam String keyword) {
        return imageService.findByDescriptionContaining(keyword);
    }

    @GetMapping("/url/{url}")
    public Mono<ResponseEntity<Image>> getImageByUrl(@PathVariable String url) {
        return imageService.findByUrl(url)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/after/{date}")
    public Flux<Image> getImagesAfterDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return imageService.findByUploadedAtAfter(date);
    }

    @GetMapping("/before/{date}")
    public Flux<Image> getImagesBeforeDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return imageService.findByUploadedAtBefore(date);
    }

    @GetMapping("/between")
    public Flux<Image> getImagesBetweenDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return imageService.findByUploadedAtBetween(start, end);
    }

    @GetMapping("/check/url/{url}")
    public Mono<ResponseEntity<Map<String, Boolean>>> checkUrlAvailability(@PathVariable String url) {
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

    @GetMapping("/user/{userId}/stats")
    public Mono<ResponseEntity<Map<String, Object>>> getUserImageStats(@PathVariable Long userId) {
        return imageService.getUserImageStats(userId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/health/s3")
    public Mono<ResponseEntity<Map<String, Boolean>>> checkS3Connection() {
        return imageService.checkS3Connection()
                .map(connected -> ResponseEntity.ok(Map.of("connected", connected)));
    }

    @PostMapping(value = "/upload-bytes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<Image>> uploadImageWithBytes(
            @RequestPart("file") FilePart filePart,
            @RequestPart(value = "description", required = false) String description,
            @RequestParam("userId") Long userId) {

        return imageService.uploadImageWithBytes(filePart,
                        description != null ? description : "",
                        userId)
                .map(image -> ResponseEntity.ok().body(image))
                .onErrorResume(error -> Mono.just(ResponseEntity.badRequest().build()));
    }

    @GetMapping
    public Flux<Image> getAllImagesPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return imageService.findAll(page, size);
    }

    @GetMapping("/user/{userId}")
    public Flux<Image> getUserImagesPaged(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return imageService.findByUserId(userId, page, size);
    }
}