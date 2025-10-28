package by.losik.commentlikeservice.mapping;

import by.losik.commentlikeservice.dto.CommentRequest;
import by.losik.commentlikeservice.dto.CommentResponse;
import by.losik.commentlikeservice.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Comment toEntity(CommentRequest commentRequest);

    CommentResponse toResponse(Comment comment);

    List<CommentResponse> toResponseList(List<Comment> comments);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromRequest(CommentRequest commentRequest, @org.mapstruct.MappingTarget Comment comment);
}