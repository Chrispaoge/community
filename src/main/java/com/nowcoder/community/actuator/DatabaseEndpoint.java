package com.nowcoder.community.actuator;

import com.nowcoder.community.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

//检查数据库当前的连接是否正常：尝试获取一个数据库连接
@Component
@Endpoint(id = "database")
public class DatabaseEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseEndpoint.class) ;

    @Autowired
    private DataSource dataSource ;

    @ReadOperation //通过get请求可以访问加了@ReadOperation注解的方法
    public String checkConnection(){
        try (Connection connection = dataSource.getConnection() ;
        ){
            return CommunityUtil.getJSONString(0, "获取连接成功！") ;
        } catch (SQLException e) {
            logger.error("获取连接失败：" + e.getMessage());
            return CommunityUtil.getJSONString(0, "获取连接失败！") ;
        }
    }
}
