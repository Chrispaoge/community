package com.nowcoder.community.entity;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class Event {

    private String topic ; //事件的类型。从Kafka的角度为主题
    private int userId ; //触发事件的人。例如A给B点赞，那么userId就是A
    private int entityType ;//触发事件的人做了什么操作，如点赞。那么点赞就是entityType
    private int entityId ;
    private int entityUserId ; //实体的作者。如帖子的作者是谁，例如A给B点赞，那么entityUserId就是B
    private Map<String, Object> data = new HashMap<>() ; //为的是以后的拓展性。不知道拓展以后还有哪些字段，统一都放到map里

    // 对set方法简单改造下。
    public Event setTopic(String topic) {
        this.topic = topic;
        return this ;
    }

    public Event setUserId(int userId) {
        this.userId = userId;
        return this ;
    }

    public Event setEntityType(int entityType) {
        this.entityType = entityType;
        return this ;
    }

    public Event setEntityId(int entityId) {
        this.entityId = entityId;
        return this ;
    }

    public Event setEntityUserId(int entityUserId) {
        this.entityUserId = entityUserId;
        return this ;
    }

    public Event setData(String key, Object value) {
        this.data.put(key, value) ;
        return this ;
    }
}
