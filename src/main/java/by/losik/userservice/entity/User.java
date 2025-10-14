package by.losik.userservice.entity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table(schema = "user_schema", value = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {
    @Id
    @Column("id")
    private Long id;

    @Column("username")
    @Pattern(regexp = "^\\w+$", message = "Username can only contain letters, numbers and underscores")
    private String username;

    @Column("email")
    @Email
    private String email;

    @Column("password")
    private String password;

    @Column("user_role")
    private Role userRole;

    @Column("enabled")
    private Boolean enabled = true;
}