package com.nowcoder.community.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Component
public class MailClient {

    //需要记录日志，就得有对应的日志字段
    private static final Logger logger = LoggerFactory.getLogger(MailClient.class) ;

    @Autowired
    private JavaMailSender mailSender ;

    //发邮件需要几个条件：发件人邮箱，收件人邮箱，邮件的标题和内容
    //通过服务器发邮件，发件人是确定的，后面两个不确定
    @Value("${spring.mail.username}")
    private String from ;

    //发给谁，主题，内容
    public void sendMail(String to, String subject, String content){
        //使用JavaMailSender发邮件
        try {
            MimeMessage message = mailSender.createMimeMessage() ;
            MimeMessageHelper helper = new MimeMessageHelper(message) ;
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true); //支持html文本
            mailSender.send(helper.getMimeMessage());
        } catch (MessagingException e) {
            logger.error("发送邮件失败：" + e.getMessage());
        }
    }
}
