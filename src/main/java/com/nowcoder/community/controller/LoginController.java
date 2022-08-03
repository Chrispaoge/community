package com.nowcoder.community.controller;

import com.google.code.kaptcha.Producer;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class) ;

    @Autowired
    private UserService userService ;

    @Autowired
    private Producer kaptchaProducer ;

    @Autowired
    private RedisTemplate redisTemplate ;

    @GetMapping(path = "/register")
    public String getRegisterPage(){
        return "/site/register" ;
    }

    @GetMapping(path = "/login")
    public String getLoginPage(){
        return "/site/login" ;
    }

    @Value("${server.servlet.context-path}") //项目路径，cookie中设置作用范围时用到
    private String contextPath ;

    // 定义方法处理注册请求。
    // 注册时传入账号，密码，邮箱，可以使用3个参数接收这三个值，还可以直接声明user对象。
    // 只要页面传入值的时候和user的属性相匹配，springmvc就会自动将值注入到user中对应的属性
    @PostMapping(path = "/register")
    public String register(Model model, User user){
        Map<String, Object> map = userService.register(user);
        if (map == null || map.isEmpty()){ //说明注册成功
            model.addAttribute("msg",
                    "注册成功，我们已经向您的邮箱发送了一封激活邮件，请尽快激活") ;
            model.addAttribute("target", "/index") ;
            return "/site/operate-result" ; //视图view可以通过${msg}和${target}来取上面设置的值
        }else { //注册失败，向页面传失败的消息，显示给用户看
            model.addAttribute("usernameMsg", map.get("usernameMsg")) ;
            model.addAttribute("passwordMsg", map.get("passwordMsg")) ;
            model.addAttribute("emailMsg", map.get("emailMsg")) ;
            return "/site/register" ; //失败还是返回登录页面，继续注册
        }
    }

    @GetMapping("/activation/{userId}/{code}")
    public String activation(Model model, @PathVariable int userId, @PathVariable String code){
        int result = userService.activation(userId, code);
        if (result == ACTIVATION_SUCCESS){
            model.addAttribute("msg", "激活成功，您的账号已经可以正常使用了") ;
            model.addAttribute("target", "/login") ;
        }else if (result == ACTIVATION_REPEAT){
            model.addAttribute("msg", "无效操作，该账号已经激活过了") ;
            model.addAttribute("target", "/index") ;
        }else {
            model.addAttribute("msg", "激活失败，您提供的激活码不正确") ;
            model.addAttribute("target", "/index") ;
        }
        return "/site/operate-result" ;
    }


    // 服务器向浏览器输出的是一个图片，不是字符串也不是网页，需要手动response输出，所以方法返回的是void
    // 生成的验证码服务器需要记住，原先的验证码存在session中，现存在redis中
    // 验证码需要和用户绑定（不同的用户不同的验证码），因此生成一个临时的凭证标记此用户，存在用户客户端的cookie中
    @GetMapping(path = "/kaptcha")
    public void getKaptcha(HttpServletResponse response) {
        // 生成验证码
        String text = kaptchaProducer.createText(); //根据配置生成字符串
        BufferedImage image = kaptchaProducer.createImage(text); //根据字符串生成图片

        // 原先做法：将验证码存入session
        // session.setAttribute("kaptcha", text);

        // 现在做法：将验证码存在redis中
        // 验证码的归属
        String kaptchaOwner = CommunityUtil.generateUUID() ; //临时凭证，需要存在客户端的cookie中
        Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner) ;
        cookie.setMaxAge(60); //此cookie的有效时间：60s
        cookie.setPath(contextPath); //整个项目下都有效
        response.addCookie(cookie); //发送给客户端
        // 将验证码存入redis
        String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner) ;
        redisTemplate.opsForValue().set(redisKey, text, 60, TimeUnit.SECONDS);

        // 将图片输出给浏览器
        response.setContentType("image/png");

        //需要获取response输出流才能响应到浏览器。
        try {
            OutputStream os = response.getOutputStream();
            ImageIO.write(image, "png", os); //往浏览器中写图片
        } catch (IOException e) {
            logger.error("响应验证码失败:" + e.getMessage());
        }
    }

    //之前是从session中取的验证码，现在从redis中取。从redis中需要先拿到key，而要想获取key，首先需要从
    //cookie中获取登录凭证，因此加上@CookieValue来获取
    @PostMapping("/login")
    public String login(String username, String password, String code, boolean rememberme,
                        Model model, HttpServletResponse response, @CookieValue("kaptchaOwner") String kaptchaOwner) {
        // 检查验证码。
        // String kaptcha = (String) session.getAttribute("kaptcha"); //原先从session中取
        String kaptcha = null ;
        if (StringUtils.isNoneBlank(kaptchaOwner)){ //数据没有失效（临时凭证60s过期 ）
            String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner) ;
            kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
        }
        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) {
            model.addAttribute("codeMsg", "验证码不正确!");
            return "/site/login";
        }
        // 检查账号,密码。需要调用业务层
        int expiredSeconds = rememberme ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        if (map.containsKey("ticket")) { //登录成功（成功map中才有ticket）
            //给客户端发送cookie，里面带上ticket
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath); //访问这个项目的任何页面，登录都应该是有效的，因此这里cookie生效范围为整个项目
            cookie.setMaxAge(expiredSeconds); //cookie有效时间
            response.addCookie(cookie); //将cookie发送给浏览器
            return "redirect:/index"; //登录成功需要重定向到首页
        } else { //失败，将错误信息发送给浏览器
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/login";
        }
    }

    @GetMapping(path = "/logout")
    public String logout(@CookieValue("ticket") String ticket) {
        userService.logout(ticket);
        SecurityContextHolder.clearContext();
        return "redirect:/login"; //重定向到登录页面
    }
}
