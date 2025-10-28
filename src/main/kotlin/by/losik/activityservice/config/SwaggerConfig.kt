package by.losik.activityservice.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {
    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Activity API")
                    .version("1.0")
                    .description("API Endpoints")
                    .contact(
                        Contact()
                            .name("Yaroslav")
                            .email("losik2006@gmail.com")
                    )
            )
    }
}