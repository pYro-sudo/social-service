package by.losik.imageservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ImageStatsDTO {
    private Long totalImages;
    private List<ImageResponseDTO> recentUploads;
}