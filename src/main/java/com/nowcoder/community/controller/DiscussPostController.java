package com.nowcoder.community.controller;

import com.nowcoder.community.entity.*;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConstant {

    @Autowired
    private DiscussPostService discussPostService ;

    @Autowired
    private HostHolder hostHolder ; //需要获取当前用户，因此注入HostHolder

    @Autowired
    private UserService userService ;

    @Autowired
    private CommentService commentService ;

    @Autowired
    private LikeService likeService ;

    @Autowired
    private EventProducer eventProducer ;

    @Autowired
    private RedisTemplate redisTemplate ;

    //对于添加帖子，浏览器只需要传入标题title和内容content，其余的内容都有
    @PostMapping(path = "/add")
    @ResponseBody
    public String addDiscussPost(String title, String content){
        User user = hostHolder.getUser(); //发帖的前提是需要登录
        if (user == null){
            return CommunityUtil.getJSONString(403, "你还没有登录哦！") ; //403表示没有权限
        }

        DiscussPost post = new DiscussPost() ;
        post.setUserId(user.getId());
        post.setTitle(title);
        post.setContent(content);
        post.setCreateTime(new Date());
        discussPostService.addDiscussPost(post) ; //type,status字段默认就是0，因此可以不用特意set

        //触发发帖事件
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(user.getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(post.getId()) ;
        eventProducer.fireEvent(event);

        //计算帖子分数
        String redisKey = RedisKeyUtil.getPostScoreKey() ;
        redisTemplate.opsForSet().add(redisKey, post.getId()) ;

        return CommunityUtil.getJSONString(0, "发布成功！") ;
    }

    //获取帖子的详细信息。需要返回的是模板，因此不要写@ResponseBody
    @GetMapping(path = "/detail/{discussPostId}")
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model, Page page){
        //帖子
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
        model.addAttribute("post", post) ;

        //作者
        //帖子post中的用户的id需要进行处理，页面上需要显示的是用户名称+头像，而不是用户id
        User user = userService.findUserById(post.getUserId());
        model.addAttribute("user", user) ;

        //帖子点赞数量
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPostId) ;
        model.addAttribute("likeCount", likeCount) ;
        //帖子点赞状态。
        int likeStatus = hostHolder.getUser() == null ? 0 : //没登陆也应该是可以看到点赞状态的，因此获取当前用户时候需要判断下
                likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_POST, discussPostId) ;
        model.addAttribute("likeStatus", likeStatus) ;

        //帖子回复部分
        //设置评论的分页信息
        page.setLimit(5); //每页展示5条评论
        page.setPath("/discuss/detail/" + discussPostId);
        page.setRows(post.getCommentCount());

        //评论：给帖子的评论
        //回复：给评论的评论
        List<Comment> commentList = commentService.findCommentByEntity(
                ENTITY_TYPE_POST, post.getId(), page.getOffset(), page.getLimit());
        //Comment数据中有user_id和target_id，需要转化成对应的user
        //构造VO列表(VO:Visual Object，具体返回给前端的数据，也是具体显示到页面上的数据)
        List<Map<String, Object>> commentVoList = new ArrayList<>(); //将显示的内容存到此list中
        if (commentList != null){
            for (Comment comment : commentList) {
                Map<String, Object> commentVo = new HashMap<>() ; //一个comment中待显示的对象都存到commentVo中
                //评论
                commentVo.put("comment", comment) ; //评论
                commentVo.put("user", userService.findUserById(comment.getUserId())) ; //评论的用户

                //评论点赞数量
                likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId()) ;
                commentVo.put("likeCount", likeCount) ;
                //评论点赞状态。
                likeStatus = hostHolder.getUser() == null ? 0 :
                        likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, comment.getId()) ;

                commentVo.put("likeStatus", likeStatus) ;

                //回复列表
                List<Comment> replyList = commentService.findCommentByEntity(
                        ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE);//回复就不做分页了
                //回复VO列表
                List<Map<String, Object>> replyVoList = new ArrayList<>(); //每个帖子所有的回复
                if (replyList != null){
                    for (Comment reply : replyList) {
                        Map<String, Object> replyVo = new HashMap<>() ;
                        replyVo.put("reply", reply) ; //回复
                        replyVo.put("user", userService.findUserById(reply.getUserId())) ; //回复作者
                        //回复目标（target_id）
                        User target = reply.getTargetId() == 0 ? null : userService.findUserById(reply.getTargetId()) ;
                        replyVo.put("target", target) ;
                        //回复点赞数量
                        likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId()) ;
                        replyVo.put("likeCount", likeCount) ;
                        //回复点赞状态。
                        likeStatus = hostHolder.getUser() == null ? 0 :
                                likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, reply.getId()) ;

                        replyVo.put("likeStatus", likeStatus) ;
                        replyVoList.add(replyVo) ;
                    }
                }
                commentVo.put("replys", replyVoList) ;
                //回复数量
                int replyCount = commentService.findCommentCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("replyCount", replyCount) ;

                commentVoList.add(commentVo) ;
            }
        }

        model.addAttribute("comments", commentVoList) ;
        return "/site/discuss-detail" ;
    }

    //置顶
    @PostMapping (path = "/top")
    @ResponseBody //异步请求
    public String setTop(int id){
        discussPostService.updateType(id, 1) ;

        //帖子发生了变化，因此需要把帖子的数据同步到es中
        //触发发帖事件
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id) ;
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0) ;
    }

    //加精
    @PostMapping (path = "/wonderful")
    @ResponseBody //异步请求
    public String setWonderful(int id){
        discussPostService.updateStatus(id, 1) ;

        //帖子发生了变化，因此需要把帖子的数据同步到es中
        //触发发帖事件
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id) ;
        eventProducer.fireEvent(event);

        //计算帖子分数
        String redisKey = RedisKeyUtil.getPostScoreKey() ;
        redisTemplate.opsForSet().add(redisKey, id) ;

        return CommunityUtil.getJSONString(0) ;
    }

    //删除
    @PostMapping (path = "/delete")
    @ResponseBody //异步请求
    public String setDelete(int id){
        discussPostService.updateStatus(id, 2) ;

        //帖子发生了变化，因此需要把帖子的数据同步到es中
        //触发删帖事件
        Event event = new Event()
                .setTopic(TOPIC_DELETE)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id) ;
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0) ;
    }
}
