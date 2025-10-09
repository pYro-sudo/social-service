package by.losik.imageservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Component
public class S3BucketInitializer implements CommandLineRunner {

    private final S3AsyncClient s3Client;

    @Value("${spring.cloud.aws.s3.bucket-name}")
    private String bucketName;

    @Autowired
    public S3BucketInitializer(S3AsyncClient s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public void run(String... args) {
        initializeBucket().subscribe(
                success -> {
                    if (success) {
                        log.info("S3 bucket initialization completed successfully");
                    } else {
                        log.warn("S3 bucket initialization completed with warnings");
                    }
                },
                error -> log.error("S3 bucket initialization failed: {}", error.getMessage())
        );
    }

    public Mono<Boolean> initializeBucket() {
        return checkBucketExists()
                .flatMap(exists -> {
                    if (exists) {
                        log.info("S3 bucket '{}' already exists", bucketName);
                        return Mono.just(true);
                    } else {
                        return createBucket()
                                .doOnSuccess(v -> log.info("S3 bucket '{}' created successfully", bucketName))
                                .thenReturn(true)
                                .onErrorReturn(false);
                    }
                })
                .onErrorReturn(false);
    }

    @NonNull
    private Mono<Boolean> checkBucketExists() {
        return Mono.fromFuture(() ->
                        s3Client.headBucket(HeadBucketRequest.builder()
                                .bucket(bucketName)
                                .build())
                )
                .thenReturn(true)
                .onErrorResume(S3Exception.class, e -> {
                    if (e.statusCode() == 404) {
                        return Mono.just(false);
                    }
                    log.error("Error checking S3 bucket '{}': {}", bucketName, e.getMessage());
                    return Mono.error(e);
                });
    }

    @NonNull
    private Mono<Void> createBucket() {
        return Mono.fromFuture(() -> {
            CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            return s3Client.createBucket(createBucketRequest);
        }).then();
    }
}