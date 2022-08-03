package com.nowcoder.community.dao;

import com.nowcoder.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CommentMapper {
    //查询某一页的数据
    List<Comment> selectCommentsByEntity(int entityType, int entityId, int offset, int limit) ;

    //评论的总数
    int selectCountByEntity(int entityType, int entityId) ;

    //增加评论
    int insertComment(Comment comment) ;

    //根据id查Comment
    Comment selectCommentById(int id) ;
}
