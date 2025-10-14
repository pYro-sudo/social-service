package by.losik.userservice.mapping;

import by.losik.userservice.dto.CreateUserDTO;
import by.losik.userservice.dto.UpdateUserDTO;
import by.losik.userservice.dto.UserDTO;
import by.losik.userservice.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    UserDTO toDTO(User user);

    User toEntity(UserDTO userDTO);

    User toEntity(CreateUserDTO createUserDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromDTO(UpdateUserDTO updateUserDTO, @MappingTarget User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromDTO(UserDTO userDTO, @MappingTarget User user);

    @AfterMapping
    default void afterMapping(@MappingTarget User user) {
        if (user.getEnabled() == null) {
            user.setEnabled(true);
        }
    }
}