package by.losik.commentlikeservice.mapping;

import by.losik.commentlikeservice.dto.LikeCountResponse;
import by.losik.commentlikeservice.dto.LikeRequest;
import by.losik.commentlikeservice.dto.LikeResponse;
import by.losik.commentlikeservice.entity.Like;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = "spring", imports = {LocalDateTime.class})
public interface LikeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(LocalDateTime.now())")
    Like toEntity(LikeRequest likeRequest);

    LikeResponse toResponse(Like like);

    List<LikeResponse> toResponseList(List<Like> likes);

    default Mono<LikeResponse> toMonoResponse(Mono<Like> likeMono) {
        return likeMono.map(this::toResponse);
    }

    default Flux<LikeResponse> toFluxResponse(Flux<Like> likeFlux) {
        return likeFlux.map(this::toResponse);
    }

    default LikeCountResponse toCountResponse(Long count) {
        return new LikeCountResponse(count);
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(LikeRequest likeRequest, @org.mapstruct.MappingTarget Like like);
}