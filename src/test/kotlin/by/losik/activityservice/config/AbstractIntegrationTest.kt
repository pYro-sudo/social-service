package by.losik.activityservice.config

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AbstractIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val mongoDBContainer: MongoDBContainer = MongoDBContainer(DockerImageName.parse("mongo:6.0"))
            .withReuse(true)

        @Container
        @JvmStatic
        val kafkaContainer: KafkaContainer = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.8.0"))
            .withEmbeddedZookeeper()
            .withReuse(true)

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.mongodb.uri") { mongoDBContainer.connectionString }
            registry.add("spring.kafka.bootstrap-servers") { kafkaContainer.bootstrapServers }
            registry.add("kafka.topics.activity-events") { "test-activity-events" }
            registry.add("spring.kafka.consumer.auto-offset-reset") { "earliest" }
            registry.add("spring.kafka.consumer.group-id") { "test-group" }
            registry.add("spring.kafka.listener.auto-startup") { "true" }
        }
    }
}