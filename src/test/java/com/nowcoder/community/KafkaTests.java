package com.nowcoder.community;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = Application.class)
public class KafkaTests {

    @Autowired
    private KafkaProducer kafkaProducer ;

    @Test
    public void testKafka(){
        //生产者发消息是主动的，消费者处理消息是被动的（队列有消息就处理，没有就阻塞监听）
        kafkaProducer.sendMessage("test", "你好");
        kafkaProducer.sendMessage("test", "在吗");

        try {
            Thread.sleep(200 * 10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

@Component
class KafkaProducer{

    @Autowired
    private KafkaTemplate kafkaTemplate ; //生产者发消息主要用它

    //提供一个方法供外界调用，调用就发消息
    public void sendMessage(String topic, String content){
        kafkaTemplate.send(topic, content) ;
    }
}

@Component
class KafkaConsumer{
    //消费者不需要依赖kafkaTemplate，它是自动的处理消息
    @KafkaListener(topics = {"test"}) //一旦服务启动，Spring就会自动监听名为test的这个主题，一旦监听到了，就交给注解下的方法处理
    public void handleMessage(ConsumerRecord record){
        System.out.println(record.value()); //这里将读取的消息打印出来
    }
}
