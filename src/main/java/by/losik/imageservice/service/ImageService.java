package by.losik.imageservice.service;

import by.losik.imageservice.annotation.Loggable;
import by.losik.imageservice.entity.Image;
import by.losik.imageservice.exception.FileUploadException;
import by.losik.imageservice.exception.ImageNotFoundException;
import by.losik.imageservice.exception.S3OperationException;
import by.losik.imageservice.exception.ValidationException;
import by.losik.imageservice.repository.ImageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@Loggable(level = Loggable.Level.DEBUG, logResult = true)
@EnableCaching
public class ImageService {

    private final ImageRepository imageRepository;
    private final S3AsyncClient s3AsyncClient;

    @Value("${spring.cloud.aws.s3.bucket-name}")
    private String bucketName;

    @Value("${spring.cloud.aws.s3.endpoint}")
    private String s3PublicUrl;

    @Autowired
    public ImageService(ImageRepository imageRepository, S3AsyncClient s3AsyncClient) {
        this.imageRepository = imageRepository;
        this.s3AsyncClient = s3AsyncClient;
    }

    @Cacheable(value = "images", key = "#id", unless = "#result == null")
    public Mono<Image> findById(Long id) {
        return imageRepository.findById(id)
                .switchIfEmpty(Mono.error(new ImageNotFoundException(id)));
    }

    @Cacheable(value = "images", key = "#url", unless = "#result == null")
    public Mono<Image> findByUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return Mono.error(new ValidationException("URL cannot be empty", Set.of("URL is required")));
        }

        return imageRepository.findByUrl(url)
                .switchIfEmpty(Mono.error(new ImageNotFoundException("Image not found with URL: " + url)));
    }

    @Cacheable(value = "images", key = "'user_' + #userId", unless = "#result == null")
    public Flux<Image> findByUserId(Long userId) {
        if (userId == null || userId <= 0) {
            return Flux.error(new ValidationException("Invalid user ID", Set.of("User ID must be positive")));
        }

        return imageRepository.findByUserId(userId);
    }

    @CacheEvict(value = {"images", "stats"}, allEntries = true)
    public Mono<Image> save(Image image) {
        if (image == null) {
            return Mono.error(new ValidationException("Image cannot be null", Set.of("Image is required")));
        }

        if (!StringUtils.hasText(image.getUrl())) {
            return Mono.error(new ValidationException("Image URL cannot be empty", Set.of("URL is required")));
        }

        return imageRepository.save(image);
    }

    @CacheEvict(value = {"images", "stats"}, allEntries = true)
    public Mono<Void> deleteById(Long id) {
        return findById(id)
                .flatMap(image -> deleteFromS3(image.getUrl())
                        .then(imageRepository.deleteById(id)))
                .onErrorMap(S3Exception.class, e ->
                        new S3OperationException("Failed to delete image from S3", e));
    }

    public Mono<Image> update(Long id, @NonNull Image image) {
        if (!Objects.equals(id, image.getId())) {
            return Mono.error(new ValidationException("ID mismatch", Set.of("Path ID and body ID must match")));
        }

        return findById(id)
                .flatMap(existing -> {
                    image.setUploadedAt(existing.getUploadedAt());
                    return imageRepository.save(image);
                });
    }

    public Flux<Image> findByDescriptionContaining(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Flux.error(new ValidationException("Search keyword cannot be empty", Set.of("Keyword is required")));
        }

        return imageRepository.findByDescriptionContaining("%" + keyword + "%");
    }

    public Flux<Image> findByUploadedAtAfter(LocalDate date) {
        if (date == null) {
            return Flux.error(new ValidationException("Date cannot be null", Set.of("Date is required")));
        }

        if (date.isAfter(LocalDate.now())) {
            return Flux.error(new ValidationException("Date cannot be in the future", Set.of("Date must be in past")));
        }

        return imageRepository.findByUploadedAtAfter(date);
    }

    public Flux<Image> findByUploadedAtBefore(LocalDate date) {
        if (date == null) {
            return Flux.error(new ValidationException("Date cannot be null", Set.of("Date is required")));
        }

        return imageRepository.findByUploadedAtBefore(date);
    }

    public Flux<Image> findByUploadedAtBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return Flux.error(new ValidationException("Both start and end dates are required",
                    Set.of("Start date and end date are required")));
        }

        if (startDate.isAfter(endDate)) {
            return Flux.error(new ValidationException("Start date cannot be after end date",
                    Set.of("Start date must be before end date")));
        }

        return imageRepository.findByUploadedAtBetween(startDate, endDate);
    }

    public Mono<Boolean> existsByUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return Mono.error(new ValidationException("URL cannot be empty", Set.of("URL is required")));
        }

        return imageRepository.existsByUrl(url);
    }

    public Mono<Long> countByUserId(Long userId) {
        if (userId == null || userId <= 0) {
            return Mono.error(new ValidationException("Invalid user ID", Set.of("User ID must be positive")));
        }

        return imageRepository.countByUserId(userId);
    }

    public Mono<Void> deleteByUserId(Long userId) {
        if (userId == null || userId <= 0) {
            return Mono.error(new ValidationException("Invalid user ID", Set.of("User ID must be positive")));
        }

        return findByUserId(userId)
                .flatMap(image -> deleteFromS3(image.getUrl()))
                .then(imageRepository.deleteByUserId(userId))
                .onErrorMap(S3Exception.class, e ->
                        new S3OperationException("Failed to delete user images from S3", e));
    }

    public Mono<Boolean> updateDescription(Long id, String description) {
        if (id == null || id <= 0) {
            return Mono.error(new ValidationException("Invalid image ID", Set.of("Image ID must be positive")));
        }

        if (!StringUtils.hasText(description)) {
            return Mono.error(new ValidationException("Description cannot be empty", Set.of("Description is required")));
        }

        return imageRepository.updateDescription(id, description)
                .then(findById(id))
                .map(updatedImage -> description.equals(updatedImage.getDescription()))
                .defaultIfEmpty(false);
    }

    public Mono<Boolean> isUrlAvailable(String url) {
        return existsByUrl(url).map(exists -> !exists);
    }

    public Flux<Image> findUserRecentImages(Long userId, Integer limit) {
        if (userId == null || userId <= 0) {
            return Flux.error(new ValidationException("Invalid user ID", Set.of("User ID must be positive")));
        }

        if (limit == null || limit <= 0) {
            limit = 10;
        }

        return findByUserId(userId)
                .sort((img1, img2) -> img2.getUploadedAt().compareTo(img1.getUploadedAt()))
                .take(limit);
    }

    public Mono<Image> uploadImageWithBytes(@NonNull FilePart filePart, String description, Long userId) {
        validateFilePart(filePart);
        validateUserId(userId);

        String fileName = generateFileName(filePart.filename());
        String contentType = getContentType(filePart);

        return uploadToS3WithBytes(filePart, fileName, contentType)
                .flatMap(fileUrl -> saveImageToDatabase(fileUrl, description, userId))
                .onErrorMap(S3Exception.class, e ->
                        new S3OperationException("Failed to upload image to S3", e));
    }

    @NonNull
    private Mono<String> uploadToS3(@NonNull FilePart filePart, String fileName, String contentType) {
        return DataBufferUtils.join(filePart.content())
                .flatMap(dataBuffer -> {
                    try {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);

                        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                .bucket(bucketName)
                                .key(fileName)
                                .contentType(contentType)
                                .build();

                        return Mono.fromFuture(() ->
                                s3AsyncClient.putObject(putObjectRequest,
                                        AsyncRequestBody.fromBytes(bytes))
                        );
                    } catch (Exception e) {
                        DataBufferUtils.release(dataBuffer);
                        return Mono.error(new FileUploadException("Failed to process file data", e));
                    }
                })
                .then(Mono.fromCallable(() -> s3PublicUrl + "/" + bucketName + "/" + fileName))
                .onErrorMap(error -> new FileUploadException("Failed to upload file to S3: " + error.getMessage(), error));
    }

    @NonNull
    private Mono<String> uploadToS3WithBytes(@NonNull FilePart filePart, String fileName, String contentType) {
        return filePart.content()
                .collectList()
                .flatMap(dataBuffers -> {
                    try {
                        int totalSize = dataBuffers.stream()
                                .mapToInt(DataBuffer::readableByteCount)
                                .sum();

                        byte[] allBytes = new byte[totalSize];
                        int offset = 0;
                        for (DataBuffer dataBuffer : dataBuffers) {
                            ByteBuffer byteBuffer = dataBuffer.asByteBuffer();
                            int bytesToRead = dataBuffer.readableByteCount();
                            byteBuffer.get(allBytes, offset, bytesToRead);
                            offset += bytesToRead;
                            DataBufferUtils.release(dataBuffer);
                        }

                        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                .bucket(bucketName)
                                .key(fileName)
                                .contentType(contentType)
                                .build();

                        return Mono.fromFuture(() ->
                                s3AsyncClient.putObject(putObjectRequest,
                                        AsyncRequestBody.fromBytes(allBytes))
                        );
                    } catch (Exception e) {
                        dataBuffers.forEach(DataBufferUtils::release);
                        return Mono.error(new FileUploadException("Failed to process file data", e));
                    }
                })
                .then(Mono.fromCallable(() -> s3PublicUrl + "/" + fileName))
                .onErrorMap(error -> new FileUploadException("Failed to upload file to S3: " + error.getMessage(), error));
    }

    @NonNull
    private Mono<Void> deleteFromS3(String imageUrl) {
        return Mono.fromCallable(() -> {
                    String fileName = extractFileNameFromUrl(imageUrl);

                    DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(fileName)
                            .build();

                    return s3AsyncClient.deleteObject(deleteObjectRequest);
                })
                .then()
                .onErrorResume(S3Exception.class, e -> {
                    log.warn("Failed to delete file from S3: {}, but continuing with database deletion", e.getMessage());
                    return Mono.empty();
                });
    }

    @NonNull
    private Mono<Image> saveImageToDatabase(String fileUrl, String description, Long userId) {
        Image image = new Image();
        image.setUrl(fileUrl);
        image.setDescription(description != null ? description : "");
        image.setUploadedAt(LocalDate.now());
        image.setUserId(userId);

        return imageRepository.save(image);
    }

    @NonNull
    private String generateFileName(String originalFileName) {
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase();
        }
        return "users/" + UUID.randomUUID() + fileExtension;
    }

    @NonNull
    private String getContentType(@NonNull FilePart filePart) {
        return Objects.requireNonNull(filePart.headers().getContentType()).toString();
    }

    @NonNull
    private String extractFileNameFromUrl(@NonNull String imageUrl) {
        if (imageUrl.startsWith(s3PublicUrl + "/" + bucketName + "/")) {
            return imageUrl.substring((s3PublicUrl + "/" + bucketName + "/").length());
        }
        return imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
    }

    public Mono<Image> updateImageWithFile(Long id, FilePart filePart, String description) {
        validateFilePart(filePart);

        return findById(id)
                .flatMap(existingImage -> {
                    String oldFileUrl = existingImage.getUrl();

                    return uploadToS3(filePart, generateFileName(filePart.filename()), getContentType(filePart))
                            .flatMap(newUrl -> {
                                existingImage.setUrl(newUrl);
                                existingImage.setDescription(description);
                                return imageRepository.save(existingImage)
                                        .publishOn(Schedulers.boundedElastic())
                                        .doOnSuccess(updatedImage ->
                                                deleteFromS3(oldFileUrl)
                                        );
                            });
                })
                .onErrorMap(S3Exception.class, e ->
                        new S3OperationException("Failed to update image in S3", e));
    }

    public Flux<Image> uploadMultipleImages(@NonNull Flux<FilePart> fileParts, String description, Long userId) {
        validateUserId(userId);

        return fileParts
                .flatMap(filePart -> uploadImage(filePart, description, userId));
    }

    public Mono<Boolean> checkS3Connection() {
        return Mono.fromFuture(s3AsyncClient::listBuckets)
                .map(response -> true)
                .onErrorReturn(false);
    }

    public Mono<Map<String, Object>> getUserImageStats(Long userId) {
        validateUserId(userId);

        return countByUserId(userId)
                .flatMap(count -> findByUserId(userId)
                        .collectList()
                        .map(images -> Map.of(
                                "totalImages", count,
                                "recentUploads", images.stream()
                                        .sorted((img1, img2) -> img2.getUploadedAt().compareTo(img1.getUploadedAt()))
                                        .limit(5)
                                        .toList()
                        )));
    }

    public Mono<Image> uploadImage(FilePart filePart, String description, Long userId) {
        validateFilePart(filePart);
        validateUserId(userId);

        return validateFileSize(filePart)
                .then(uploadToS3(filePart, generateFileName(filePart.filename()), getContentType(filePart))
                        .flatMap(fileUrl -> saveImageToDatabase(fileUrl, description, userId))
                        .onErrorMap(S3Exception.class, e ->
                                new S3OperationException("Failed to upload image to S3", e)));
    }

    private Mono<Void> validateFileSize(FilePart filePart) {
        return filePart.content()
                .collectList()
                .flatMap(dataBuffers -> {
                    long totalSize = dataBuffers.stream()
                            .mapToLong(DataBuffer::readableByteCount)
                            .sum();

                    if (totalSize == 0) {
                        return Mono.error(new ValidationException("File is empty", Set.of("File cannot be empty")));
                    }

                    if (totalSize > 10 * 1024 * 1024) {
                        return Mono.error(new ValidationException("File too large", Set.of("File size exceeds 10MB")));
                    }

                    dataBuffers.forEach(DataBufferUtils::release);
                    return Mono.empty();
                });
    }

    public Flux<Image> findAll(int page, int size) {
        if (page < 0) {
            return Flux.error(new ValidationException("Page cannot be negative", Set.of("Page must be >= 0")));
        }

        if (size <= 0 || size > 100) {
            return Flux.error(new ValidationException("Size must be between 1 and 100", Set.of("Size must be 1-100")));
        }

        return imageRepository.findAll()
                .skip((long) page * size)
                .take(size);
    }

    public Flux<Image> findByUserId(Long userId, int page, int size) {
        validateUserId(userId);

        if (page < 0) {
            return Flux.error(new ValidationException("Page cannot be negative", Set.of("Page must be >= 0")));
        }

        if (size <= 0 || size > 100) {
            return Flux.error(new ValidationException("Size must be between 1 and 100", Set.of("Size must be 1-100")));
        }

        return imageRepository.findByUserId(userId)
                .skip((long) page * size)
                .take(size);
    }

    private void validateFilePart(FilePart filePart) {
        if (filePart == null) {
            throw new ValidationException("File cannot be null", Set.of("File is required"));
        }

        String filename = filePart.filename();
        if (!StringUtils.hasText(filename)) {
            throw new ValidationException("File name cannot be empty", Set.of("File name is required"));
        }

        if (filePart.headers().getContentLength() == 0) {
            throw new ValidationException("File cannot be empty", Set.of("File content is required"));
        }

        if (!isSupportedFileType(filename)) {
            throw new ValidationException("Unsupported file type", Set.of("File type not supported"));
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ValidationException("Invalid user ID", Set.of("User ID must be positive"));
        }
    }

    private boolean isSupportedFileType(String filename) {
        if (!StringUtils.hasText(filename)) {
            return false;
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp").contains(extension);
    }
}