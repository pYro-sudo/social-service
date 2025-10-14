package by.losik.imageservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ImageResponseDTO {
    private Long id;
    private String url;
    private String description;
    private LocalDate uploadedAt;
    private Long userId;
}