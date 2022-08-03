package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.SimpleFormatter;

@Service
public class DataService {

    @Autowired
    private RedisTemplate redisTemplate ;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd") ;

    //将指定的IP计入UV
    public void recordUV(String ip){
        String redisKey = RedisKeyUtil.getUVKey(dateFormat.format(new Date())) ;
        redisTemplate.opsForHyperLogLog().add(redisKey, ip) ;
    }

    // 统计指定日期范围内的UV，并将其存到redis中。UV是独立访客，HyperLogLog做的是唯一统计
    public long calculateUV(Date start, Date end){
        if (start == null || end == null){
            throw new IllegalArgumentException("参数不能为空！") ;
        }
        // 整理该日期范围内的key
        List<String> keyList = new ArrayList<>() ;
        Calendar calendar = Calendar.getInstance() ;//需要遍历拿到每一天key，因此需要Calendar来遍历日期
        calendar.setTime(start);
        while (!calendar.getTime().after(end)){//就start小于end时
            String key = RedisKeyUtil.getUVKey(dateFormat.format(calendar.getTime())) ;
            keyList.add(key) ;
            calendar.add(Calendar.DATE, 1); //日期+1
        }

        //合并这些数
        String redisKey = RedisKeyUtil.getUVKey(dateFormat.format(start), dateFormat.format(end)) ;
        redisTemplate.opsForHyperLogLog().union(redisKey, keyList.toArray()) ;

        //返回统计的结果
        return redisTemplate.opsForHyperLogLog().size(redisKey) ;
    }

    // 将指定用户计入DAU
    public void recordDAU(int userId){
        String redisKey = RedisKeyUtil.getDAUKey(dateFormat.format(new Date())) ;
        redisTemplate.opsForValue().setBit(redisKey, userId, true) ; //将userId这个位置上的bit置为1
    }

    // 统计指定日期范围内的DAU(日活跃用户)
    public long calculateDAU(Date start, Date end){
        if (start == null || end == null){
            throw new IllegalArgumentException("参数不能为空！") ;
        }
        // 整理该日期范围内的key
        List<byte[]> keyList = new ArrayList<>() ; //存该日期范围内的key
        Calendar calendar = Calendar.getInstance() ;//需要遍历拿到每一天key，因此需要Calendar来遍历日期
        calendar.setTime(start);
        while (!calendar.getTime().after(end)){//就start小于end时
            String key = RedisKeyUtil.getDAUKey(dateFormat.format(calendar.getTime()));//每次遍历到拿到一个key
            keyList.add(key.getBytes()) ;
            calendar.add(Calendar.DATE, 1); //日期+1
        }
        // 进行OR运算
        long res = (long) redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                String redisKey = RedisKeyUtil.getDAUKey(dateFormat.format(start), dateFormat.format(end));
                connection.bitOp(RedisStringCommands.BitOperation.OR,
                        redisKey.getBytes(), keyList.toArray(new byte[0][0])) ;
                return connection.bitCount(redisKey.getBytes()) ;
            }
        });
        return res;
    }
}
