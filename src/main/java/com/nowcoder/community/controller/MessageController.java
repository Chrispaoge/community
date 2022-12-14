package com.nowcoder.community.controller;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageService;
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
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@Controller
public class MessageController implements CommunityConstant {

    @Autowired
    private MessageService messageService ;

    @Autowired
    private HostHolder hostHolder ; //需要查当前用户的私信，需要获取当前用户

    @Autowired
    private UserService userService ;

    //私信列表
    @GetMapping(path = "/letter/list")
    public String getLetterList(Model model, Page page){ //需要传数据，因此有model；支持分页，因此传page
        User user = hostHolder.getUser() ;
        //分页信息
        page.setLimit(5);
        page.setPath("/letter/list");
        page.setRows(messageService.findConversationCount(user.getId()));
        //查询会话列表
        List<Message> conversationList = messageService.findConversations(
                user.getId(), page.getOffset(), page.getLimit());
        //得到会话列表还不够，还需要显示会话的未读消息，当前用户总的未读消息数量，每次会话共包含几条私信
        //按常规方法处理，将每一个会话要返回的数据都存到一个map中
        List<Map<String, Object>> conversations = new ArrayList<>() ;
        if (conversationList != null){
            for (Message message : conversationList) {
                Map<String, Object> map = new HashMap<>() ;
                map.put("conversation", message) ;
                map.put("letterCount", messageService.findLetterCount(message.getConversationId())) ;
                map.put("unreadCount", messageService.findLetterUnreadCount(
                        user.getId(), message.getConversationId())) ;//每个会话中包含的未读的消息数量
                //还需显示用户头像（显示对话的另一个人的id，而不是当前用户的头像）
                int targetId = user.getId() == message.getFromId() ? message.getToId() : message.getFromId();
                map.put("target", userService.findUserById(targetId)) ;
                conversations.add(map) ;
            }
        }
        model.addAttribute("conversations", conversations) ;

        //查询用户的所有未读消息数量
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null) ;
        model.addAttribute("letterUnreadCount", letterUnreadCount) ;
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null) ;
        model.addAttribute("noticeUnreadCount", noticeUnreadCount) ;

        return "/site/Letter" ;
    }

    /**
     * 查看具体某个对话的私信详情
     * @param conversationId：会话id
     * @param page：分页
     * @param model：给页面传数据
     * @return：页面的地址
     */
    @GetMapping(path = "/letter/detail/{conversationId}")
    public String getLetterDetail(@PathVariable("conversationId") String conversationId, Page page, Model model){
        //分页信息
        page.setLimit(5);
        page.setPath("/letter/detail/" + conversationId);
        page.setRows(messageService.findLetterCount(conversationId));
        //私信列表
        List<Message> letterList = messageService.findLetters(conversationId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> letters = new ArrayList<>() ;
        if (letterList != null){
            for (Message message : letterList) {
                Map<String, Object> map = new HashMap<>() ;
                map.put("letter", message) ;
                map.put("fromUser", userService.findUserById(message.getFromId())) ; //给我发私信的用户
                letters.add(map) ;
            }
        }
        model.addAttribute("letters", letters);
        //私信目标
        model.addAttribute("target", getLetterTarget(conversationId)) ;

        //将这个对话私信详情中未读的消息提取出来
        List<Integer> ids = getLetterIds(letterList);
        if (!ids.isEmpty()){
            messageService.readMessage(ids) ; //将这些id对应的私信状态设置为已经，即set status=1
        }
        return "/site/letter-detail" ; //返回模板
    }

    //从私信列表中获取当前用户未读的消息id
    private List<Integer> getLetterIds(List<Message> letterList){
        List<Integer> ids = new ArrayList<>() ;
        if (letterList != null){
            for (Message message : letterList) {
                //不仅要看状态（未读），还需要检查当前的用户是不是接收者
                if (hostHolder.getUser().getId() == message.getToId() && message.getStatus() == 0){
                    ids.add(message.getId()) ;
                }
            }
        }
        return ids ;
    }

    //获取私信的用户（拿到给我发私信的那个user）
    private User getLetterTarget(String conversationId){
        String[] ids = conversationId.split("_") ;
        int id0 = Integer.parseInt(ids[0]) ;
        int id1 = Integer.parseInt(ids[1]) ;

        if (hostHolder.getUser().getId() == id0){
            return userService.findUserById(id1) ;
        } else {
            return userService.findUserById(id0) ;
        }
    }

    /**
     * 页面上需要传两个数据过来，分别是私信接收者的用户名和私信的内容
     * 发私信肯定是当前用户发给某一个用户的
     */
    @PostMapping(path = "/letter/send")
    @ResponseBody
    public String sendLetter(String toName, String content){
        User target = userService.findUserByName(toName) ;
        if (target == null){
            return CommunityUtil.getJSONString(1, "目标用户不存在!") ;
        }

        Message message = new Message() ;
        message.setFromId(hostHolder.getUser().getId());
        message.setToId(target.getId());
        //拼接conversationId的时候，记得小的在前，大的在后
        if (message.getFromId() < message.getToId()){
            message.setConversationId(message.getFromId() + "_" + message.getToId());
        } else {
            message.setConversationId(message.getToId() + "_" + message.getFromId());
        }
        message.setContent(content);
        message.setCreateTime(new Date());
        messageService.addMessage(message) ;

        return CommunityUtil.getJSONString(0) ;
    }

    @GetMapping(path = "/notice/list")
    public String getNoticeList(Model model){
        User user = hostHolder.getUser();

        //查询评论通知
        Message message = messageService.findLatestNotice(user.getId(), TOPIC_COMMENT);
        if (message != null){
            Map<String, Object> messageVO = new HashMap<>() ;
            messageVO.put("message", message) ;
            String content = HtmlUtils.htmlUnescape(message.getContent());
            HashMap<String, Object> data = JSONObject.parseObject(content, HashMap.class);
            messageVO.put("user", userService.findUserById((Integer) data.get("userId"))) ;
            messageVO.put("entityType", data.get("entityType")) ;
            messageVO.put("entityId", data.get("entityId")) ;
            messageVO.put("postId", data.get("postId")) ;
            //这一类通知总的数量和未读的数量
            int count = messageService.findNoticeCount(user.getId(), TOPIC_COMMENT);
            messageVO.put("count", count) ;
            int unread = messageService.findNoticeUnreadCount(user.getId(), TOPIC_COMMENT);
            messageVO.put("unread", unread) ;
            model.addAttribute("commentNotice", messageVO) ;
        }

        //查询点赞通知
        message = messageService.findLatestNotice(user.getId(), TOPIC_LIKE);
        if (message != null){
            Map<String, Object> messageVO = new HashMap<>() ;
            messageVO.put("message", message) ;
            String content = HtmlUtils.htmlUnescape(message.getContent());
            HashMap<String, Object> data = JSONObject.parseObject(content, HashMap.class);
            messageVO.put("user", userService.findUserById((Integer) data.get("userId"))) ;
            messageVO.put("entityType", data.get("entityType")) ;
            messageVO.put("entityId", data.get("entityId")) ;
            messageVO.put("postId", data.get("postId")) ;

            int count = messageService.findNoticeCount(user.getId(), TOPIC_LIKE);
            messageVO.put("count", count) ;
            int unread = messageService.findNoticeUnreadCount(user.getId(), TOPIC_LIKE);
            messageVO.put("unread", unread) ;
            model.addAttribute("likeNotice", messageVO) ;
        }

        //查询关注通知
        message = messageService.findLatestNotice(user.getId(), TOPIC_FOLLOW);
        if (message != null){
            Map<String, Object> messageVO = new HashMap<>() ;
            messageVO.put("message", message) ;
            String content = HtmlUtils.htmlUnescape(message.getContent());
            HashMap<String, Object> data = JSONObject.parseObject(content, HashMap.class);
            messageVO.put("user", userService.findUserById((Integer) data.get("userId"))) ;
            messageVO.put("entityType", data.get("entityType")) ;
            messageVO.put("entityId", data.get("entityId")) ;

            int count = messageService.findNoticeCount(user.getId(), TOPIC_FOLLOW);
            messageVO.put("count", count) ;
            int unread = messageService.findNoticeUnreadCount(user.getId(), TOPIC_FOLLOW);
            messageVO.put("unread", unread) ;
            model.addAttribute("followNotice", messageVO) ;
        }

        //查询总的未读数量和私信数量
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null) ;
        model.addAttribute("letterUnreadCount", letterUnreadCount) ;
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null) ;
        model.addAttribute("noticeUnreadCount", noticeUnreadCount) ;

        return "/site/notice" ;
    }

    @GetMapping(path = "/notice/detail/{topic}")
    public String getNoticeDetail(@PathVariable("topic") String topic, Page page, Model model){
        User user = hostHolder.getUser();
        page.setLimit(5);
        page.setPath("/notice/detail/" + topic);
        page.setRows(messageService.findNoticeCount(user.getId(), topic));

        List<Message> noticeList = messageService.findNotices(user.getId(), topic, page.getOffset(), page.getLimit());
        List<Map<String, Object>> noticeVoList = new ArrayList<>() ;
        if (noticeList != null){
            for (Message notice : noticeList) {
                Map<String, Object> map = new HashMap<>() ;
                // 通知
                map.put("notice", notice) ;
                // 内容
                String content = HtmlUtils.htmlUnescape(notice.getContent()) ;
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class) ;
                map.put("user", userService.findUserById((Integer) data.get("userId"))) ;
                map.put("entityType", data.get("entityType")) ;
                map.put("entityId", data.get("entityId")) ;
                map.put("postId", data.get("postId")) ;
                // 通知的作者
                map.put("fromUser", userService.findUserById(notice.getFromId())) ;

                noticeVoList.add(map) ;
            }
        }
        model.addAttribute("notices", noticeVoList) ;

        //设置已读
        List<Integer> ids = getLetterIds(noticeList) ;
        if (!ids.isEmpty()){ //非空，即有未读的消息，那么将这些消息的状态设置为已读（点进去了就是已读了）
            messageService.readMessage(ids) ;
        }

        return "/site/notice-detail" ;
    }

}
