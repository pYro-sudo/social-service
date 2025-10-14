package by.losik.userservice.dto;

import by.losik.userservice.entity.Role;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long id;

    @Pattern(regexp = "^\\w+$", message = "Username can only contain letters, numbers and underscores")
    private String username;

    private String email;
    private Role userRole;
    private Boolean enabled;
}