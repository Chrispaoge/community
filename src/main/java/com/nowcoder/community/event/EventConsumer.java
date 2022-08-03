package com.nowcoder.community.event;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.CommunityConstant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class EventConsumer implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class) ;

    @Autowired
    private MessageService messageService ;

    @Autowired
    private DiscussPostService discussPostService ;

    @Autowired
    private ElasticsearchService elasticsearchService ;

    //处理事件（本质上往message表里发送一条数据）
    //可以一个方法消费一个主题，也可以一个方法消费多个主题，一个主题可以被多个方法消费，这是一个多对多的关系
    //由于形式和逻辑很相近，因此这里写一个方法消费多个主题
    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_LIKE, TOPIC_FOLLOW})
    public void handleCommentMessage(ConsumerRecord record){
        if (record == null || record.value() == null){
            logger.error("消息的内容为空!");
            return ;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null){
            logger.error("消息格式错误！");
            return ;
        }

        //发送站内通知。即构造一条message数据插入到message表中
        // from_id固定为1，表示是系统发的消息。conversation_id改存为主题信息:评论，点赞，关注。内容存的不是一句话，而是一个JSON字符串
        // 为的是在页面上拼接出一句话。如用户xxx点赞了你的帖子，那么需要知道xxx的id，还需要知道帖子这个实体类型和id，因此弄个map
        Message message = new Message() ;
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());

        Map<String, Object> content = new HashMap<>() ;
        content.put("userId", event.getUserId()) ;
        content.put("entityType", event.getEntityType()) ;
        content.put("entityId", event.getEntityId()) ;
        //event中的map所有的内容都放到上面的map中，即最终都存到message的content字段里
        if (!event.getData().isEmpty()){
            for (Map.Entry<String, Object> entry : event.getData().entrySet()) {
                content.put(entry.getKey(), entry.getValue()) ;

            }
        }
        message.setContent(JSONObject.toJSONString(content));
        messageService.addMessage(message) ;
    }

    //消费发帖事件
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误！");
            return;
        }

        //从发帖事件中获取帖子的id，查到对应的帖子，然后存到es服务器中
        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId()) ;
        elasticsearchService.saveDiscussPost(post);
    }

    //消费删帖事件
    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误！");
            return;
        }
        elasticsearchService.deleteDiscussPost(event.getEntityId());
    }
}
