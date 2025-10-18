package by.losik.activityservice.service

import by.losik.activityservice.config.AbstractIntegrationTest
import by.losik.activityservice.entity.ActivityEventType
import by.losik.activityservice.entity.CommentEvent
import by.losik.activityservice.entity.LikeEvent
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.test.utils.KafkaTestUtils
import reactor.test.StepVerifier

class ActivityEventConsumerTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var activityEventService: ActivityEventService

    private fun createProducer(): Producer<String, Any> {
        val producerProps = KafkaTestUtils.producerProps(kafkaContainer.bootstrapServers)
        return DefaultKafkaProducerFactory<String, Any>(producerProps).createProducer()
    }

    @Test
    fun `should consume and process like event`() {
        val likeEvent = LikeEvent(
            userId = 1L,
            imageId = 100L,
            eventType = ActivityEventType.ADD_LIKE
        )

        val producer = createProducer()
        producer.send(ProducerRecord("test-activity-events", likeEvent))
        producer.close()

        Thread.sleep(3000)

        StepVerifier.create(activityEventService.findByUserId(1L))
            .expectNextMatches { event ->
                event.userId == 1L &&
                        event.imageId == 100L &&
                        event.type == ActivityEventType.ADD_LIKE
            }
            .verifyComplete()
    }

    @Test
    fun `should consume and process comment event`() {
        val commentEvent = CommentEvent(
            userId = 2L,
            imageId = 200L,
            content = "Test comment",
            eventType = ActivityEventType.CREATE_COMMENT
        )

        val producer = createProducer()
        producer.send(ProducerRecord("test-activity-events", commentEvent))
        producer.close()

        Thread.sleep(3000)

        StepVerifier.create(activityEventService.findByUserId(2L))
            .expectNextMatches { event ->
                event.userId == 2L &&
                        event.imageId == 200L &&
                        event.type == ActivityEventType.CREATE_COMMENT &&
                        event.content == "Test comment"
            }
            .verifyComplete()
    }
}