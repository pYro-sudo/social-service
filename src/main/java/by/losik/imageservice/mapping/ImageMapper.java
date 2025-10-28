package by.losik.imageservice.mapping;

import by.losik.imageservice.dto.ImageCreateDTO;
import by.losik.imageservice.dto.ImageDTO;
import by.losik.imageservice.dto.ImageResponseDTO;
import by.losik.imageservice.dto.ImageUpdateDTO;
import by.losik.imageservice.dto.ImageUploadDTO;
import by.losik.imageservice.entity.Image;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ImageMapper {

    ImageDTO toDTO(Image image);
    ImageResponseDTO toResponseDTO(Image image);

    Image toEntity(ImageDTO imageDTO);
    @Mapping(target = "uploadedAt", expression = "java(java.time.LocalDate.now())")
    Image toEntity(ImageCreateDTO imageCreateDTO);
    Image toEntity(ImageUpdateDTO imageUpdateDTO);

    List<ImageDTO> toDTOList(List<Image> images);
    List<ImageResponseDTO> toResponseDTOList(List<Image> images);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uploadedAt", ignore = true)
    @Mapping(target = "userId", ignore = true)
    void updateEntityFromDTO(ImageUpdateDTO dto, @MappingTarget Image entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uploadedAt", ignore = true)
    Image createDTOToEntity(ImageCreateDTO dto);

    @Mapping(target = "uploadedAt", expression = "java(java.time.LocalDate.now())")
    Image uploadDTOToEntity(ImageUploadDTO dto, String url);

    default Image toEntity(ImageUploadDTO uploadDTO, String fileUrl) {
        if (uploadDTO == null && fileUrl == null) {
            return null;
        }

        Image image = new Image();
        if (uploadDTO != null) {
            image.setDescription(uploadDTO.getDescription());
            image.setUserId(uploadDTO.getUserId());
        }
        image.setUrl(fileUrl);
        image.setUploadedAt(java.time.LocalDate.now());

        return image;
    }
}