package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController implements CommunityConstant {

    @Autowired
    private CommentService commentService ;

    @Autowired
    private HostHolder hostHolder ;

    @Autowired
    private EventProducer eventProducer ;

    @Autowired
    private DiscussPostService discussPostService ;

    @Autowired
    private RedisTemplate redisTemplate ;

    /** 添加评论
     * 页面上就是提交评论的内容content，另外还有两个隐含的，评论的类型和id，因此形参写comment，这些参数直接都封装到comment中
     * comment中有个字段为userId，那么需要获取当前用户，因此需要注入HostHolder
     * @param discussPostId
     * @param comment
     * @return
     */
    @PostMapping(path = "/add/{discussPostId}")
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment){
        comment.setUserId(hostHolder.getUser().getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        commentService.addComment(comment) ;

        //触发评论事件
        Event event = new Event()
                .setTopic(TOPIC_COMMENT) //体现出前面改造set方法的好处了，可以链式编程
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(comment.getEntityType())
                .setEntityId(comment.getEntityId())
                .setData("postId", discussPostId) ; //页面上还需要链到帖子，因此还需要帖子的id(不是每个事件都需要帖子id，因此放到map中)
        //还需要实体的作者id，即entityUserId。这个需要查了，因为添加评论可能是给帖子添加，也可能是给别人的评论添加
        if (comment.getEntityType() == ENTITY_TYPE_POST){ //帖子
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());
            event.setEntityUserId(target.getUserId()) ;
        } else if (comment.getEntityType() == ENTITY_TYPE_COMMENT){ //回复
            Comment target = commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(target.getUserId()) ;
        }
        eventProducer.fireEvent(event);

        //触发发帖事件
        if (comment.getEntityType() == ENTITY_TYPE_POST){
            event = new Event()
                    .setTopic(TOPIC_PUBLISH)
                    .setUserId(comment.getUserId())
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(discussPostId) ;
            eventProducer.fireEvent(event);

            //计算帖子分数
            String redisKey = RedisKeyUtil.getPostScoreKey() ;
            redisTemplate.opsForSet().add(redisKey, discussPostId) ;
        }

        return "redirect:/discuss/detail/" + discussPostId ;//最终需要跳回到这个帖子的详情页面，因此重定向回去
    }
}
