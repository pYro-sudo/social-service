package by.losik.userservice.config;

import by.losik.userservice.entity.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.lang.NonNull;

@WritingConverter
@Slf4j
public class RoleWritingConverter implements Converter<Role, String> {
    @Override
    public String convert(@NonNull Role source) {
        log.debug("Converting Role to String: {}", source);
        return source.name();
    }
}