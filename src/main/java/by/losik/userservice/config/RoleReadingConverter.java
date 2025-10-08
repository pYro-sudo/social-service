package by.losik.userservice.config;

import by.losik.userservice.entity.Role;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
@Slf4j
public class RoleReadingConverter implements Converter<String, Role> {

    @Override
    public Role convert(@NonNull String source) {
        try {
            log.debug("Converting String to Role: {}", source);
            return Role.valueOf(source.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown role value from database: {}, defaulting to USER", source);
            return Role.USER;
        }
    }
}