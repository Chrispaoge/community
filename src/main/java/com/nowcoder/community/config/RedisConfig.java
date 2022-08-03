package com.nowcoder.community.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {

    /**
     * redisTemplate方法要想具备连接数据库的能力，需要能创建数据库连接，而连接是由连接工厂创建的，因此参数需要注入连接工厂
     * @return
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory){
        RedisTemplate<String, Object> template = new RedisTemplate<>() ;
        template.setConnectionFactory(factory); //有了连接工厂后，就具备了访问数据库的能力

        //Java类型的数据要写入redis数据库中需要序列化
        //设置key序列化方式
        template.setKeySerializer(RedisSerializer.string());
        //设置value的序列化方式
        template.setValueSerializer(RedisSerializer.json());
        //设置hash的key的序列化方式
        template.setHashKeySerializer(RedisSerializer.string());
        //这只hash的value序列化方式
        template.setHashValueSerializer(RedisSerializer.json());

        template.afterPropertiesSet(); //使设置的参数生效
        return template ;
    }
}
