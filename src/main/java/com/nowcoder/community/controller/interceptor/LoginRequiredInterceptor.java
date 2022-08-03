package com.nowcoder.community.controller.interceptor;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {

    @Autowired
    private HostHolder hostHolder ;

    //显然应该在请求之前就得判断，因此重写preHandle方法。
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //handler是拦截的目标，首先判断拦截的目标是不是一个方法
        if (handler instanceof HandlerMethod){
            HandlerMethod handlerMethod = (HandlerMethod)handler ;
            //获取拦截的method对象
            Method method = handlerMethod.getMethod();
            //取出这个方法上的LoginRequired注解
            LoginRequired loginRequired = method.getAnnotation(LoginRequired.class);
            if (loginRequired != null && hostHolder.getUser() == null){ //用户没登录
                //此方法是接口声明的，不是在Controller中的，因此重定向需要通过response来完成，而不是直接return放回一个模板
                response.sendRedirect(request.getContextPath() + "/login"); //重定向到登录页面
                return false ;
            }
        }
        return true ;
    }
}
