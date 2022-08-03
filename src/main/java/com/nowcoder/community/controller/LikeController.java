package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class LikeController implements CommunityConstant {

    @Autowired
    private LikeService likeService ;

    @Autowired
    private HostHolder hostHolder ; //当前用户点赞，因此需要获取当前用户

    @Autowired
    private EventProducer eventProducer ;

    @Autowired
    private RedisTemplate redisTemplate ;

    @PostMapping(path = "/like")
    @ResponseBody
    public String like(int entityType, int entityId, int entityUserId, int postId){
        User user = hostHolder.getUser() ;
        //这里暂时不判断user是否为null(即判断用户有没有登录)，后续会用SpringSecurity重构拦截器，统一管理权限问题
        //当然这里用拦截器也是可以的，进行配置，不登录就不让点赞

        //点赞
        likeService.like(user.getId(), entityType, entityId, entityUserId);
        //点赞数量
        long likeCount = likeService.findEntityLikeCount(entityType, entityId);
        //点赞状态
        int likeStatus = likeService.findEntityLikeStatus(user.getId(), entityType, entityId);

        //将上面的两个值传给页面，使用map封装一下传
        Map<String, Object> map = new HashMap<>();
        map.put("likeCount", likeCount);
        map.put("likeStatus", likeStatus);

        //触发点赞事件
        if (likeStatus == 1){ //点赞才发通知，取消赞不发通知
            Event event = new Event()
                    .setTopic(TOPIC_LIKE)
                    .setUserId(hostHolder.getUser().getId())
                    .setEntityType(entityType)
                    .setEntityId(entityId)
                    .setEntityUserId(entityUserId)
                    .setData("postId", postId) ; //帖子的id。无论是给帖子点赞还是给评论点赞，都是在帖子里的
            eventProducer.fireEvent(event);
        }

        if (entityType == ENTITY_TYPE_POST){
            String redisKey = RedisKeyUtil.getPostScoreKey() ;
            redisTemplate.opsForSet().add(redisKey, postId) ;
        }

        return CommunityUtil.getJSONString(0, null, map);
    }
}
