package com.nowcoder.community.controller.interceptor;

import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CookieUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Component
public class LoginTicketInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 从cookie中获取凭证
        String ticket = CookieUtil.getValue(request, "ticket");

        if (ticket != null) {
            // 查询凭证(通过获取到的ticket从数据库中查)
            LoginTicket loginTicket = userService.findLoginTicket(ticket);
            // 检查凭证是否有效
            if (loginTicket != null && loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())) {
                // ticket有效
                // 根据ticket查询用户
                User user = userService.findUserById(loginTicket.getUserId());
                // 在本次请求中持有用户。后面很多的地方会用到这个user，因此需要暂存此user：ThreadLocal
                // 请求没有处理完，那么线程就一直还在，请求处理完，服务器做出了响应之后，线程销毁
                hostHolder.setUser(user);

                /**
                构建用户认证的结果，并存入SecurityContext，以便于Security进行授权
                但是这里代码有问题，认证需要在授权之前，下面的认证逻辑应该是写在Filter中的，而不是拦截器中，若是认证的逻辑写在了
                拦截器中，那么认证就在授权之后了（授权是security自动在filter中帮我们做的，我们只是进行配置）

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        user, user.getPassword(), userService.getAuthorities(user.getId())) ;
                SecurityContextHolder.setContext(new SecurityContextImpl(authentication));

                 */
            }
        }

        return true;
    }


    //视图渲染之前
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if (user != null && modelAndView != null) {
            modelAndView.addObject("loginUser", user);
        }
    }

    //视图渲染之后
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        hostHolder.clear();
        SecurityContextHolder.clearContext();
    }
}

