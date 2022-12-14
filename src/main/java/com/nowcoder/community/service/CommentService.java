package com.nowcoder.community.service;

import com.nowcoder.community.dao.CommentMapper;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class CommentService implements CommunityConstant {

    @Autowired
    private CommentMapper commentMapper ;

    @Autowired
    private SensitiveFilter sensitiveFilter ;

    @Autowired
    private DiscussPostService discussPostService ;

    //查询某一页的数据
    public List<Comment> findCommentByEntity(int entityType, int entityId, int offset, int limit){
        return commentMapper.selectCommentsByEntity(entityType, entityId, offset, limit) ;
    }

    public int findCommentCount(int entityType, int entityId){
        return commentMapper.selectCountByEntity(entityType, entityId) ;
    }

    //增加评论+更新帖子评论数量
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public int addComment(Comment comment){
        if (comment == null){
            throw new IllegalArgumentException("参数不能为空！") ;
        }
        //增加评论
        comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
        comment.setContent(sensitiveFilter.filter(comment.getContent()));
        int rows = commentMapper.insertComment(comment) ;

        //更新帖子评论数量。从评论表查到这个帖子一共有多少条评论，然后将其插入到帖子中
        if (comment.getEntityType() == ENTITY_TYPE_POST){
            int count = commentMapper.selectCountByEntity(comment.getEntityType(), comment.getEntityId());
            discussPostService.updateCommentCount(comment.getEntityId(), count) ;
        }
        return rows ;
    }

    public Comment findCommentById(int id){
        return commentMapper.selectCommentById(id) ;
    }
}
