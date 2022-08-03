package com.nowcoder.community.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.service.ElasticsearchService;
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
public class SearchController implements CommunityConstant {

    @Autowired
    private ElasticsearchService elasticsearchService ;

    @Autowired
    private UserService userService ;

    @Autowired
    private LikeService likeService ;

    //get请求，keyword通过url传，search?keyword=xxx
    @GetMapping(path = "/search")
    public String search(@RequestParam(value = "keyword", required = true) String keyword, Page page, Model model){
        // 搜索帖子
        Map<String, Object> resultMap = elasticsearchService.searchDiscussPost(keyword, page.getOffset(), page.getLimit());
        // 聚合数据
        List<Map<String,Object>> discussPosts= new ArrayList<>();
        List<DiscussPost> result = (List<DiscussPost>) resultMap.get("posts");
        if (result != null){
            for (DiscussPost discussPost : result) {
                Map<String, Object> map = new HashMap<>() ;
                //帖子
                map.put("post", discussPost) ;
                //作者
                map.put("user", userService.findUserById(discussPost.getUserId())) ;
                //点赞数量
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPost.getId())) ;
                discussPosts.add(map) ;
            }
        }
        model.addAttribute("discussPosts", discussPosts) ;
        model.addAttribute("keyword", keyword) ;
        page.setPath("/search?keyword=" + keyword);
        long rows = (long) resultMap.get("rows");
        page.setRows(result==null?0: (int) rows);
        return "site/search";
    }
}
