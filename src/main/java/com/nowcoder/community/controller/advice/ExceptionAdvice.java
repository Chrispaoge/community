package com.nowcoder.community.controller.advice;

import com.nowcoder.community.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@ControllerAdvice(annotations = Controller.class) //只去扫描带有Controller的bean
public class ExceptionAdvice {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionAdvice.class) ;

    /**
     * 处理Controller中所有的异常，这里只记录到日志里。参数可以有很多（手册中查），但一般用的就是下面3个
     * @param e
     * @param request
     * @param response
     */
    @ExceptionHandler({Exception.class})//表示下面的方法是处理所有异常的方法，这里设置是处理所有的异常
    public void handleException(Exception e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.error("服务器发生异常" + e.getMessage()); //异常的概括
        for (StackTraceElement element : e.getStackTrace()) { //每个element记录的是一条异常的信息
            logger.error(element.toString());
        }

        //重定向到错误页面。浏览器访问服务器，可能是普通的请求，希望返回的是网页，那么可以返回500的错误页面。
        //但是浏览器访问服务器也有可能是异步的请求，希望返回的是JSON，这时候就不能重定向到500的页面了
        //因此需要判断请求是普通请求还是异步请求，判断的方式是固定的，需要记住
        String xRequestedWith = request.getHeader("x-requested-with");//拿到请求的方式
        if ("XMLHttpRequest".equals(xRequestedWith)) {//异步请求（只有异步请求才要求返回XML）
            response.setContentType("application/plain;charset=utf-8");
            PrintWriter writer = response.getWriter();
            writer.write(CommunityUtil.getJSONString(1, "服务器异常"));
        } else { //普通请求，重定向到错误页面
            response.sendRedirect(request.getContextPath() + "/error");
        }
    }
}
