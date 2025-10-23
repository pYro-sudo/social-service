package by.losik.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;

import java.util.Arrays;

@Configuration
public class DatabaseConfig {

    @Bean
    public R2dbcCustomConversions customConversions() {
        return R2dbcCustomConversions.of(PostgresDialect.INSTANCE,
                Arrays.asList(
                        new RoleReadingConverter(),
                        new RoleWritingConverter()
                ));
    }
}