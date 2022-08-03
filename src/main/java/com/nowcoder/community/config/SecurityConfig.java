package com.nowcoder.community.config;

import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.CookieUtil;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter implements CommunityConstant {

    @Autowired
    private UserService userService ;


    //忽略对静态资源的拦截
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/resources/**") ; //所有resources下的静态资源都可以访问
    }

    //授权
    //主要就是看项目中所有的controller，里面有访问路径，哪些是不用登录就可以访问的，这些不用管。哪些是需要登录才能访问的，统一进行管理
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        //授权
        http.authorizeHttpRequests()
                .antMatchers(
                        "/user/setting",
                        "/user/upload",
                        "/discuss/add/**",
                        "/comment/add/**",
                        "/letter/**",
                        "/notice/**",
                        "/like",
                        "/follow",
                        "/unfollow"
                )
                .hasAnyAuthority(AUTHORITY_USER, AUTHORITY_ADMIN, AUTHORITY_MODERATOR) //这三个权限都可以访问上述路径
                .antMatchers("/discuss/top", "/discuss/wonderful")
                .hasAnyAuthority(AUTHORITY_MODERATOR)
                .antMatchers("/discuss/delete", "/data/**", "/actuator/**")
                .hasAnyAuthority(AUTHORITY_ADMIN)
                .anyRequest().permitAll() //除开上述的请求，其余请求都允许访问
                .and().csrf().disable(); //禁用csrf攻击的检查

        //权限不够时的处理
        http.exceptionHandling()  //SpringSecurity底层会捕获权限不够时的异常
                .authenticationEntryPoint(new AuthenticationEntryPoint() {//没登录时的处理（即还没通过认证）
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
                        // 针对不同的请求方式分别处理
                        String xRequestedWith = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equalsIgnoreCase(xRequestedWith)){// 异步请求，不能返回网页，而是返回JSON数据给出提示
                            response.setContentType("application/plain;charset=utf-8");
                            response.getWriter().write(CommunityUtil.getJSONString(HttpStatus.SC_FORBIDDEN, "你还没有登录！"));
                        }else {// 普通请求，重定向到登录页面
                            response.sendRedirect(request.getContextPath() + "/login");
                        }
                    }
                })
                .accessDeniedHandler(new AccessDeniedHandler() { //权限不足
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
                        String xRequestedWith = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equalsIgnoreCase(xRequestedWith)){// 异步请求，不能返回网页，而是返回JSON数据给出提示
                            response.setContentType("application/plain;charset=utf-8");
                            response.getWriter().write(CommunityUtil.getJSONString(HttpStatus.SC_FORBIDDEN, "你没有权限！"));
                        }else {// 普通请求，重定向到登录页面
                            response.sendRedirect(request.getContextPath() + "/denied");
                        }
                    }
                }) ;
        // Security底层默认会拦截/logout请求，进行退出处理
        // 覆盖它默认的逻辑，才能执行我们自己的退出代码
        http.logout().logoutUrl("/securitylogout") ;

        //认证。构建用户认证结果，并存入SecurityContext
        http.addFilterBefore(new Filter() {
                                 @Override
                                 public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
                                     HttpServletRequest request = (HttpServletRequest) servletRequest;
                                     String ticket= CookieUtil.getValue(request,"ticket");
                                     if (ticket!=null){
                                         //如果登录凭证存在，查询LoginTicket
                                         LoginTicket loginTicket = userService.findLoginTicket(ticket);
                                         //检查凭证是否有效
                                         if (loginTicket!=null&&loginTicket.getStatus()==0&& loginTicket.getExpired().after(new Date())){
                                             //根据凭证查询用户
                                             User user = userService.findUserById(loginTicket.getUserId());
                                             //构建用户认证结果，并存入SecurityContext，以便于Security授权
                                             UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                                                     user,
                                                     user.getPassword(),
                                                     userService.getAuthorities(user.getId())
                                             );
                                             SecurityContextHolder.setContext(new SecurityContextImpl(authenticationToken));

                                         }
                                     }
                                     filterChain.doFilter(servletRequest,servletResponse);
                                 }
                             }
                , UsernamePasswordAuthenticationFilter.class);
    }
}
