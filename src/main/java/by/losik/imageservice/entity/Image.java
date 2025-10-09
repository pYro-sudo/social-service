package by.losik.imageservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

@Table(schema = "gallery", value = "images")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Image {
    @Id
    @Column("id")
    private Long id;

    @Column("url")
    private String url;

    @Column("description")
    private String description;

    @Column("uploaded_at")
    private LocalDate uploadedAt;

    @Column("user_id")
    private Long userId;
}