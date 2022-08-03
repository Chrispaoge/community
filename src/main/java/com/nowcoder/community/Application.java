package com.nowcoder.community;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class Application {

//    @PostConstruct
//    public void init(){
//        // 解决netty启动冲突问题（redis的底层也是netty）
//        // see Netty4Utils.setAvailableProcessors()
//        System.setProperty("es")
//    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args) ;
    }
}
