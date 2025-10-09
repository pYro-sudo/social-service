package by.losik.imageservice.service;

import by.losik.imageservice.annotation.Loggable;
import by.losik.imageservice.entity.Image;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.Map;
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
        return imageRepository.findById(id);
    }

    @Cacheable(value = "images", key = "#url", unless = "#result == null")
    public Mono<Image> findByUrl(String url) {
        return imageRepository.findByUrl(url);
    }

    @Cacheable(value = "images", key = "'user_' + #userId", unless = "#result == null")
    public Flux<Image> findByUserId(Long userId) {
        return imageRepository.findByUserId(userId);
    }

    @CacheEvict(value = {"images", "stats"}, allEntries = true)
    public Mono<Image> save(Image image) {
        return imageRepository.save(image);
    }

    @CacheEvict(value = {"images", "stats"}, allEntries = true)
    public Mono<Void> deleteById(Long id) {
        return findById(id)
                .flatMap(image -> deleteFromS3(image.getUrl())
                        .then(imageRepository.deleteById(id)));
    }

    public Mono<Image> update(Long id, @NonNull Image image) {
        image.setId(id);
        return imageRepository.save(image);
    }

    public Flux<Image> findByDescriptionContaining(String keyword) {
        return imageRepository.findByDescriptionContaining("%" + keyword + "%");
    }

    public Flux<Image> findByUploadedAtAfter(LocalDate date) {
        return imageRepository.findByUploadedAtAfter(date);
    }

    public Flux<Image> findByUploadedAtBefore(LocalDate date) {
        return imageRepository.findByUploadedAtBefore(date);
    }

    public Flux<Image> findByUploadedAtBetween(LocalDate startDate, LocalDate endDate) {
        return imageRepository.findByUploadedAtBetween(startDate, endDate);
    }

    public Mono<Boolean> existsByUrl(String url) {
        return imageRepository.existsByUrl(url);
    }

    public Mono<Long> countByUserId(Long userId) {
        return imageRepository.countByUserId(userId);
    }

    public Mono<Void> deleteByUserId(Long userId) {
        return findByUserId(userId)
                .flatMap(image -> deleteFromS3(image.getUrl()))
                .then(imageRepository.deleteByUserId(userId));
    }

    public Mono<Boolean> updateDescription(Long id, String description) {
        return imageRepository.updateDescription(id, description)
                .map(count -> count > 0)
                .defaultIfEmpty(false);
    }

    public Mono<Boolean> isUrlAvailable(String url) {
        return existsByUrl(url).map(exists -> !exists);
    }

    public Flux<Image> findUserRecentImages(Long userId, Integer limit) {
        return findByUserId(userId)
                .sort((img1, img2) -> img2.getUploadedAt().compareTo(img1.getUploadedAt()))
                .take(limit);
    }

    public Mono<Image> uploadImage(FilePart filePart, String description, Long userId) {
        return uploadToS3(filePart, generateFileName(filePart.filename()), getContentType(filePart))
                .flatMap(fileUrl -> saveImageToDatabase(fileUrl, description, userId));
    }

    public Mono<Image> uploadImageWithBytes(@NonNull FilePart filePart, String description, Long userId) {
        String fileName = generateFileName(filePart.filename());
        String contentType = getContentType(filePart);

        return uploadToS3WithBytes(filePart, fileName, contentType)
                .flatMap(fileUrl -> saveImageToDatabase(fileUrl, description, userId));
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
                        return Mono.error(new RuntimeException("Failed to process file data", e));
                    }
                })
                .then(Mono.fromCallable(() -> s3PublicUrl + "/" + bucketName + "/" + fileName))
                .onErrorMap(error -> new RuntimeException("Failed to upload file to S3: " + error.getMessage(), error));
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
                        return Mono.error(new RuntimeException("Failed to process file data", e));
                    }
                })
                .then(Mono.fromCallable(() -> s3PublicUrl + "/" + fileName));
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
                .onErrorResume(error -> Mono.empty());
    }

    @NonNull
    private Mono<Image> saveImageToDatabase(String fileUrl, String description, Long userId) {
        return Mono.just(new Image()).flatMap(image1 -> {
            image1.setUrl(fileUrl);
            image1.setDescription(description);
            image1.setUploadedAt(LocalDate.now());
            image1.setUserId(userId);
            return imageRepository.save(image1);
        });
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
        filePart.headers().getContentType();
        return filePart.headers().getContentType().toString();
    }

    @NonNull
    private String extractFileNameFromUrl(@NonNull String imageUrl) {
        if (imageUrl.startsWith(s3PublicUrl + "/" + bucketName + "/")) {
            return imageUrl.substring((s3PublicUrl + "/" + bucketName + "/").length());
        }
        return imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
    }

    public Mono<Image> updateImageWithFile(Long id, FilePart filePart, String description) {
        return findById(id)
                .flatMap(existingImage -> {
                    String oldFileUrl = existingImage.getUrl();

                    return uploadToS3(filePart, generateFileName(filePart.filename()), getContentType(filePart))
                            .flatMap(newUrl -> {
                                existingImage.setUrl(newUrl);
                                existingImage.setDescription(description);
                                return imageRepository.save(existingImage)
                                        .publishOn(Schedulers.boundedElastic())
                                        .doOnSuccess(updatedImage -> deleteFromS3(oldFileUrl).subscribe());
                            });
                });
    }

    public Flux<Image> uploadMultipleImages(@NonNull Flux<FilePart> fileParts, String description, Long userId) {
        return fileParts
                .flatMap(filePart -> uploadImage(filePart, description, userId));
    }

    public Mono<Boolean> checkS3Connection() {
        return Mono.fromFuture(s3AsyncClient::listBuckets)
                .map(response -> true)
                .onErrorReturn(false);
    }

    public Mono<Map<String, Object>> getUserImageStats(Long userId) {
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

    public Flux<Image> findAll(int page, int size) {
        return imageRepository.findAll()
                .skip((long) page * size)
                .take(size);
    }

    public Flux<Image> findByUserId(Long userId, int page, int size) {
        return imageRepository.findByUserId(userId)
                .skip((long) page * size)
                .take(size);
    }
}