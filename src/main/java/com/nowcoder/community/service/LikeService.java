package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class LikeService {

    @Autowired
    private RedisTemplate redisTemplate ;

    /**
     * 点赞
     * @param userId：点赞的人
     * @param entityType：实体类型
     * @param entityId：实体id
     * @param entityUserId:实体的作者
     */
    public void like(int userId, int entityType, int entityId, int entityUserId){
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId) ;
                String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId) ;
                //判断当前用户点没点过赞。这是一个查询操作，必须放到事务之外
                boolean isMember = operations.opsForSet().isMember(entityLikeKey, userId) ;

                //开启事务
                operations.multi();
                if (isMember){ //当前用户点过赞
                    operations.opsForSet().remove(entityLikeKey, userId) ; //对这个实体的赞取消
                    operations.opsForValue().decrement(userLikeKey) ; //实体作者拥有的赞减1
                }else {//当前用户没点过赞
                    operations.opsForSet().add(entityLikeKey, userId) ;
                    operations.opsForValue().increment(userLikeKey) ; //实体作者拥有的赞加1
                }

                return operations.exec();
            }
        }) ;
    }

    // 查询某实体点赞的数量
    public long findEntityLikeCount(int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().size(entityLikeKey);
    }

    //查询某人对某实体的点赞状态。boolean只能表示两种状态，点赞和没点赞。后续业务还可以拓展，例如踩。因此返回整数，可以表示多种状态
    public int findEntityLikeStatus(int userId, int entityType, int entityId){
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().isMember(entityLikeKey, userId) ? 1 : 0 ; //1:点了赞；0：没点赞
    }

    // 查询某个用户获得的赞
    public int findUserLikeCount(int userId) {
        String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);
        Integer count = (Integer) redisTemplate.opsForValue().get(userLikeKey);
        return count == null ? 0 : count.intValue();
    }
}
