package com.nowcoder.community.service;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowService implements CommunityConstant {

    @Autowired
    private RedisTemplate redisTemplate ;

    @Autowired
    private UserService userService ;

    //关注
    //存的时候需要存两份数据：1.用户关注的实体目标；2.此实体目标的粉丝（多了这个用户），因此需要保证事务
    public void follow(int userId, int entityType, int entityId){
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType) ;
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId) ;

                //启动事务
                operations.multi();
                //1.存用户关注的实体目标
                operations.opsForZSet().add(followeeKey, entityId, System.currentTimeMillis()) ;
                //2.此实体目标的粉丝。下面的userId就是当前用户的id，即表示此实体多了当前用户这个粉丝
                operations.opsForZSet().add(followerKey, userId, System.currentTimeMillis()) ;
                return operations.exec(); //提交事务
            }
        }) ;
    }

    //取关
    //和上面的逻辑类似，就是将保存改为删除
    public void unfollow(int userId, int entityType, int entityId){
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType) ;
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId) ;

                //启动事务
                operations.multi();
                operations.opsForZSet().remove(followeeKey, entityId) ;
                operations.opsForZSet().remove(followerKey, userId) ;
                return operations.exec(); //提交事务
            }
        }) ;
    }

    //查询某个用户关注的实体的数量（如ycj关注了几个人，需要分别查出来）
    public long findFolloweeCount(int userId, int entityType){
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType) ;
        return redisTemplate.opsForZSet().zCard(followeeKey) ;
    }

    //查询某个实体的粉丝数量
    public long findFollowerCount(int entityType, int entityId){
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId) ;
        return redisTemplate.opsForZSet().zCard(followerKey) ;
    }

    //查询当前用户是否已关注该实体
    public boolean hasFollowed(int userId, int entityType, int entityId){
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType) ; //获取该实体key
        return redisTemplate.opsForZSet().score(followeeKey, entityId) != null ;
    }

    //查询某个用户关注的人
    //传给页面的需要有user和关注的时间，因此用集合存
    public List<Map<String, Object>> findFollowee(int userId, int offset, int limit){
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, ENTITY_TYPE_USER) ; //userId关注的人
        // offset起始索引；offset + limit - 1：截止索引。
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followeeKey, offset, offset + limit - 1);
        if (targetIds == null){
            return null ;
        }

        List<Map<String, Object>> list = new ArrayList<>() ;
        for (Integer targetId : targetIds) {
            Map<String, Object> map = new HashMap<>() ;
            User user = userService.findUserById(targetId);
            map.put("user", user) ;
            Double score = redisTemplate.opsForZSet().score(followeeKey, targetId);//关注的时间即zset中的score，取出即可
            map.put("followTime", new Date(score.longValue())) ;
            list.add(map) ;
        }

        return list ;
    }

    //查询某用户的粉丝。和上面的逻辑一样
    public List<Map<String, Object>> findFollower(int userId, int offset, int limit){
        String followerKey = RedisKeyUtil.getFollowerKey(ENTITY_TYPE_USER, userId) ; //userId的粉丝
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followerKey, offset, offset + limit - 1);
        if (targetIds == null){
            return null ;
        }

        List<Map<String, Object>> list = new ArrayList<>() ;
        for (Integer targetId : targetIds) {
            Map<String, Object> map = new HashMap<>() ;
            User user = userService.findUserById(targetId);
            map.put("user", user) ;
            Double score = redisTemplate.opsForZSet().score(followerKey, targetId);
            map.put("followTime", new Date(score.longValue())) ;
            list.add(map) ;
        }

        return list ;
    }
}
