package by.losik.commentlikeservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LikeRequest {
    @NotNull(message = "User ID cannot be null")
    private Long userId;

    @NotNull(message = "Image ID cannot be null")
    private Long imageId;
}