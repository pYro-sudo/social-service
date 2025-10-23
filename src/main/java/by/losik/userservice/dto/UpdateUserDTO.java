package by.losik.userservice.dto;

import by.losik.userservice.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserDTO {

    @Pattern(regexp = "^\\w+$", message = "Username can only contain letters, numbers and underscores")
    private String username;

    @Email(message = "Email should be valid")
    private String email;

    private String password;
    private Role userRole;
    private Boolean enabled;
}