package com.nowcoder.community.service;

import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {

    @Autowired
    private UserMapper userMapper ;

    @Autowired
    private MailClient mailClient ; //注册需要发邮件，因此注入邮件客户端

    @Autowired
    private TemplateEngine templateEngine ; //TemplateEngine是受Spring管理的，这里红线是IDEA BUG

    @Autowired
    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private RedisTemplate redisTemplate ;

    //发邮件需要生成激活码，包含域名+项目名，所以需要把配置文件中的域名+项目名都注入进来
    @Value("${community.path.domain}") //从配置文件中取
    private String domain ; //域名：http://localhost:8080

    @Value("${server.servlet.context-path}")
    private String contextPath ; //项目名：community

    //根据userId查到user
    public User findUserById(int id){
        // return userMapper.selectById(id);
        User user = getCache(id) ;//先从缓存查
        if (user == null){ //缓存没查到，初始化缓存
            user = initCache(id);
        }
        return user ;
    }

    // 注册代码逻辑就一个，判断参数是否有，有的话看数据库中是否存在，有问题将错误信息封装到map中返回给用户
    // 都不存在就注册（即往数据库User表中插数据）
    public Map<String, Object> register(User user){
        Map<String, Object> map = new HashMap<>() ;

        //空参处理
        if (user == null){
            throw new IllegalArgumentException("参数不能为空!") ;
        }

        //空值处理。这是业务的错误，不是代码的问题（用户自己不输入账号就提交），因此不是抛异常，而是返回错误信息给用户即可
        if (StringUtils.isBlank(user.getUsername())){
            map.put("usernameMsg", "账号不能为空！") ;
            return map ;
        }
        if (StringUtils.isBlank(user.getPassword())){
            map.put("passwordMsg", "密码不能为空！") ;
            return map ;
        }
        if (StringUtils.isBlank(user.getEmail())){
            map.put("emailMsg", "邮箱不能为空！") ;
            return map ;
        }

        //都不为空（包含null,"",空格），那么就查数据库中是否已经存在了
        //验证账号
        User u = userMapper.selectByName(user.getUsername());
        if (u != null){ //说明此用户名数据库中已经存在了
            map.put("usernameMsg", "该账号已经存在！") ;
            return map ;
        }
        //验证邮箱
        u = userMapper.selectByEmail(user.getEmail()) ;
        if (u != null){//说明此邮箱数据库中已经存在了
            map.put("emailMsg", "该邮箱已经存在") ;
            return map ;
        }

        //能走到这一步，说明参数都是没问题的，可以注册账号了
        //注册用户。即将用户注册到数据库中
        //密码进行加密，然后覆盖掉用户原来输入的密码
        user.setSalt(CommunityUtil.generateUUID().substring(0,5));//拿前5位作为盐
        user.setPassword(CommunityUtil.md5(user.getPassword()+user.getSalt()));
        user.setType(0);//其余信息
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png"
            , new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user) ;

        //激活邮件。这就和thymeleaf相关了
        Context context = new Context() ; //thymeleaf包下的
        context.setVariable("email", user.getEmail());
        // 定义url，即你希望此服务器使用啥url来处理这个请求。例如下面，101指的是用户di
        //http://localhost8080/community/activation/101/code
        String url = domain + contextPath + "/activation/" +
                user.getId() + "/" + user.getActivationCode() ;
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context) ;
        mailClient.sendMail(user.getEmail(), "激活账号", content);
        return map ;
    }

    //验证激活码，返回激活状态
    //传入的参数由url定，url中包含的是userId和激活码code
    public int activation(int userId, String code){
        User user = userMapper.selectById(userId);
        if (user.getStatus() == 1){ //说明是重复的激活
            return ACTIVATION_REPEAT ;
        }else if (user.getActivationCode().equals(code)){ //验证正确
            userMapper.updateStatus(userId, 1) ; //修改此用户的状态
            clearCache(userId); //删除用户缓存
            return ACTIVATION_SUCCESS ;
        }else {
            return ACTIVATION_FAILURE ;
        }
    }

    /**
     * 登录
     * @param username：用户名
     * @param password：密码（明文），而数据库中存的是加密后的，因此不能直接比，而是加密后再比
     * @param expiredSeconds：多少秒后，凭证过期
     * @return：登录可能成功，也可能失败，失败的原因有很大，所以返回map，即返回多种情况
     */
    public Map<String, Object> login(String username, String password, int expiredSeconds) {
        Map<String, Object> map = new HashMap<>();

        // 空值处理
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号不能为空!");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }

        // 验证账号
        User user = userMapper.selectByName(username);
        if (user == null) {
            map.put("usernameMsg", "该账号不存在!");
            return map;
        }

        // 验证状态
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "该账号未激活!");
            return map;
        }

        // 验证密码
        password = CommunityUtil.md5(password + user.getSalt()); //先对明文密码进行md5加密
        if (!user.getPassword().equals(password)) {
            map.put("passwordMsg", "密码不正确!");
            return map;
        }

        // 生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
        // loginTicketMapper.insertLoginTicket(loginTicket);
        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket()) ;
        redisTemplate.opsForValue().set(redisKey, loginTicket); //redis将loginTicket序列化为JSON字符串然后存

        //登录成功，需要将凭证返回（最终要发给客户端的）
        map.put("ticket", loginTicket.getTicket());
        return map;
    }

    //退出登录，将状态修改为1
    public void logout(String ticket) {
        // loginTicketMapper.updateStatus(ticket, 1);
        String redisKey = RedisKeyUtil.getTicketKey(ticket) ;
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey); //先取出值
        loginTicket.setStatus(1); //修改状态
        redisTemplate.opsForValue().set(redisKey, loginTicket); //再存回去
    }

    //查询登录凭证
    public LoginTicket findLoginTicket(String ticket) {
        // return loginTicketMapper.selectByTicket(ticket);
        String redisKey = RedisKeyUtil.getTicketKey(ticket) ;
        return (LoginTicket) redisTemplate.opsForValue().get(redisKey);
    }

    //更新用户头像
    public int updateHeader(int userId, String headerUrl) {
        // return userMapper.updateHeader(userId, headerUrl);
        int rows = userMapper.updateHeader(userId, headerUrl);
        clearCache(userId);
        return rows ;
    }

    public User findUserByName(String username){
        return userMapper.selectByName(username) ;
    }

    //redis做缓存一般三个步骤，封装为3个方法。同时这3个方法都是Service内部使用
    //1.优先从缓冲中取值
    private User getCache(int userId){
        String redisKey = RedisKeyUtil.getUserKey(userId) ;
        return (User) redisTemplate.opsForValue().get(redisKey);
    }

    //2.取不到时去数据库中取，并初始化缓存数据
    private User initCache(int userId){
        User user = userMapper.selectById(userId) ; //从MySQL中取出此用户的值
        String redisKey = RedisKeyUtil.getUserKey(userId) ;
        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);
        return user ;
    }

    //3.数据变更时清楚缓存数据
    private void clearCache(int userId){
        String redisKey = RedisKeyUtil.getUserKey(userId) ;
        redisTemplate.delete(redisKey) ;
    }

    //查询用户权限
    public Collection<? extends GrantedAuthority> getAuthorities(int userId){
        User user = this.findUserById(userId) ;

        List<GrantedAuthority> list = new ArrayList<>() ;
        list.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                switch (user.getType()){
                    case 1:
                        return AUTHORITY_ADMIN ;
                    case 2:
                        return AUTHORITY_MODERATOR ;
                    default:
                        return AUTHORITY_USER ;
                }
            }
        }) ;
        return list ;
    }
}
