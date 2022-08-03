package com.nowcoder.community.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController implements CommunityConstant {

    @Autowired
    private DiscussPostService discussPostService ;

    @Autowired
    private UserService userService ;

    @Autowired
    private LikeService likeService ;

    //方法调用前，SpringMVC中的dispatcherServlet会自动实例化model，page这些参数，并将page注入到model中
    //因此，在thymeleaf中可以直接访问Page对象中的数据，就不要再model.addAttribute(xxxx..)
    @GetMapping(path = "/index")
    public String getIndexPage(Model model, Page page,
                               @RequestParam(name = "orderMode", defaultValue = "0") int orderMode){

        page.setRows(discussPostService.findDiscussPostRows(0)); //0:表示查询所有的帖子
        page.setPath("/index?orderMode=" + orderMode);

        List<DiscussPost> list = discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit(), orderMode);
        List<Map<String, Object>> discussPosts = new ArrayList<>() ;

        if (list != null){
            for (DiscussPost post : list) {
                Map<String, Object> map = new HashMap<>() ;
                map.put("post", post) ;
                User user = userService.findUserById(post.getUserId());
                map.put("user", user) ;
                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()) ; //帖子赞的数量
                map.put("likeCount", likeCount) ;
                discussPosts.add(map) ;
            }
        }
        //将返回给页面的结果封装到model中
        model.addAttribute("discussPosts", discussPosts) ;
        model.addAttribute("orderMode", orderMode) ;
        return "/index" ;
    }

    @GetMapping(path = "/error")
    public String getErrorPage(){
        return "/error/500" ;
    }

    @GetMapping(path = "/denied")
    public String getDeniedPage(){
        return "/error/404" ;
    }
}
