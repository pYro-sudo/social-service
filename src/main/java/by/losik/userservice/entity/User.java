package by.losik.userservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table(schema = "user_schema", value = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @Id
    @Column("id")
    private Long id;

    @Column("username")
    private String username;

    @Column("email")
    private String email;

    @Column("password")
    private String password;

    @Column("user_role")
    private Role userRole;

    @Column("enabled")
    private Boolean enabled = true;
}