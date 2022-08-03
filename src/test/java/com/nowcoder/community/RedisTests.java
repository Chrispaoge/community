package com.nowcoder.community;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = Application.class)
public class RedisTests {

    @Autowired
    private RedisTemplate redisTemplate ; //哪需要访问redis，RedisTemplate

    @Test
    public void testStrings(){
        String redisKey = "test:count" ;
        redisTemplate.opsForValue().set(redisKey, 1);
        System.out.println(redisTemplate.opsForValue().get(redisKey));
        System.out.println(redisTemplate.opsForValue().increment(redisKey));
    }

    @Test
    public void testHashes(){
        String redisKey = "test:user" ;
        redisTemplate.opsForHash().put(redisKey, "id", 1) ;
        redisTemplate.opsForHash().put(redisKey, "username", "zhangsan");
        System.out.println(redisTemplate.opsForHash().get(redisKey, "id"));
        System.out.println(redisTemplate.opsForHash().get(redisKey, "username"));
    }

    @Test
    public void testLists(){
        String redisKey = "test:ids" ;
        redisTemplate.opsForList().leftPush(redisKey, 101) ;
        redisTemplate.opsForList().leftPush(redisKey, 102) ;
        redisTemplate.opsForList().leftPush(redisKey, 103) ;
        System.out.println(redisTemplate.opsForList().size(redisKey));
        System.out.println(redisTemplate.opsForList().index(redisKey, 0));
        System.out.println(redisTemplate.opsForList().range(redisKey, 0, 2));
    }

    //编程式事务：最关键的是下面的代码结构以及开启事务operations.multi()和提交事务operations.exec()两句代码
    @Test
    public void testTransactional(){
        Object obj = redisTemplate.execute(new SessionCallback() {
            @Override                           //operations执行命令的对象
            public Object execute(RedisOperations operations) throws DataAccessException {
                String redisKey = "test:tx" ;
                operations.multi(); //启动事务

                operations.opsForSet().add(redisKey, "zhangsan") ;
                operations.opsForSet().add(redisKey, "lisi") ;
                operations.opsForSet().add(redisKey, "wangwu") ;

                System.out.println(operations.opsForSet().members(redisKey)); //输出[]。事务之中，查询是没有作用的

                return operations.exec(); //operations.exec()提交事务
            }
        });
        System.out.println(obj); //[1, 1, 1, [lisi, wangwu, zhangsan]]
    }

    // 统计20w个重复数据的独立总数
    @Test
    public void testHyperLogLog(){
        String redisKey = "test:hll:01" ;
        for (int i = 1 ; i < 100000 ; i ++){
            redisTemplate.opsForHyperLogLog().add(redisKey, i) ;
        }
        for (int i = 1 ; i < 100000 ; i ++){
            int r = (int) (Math.random() * 10000 + 1);
            redisTemplate.opsForHyperLogLog().add(redisKey, r) ;
        }
        Long size = redisTemplate.opsForHyperLogLog().size(redisKey);
        System.out.println(size);
    }

    // 将3组数据合并，再统计合并后的重复数据的独立总数
    @Test
    public void testHyperLogLogUnion(){
        String redisKey2 = "test:hll:02" ;
        for (int i = 1 ; i <= 10000 ; i ++){
            redisTemplate.opsForHyperLogLog().add(redisKey2, i) ;
        }

        String redisKey3 = "test:hll:03" ;
        for (int i = 5001 ; i <= 15000 ; i ++){
            redisTemplate.opsForHyperLogLog().add(redisKey3, i) ;
        }

        String redisKey4 = "test:hll:04" ;
        for (int i = 10001 ; i <= 20000 ; i ++){
            redisTemplate.opsForHyperLogLog().add(redisKey4, i) ;
        }

        String unionKey = "test:hll:union" ;
        redisTemplate.opsForHyperLogLog().union(unionKey, redisKey2, redisKey3, redisKey4) ;

        Long size = redisTemplate.opsForHyperLogLog().size(unionKey); //19833
        System.out.println(size);
    }

    // 统计一组数据的布尔值
    @Test
    public void testBitMap(){
        String redisKey = "test:bm:01" ;
        //记录
        redisTemplate.opsForValue().setBit(redisKey, 1, true) ;
        redisTemplate.opsForValue().setBit(redisKey, 4, true) ;
        redisTemplate.opsForValue().setBit(redisKey, 7, true) ;
        //查询
        Boolean bit = redisTemplate.opsForValue().getBit(redisKey, 0);
        System.out.println(bit); //false
        bit = redisTemplate.opsForValue().getBit(redisKey, 1);
        System.out.println(bit); //true

        //统计
        //获取redis底层的连接才能访问，下面就有RedisConnection
        Object obj = redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                return connection.bitCount(redisKey.getBytes()) ;
            }
        }) ;
        System.out.println(obj); //3
    }

    // 统计3组数据的布尔值，并对这3组数据做or运算
    @Test
    public void testBitMapOperation(){
        String redisKey2 = "test:bm:02" ;
        redisTemplate.opsForValue().setBit(redisKey2, 0, true) ;
        redisTemplate.opsForValue().setBit(redisKey2, 1, true) ;
        redisTemplate.opsForValue().setBit(redisKey2, 2, true) ;

        String redisKey3 = "test:bm:03" ;
        redisTemplate.opsForValue().setBit(redisKey3, 2, true) ;
        redisTemplate.opsForValue().setBit(redisKey3, 3, true) ;
        redisTemplate.opsForValue().setBit(redisKey3, 4, true) ;

        String redisKey4 = "test:bm:04" ;
        redisTemplate.opsForValue().setBit(redisKey4, 4, true) ;
        redisTemplate.opsForValue().setBit(redisKey4, 5, true) ;
        redisTemplate.opsForValue().setBit(redisKey4, 6, true) ;

        String redisKey = "test:bm:or" ;

        Object obj = redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                connection.bitOp(RedisStringCommands.BitOperation.OR,
                        redisKey.getBytes(), redisKey2.getBytes(), redisKey3.getBytes(), redisKey4.getBytes()) ;
                return connection.bitCount(redisKey.getBytes());
            }
        }) ;

        System.out.println(obj); //7
    }
}