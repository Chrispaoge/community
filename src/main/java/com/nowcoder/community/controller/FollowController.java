package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class FollowController implements CommunityConstant {

    @Autowired
    private FollowService followService ;

    @Autowired
    private HostHolder hostHolder ;

    @Autowired
    private UserService userService ;

    @Autowired
    private EventProducer eventProducer ;

    //关注和取关都是异步的，整个页面不刷新，因此需要加response注解
    //关注
    @PostMapping(path = "/follow")
    @ResponseBody
    public String follow(int entityType, int entityId){
        User user = hostHolder.getUser() ; //关注一定是当前用户关注
        followService.follow(user.getId(), entityType, entityId);

        //触发关注事件
        Event event = new Event()
                .setTopic(TOPIC_FOLLOW)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(entityType)
                .setEntityId(entityId)
                .setEntityUserId(entityId) ;//目前只能关注人，因此直接写entityId
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0, "已关注！") ;
    }

    //取关
    @PostMapping(path = "/unfollow")
    @ResponseBody
    public String unfollow(int entityType, int entityId){
        User user = hostHolder.getUser() ;
        followService.unfollow(user.getId(), entityType, entityId);
        return CommunityUtil.getJSONString(0, "已取消关注！") ;
    }

    //查询某个用户关注的人
    @GetMapping(path = "/followees/{userId}")
    public String getFollowees(@PathVariable("userId") int userId, Page page, Model model){
        User user = userService.findUserById(userId);
        if (user == null){
            throw new RuntimeException("该用户不存在!") ;
        }
        model.addAttribute("user", user) ;

        page.setLimit(5);
        page.setPath("/followees/" + userId) ;
        page.setRows((int) followService.findFolloweeCount(userId, ENTITY_TYPE_USER));

        List<Map<String, Object>> userList = followService.findFollowee(userId, page.getOffset(), page.getLimit());
        if (userList != null){
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");
                map.put("hasFollowed", hasFollowed(u.getId())) ;//判断当前用户对这个用户u关注的状态
            }
        }
        model.addAttribute("users", userList) ;
        return "/site/followee" ;
    }

    //查询某个用户的粉丝
    @GetMapping(path = "/followers/{userId}")
    public String getFollowers(@PathVariable("userId") int userId, Page page, Model model){
        User user = userService.findUserById(userId);
        if (user == null){
            throw new RuntimeException("该用户不存在!") ;
        }
        model.addAttribute("user", user) ;

        page.setLimit(5);
        page.setPath("/followers/" + userId) ;
        page.setRows((int) followService.findFollowerCount(ENTITY_TYPE_USER, userId));

        List<Map<String, Object>> userList = followService.findFollower(userId, page.getOffset(), page.getLimit());
        if (userList != null){
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");
                map.put("hasFollowed", hasFollowed(u.getId())) ;//判断当前用户对这个用户u关注的状态
            }
        }
        model.addAttribute("users", userList) ;
        return "/site/follower" ;
    }

    //查询当前用户是否关注了userId用户
    private boolean hasFollowed(int userId){
        if (hostHolder.getUser() == null){
            return false ; //都没登录，认为没有关注userId用户
        }
        return followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId) ;
    }
}
