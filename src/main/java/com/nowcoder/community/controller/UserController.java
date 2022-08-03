package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private String uploadPath; //上传路径

    @Value("${community.path.domain}")
    private String domain; //项目域名

    @Value("${server.servlet.context-path}")
    private String contextPath; //项目访问路径

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder; //更新的是当前用户的头像，因此需要从hostHolder中取当前用户

    @Autowired
    private LikeService likeService ;

    @Autowired
    private FollowService followService ;

    @Value("${qiniu.key.access}")
    private String accessKey ;

    @Value("${qiniu.key.secret}")
    private String secretKey ;

    @Value("${qiniu.bucket.header.name}")
    private String headerBucketName ;

    @Value("${qiniu.bucket.header.url}")
    private String headerBucketUrl ;

    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage(Model model) {
        // 上传文件名称
        String fileName = CommunityUtil.generateUUID() ;
        // 设置响应信息
        StringMap policy = new StringMap() ;
        policy.put("returnBody", CommunityUtil.getJSONString(0)) ; //上传成功，JSON中返回0
        // 生成上传凭证（不然无法将上传的图片存到七牛云）
        Auth auth = Auth.create(accessKey, secretKey) ;
        String uploadToken = auth.uploadToken(headerBucketName, fileName, 3600, policy) ;

        model.addAttribute("uploadToken", uploadToken) ;
        model.addAttribute("fileName", fileName) ; //页面利用这些数据，重新构造表单，异步提交给七牛云

        return "/site/setting";
    }

    // 更新头像路径
    @PostMapping(path = "/header/url")
    @ResponseBody
    public String updateHeaderUrl(String fileName){
        if (StringUtils.isBlank(fileName)){
            return CommunityUtil.getJSONString(1, "文件名不能为空!") ;
        }

        String url = headerBucketUrl + "/" + fileName ;
        userService.updateHeader(hostHolder.getUser().getId(), url) ;

        return CommunityUtil.getJSONString(0) ;
    }

    /**
     * 上传文件。表单提交方式必须为POST
     * @param headerImage：图片文件。由于合理只上传一张图片，因此为单个参数。若是多个文件，那么为MultipartFile[]
     * @param model
     * @return
     */
    // 废弃
    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model) {
        if (headerImage == null) { //参数校验
            model.addAttribute("error", "您还没有选择图片!");
            return "/site/setting";
        }

        //存图片的时候，不能按照图片原始的文件名来存。例如1.png，可能有很多个用户传的文件名都为1.png
        //那么就会发生覆盖，因此做法是生成随机的文件名存(注意后缀不要替换)
        String fileName = headerImage.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf(".")); //文件名后缀
        if (StringUtils.isBlank(suffix)) { //判断下有无后缀
            model.addAttribute("error", "文件的格式不正确!");
            return "/site/setting";
        }

        // 生成随机文件名
        fileName = CommunityUtil.generateUUID() + suffix;
        // 确定文件存放的路径
        File dest = new File(uploadPath + "/" + fileName);
        try {
            // 存储文件。将headerImage存到dest中
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败: " + e.getMessage());
            throw new RuntimeException("上传文件失败,服务器发生异常!", e);
        }

        // 执行到这，说明存储图像成功，那么需要更新当前用户的头像的路径(注意是web访问路径，而不是文件的存储路径)
        // http://localhost:8080/community/user/header/xxx.png
        // 给用户提供读取图片方法的时候需要按照这个路径来处理这样的请求
        User user = hostHolder.getUser(); //获取当前用户
        String headerUrl = domain + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(), headerUrl);

        return "redirect:/index";
    }

    /**
     * 获取头像。
     * 访问的路径为，例如http://localhost:8080/community/user/header/xxx.png
     * @param fileName：即上面的文件名：xxx.png
     * @param response
     * 此方法向浏览器响应的不是网页，也不是字符串，而是二进制数据（图片），需要通过流手动往浏览器输出，因此返回值为void
     */
    //废弃
    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        // 服务器存放路径
        fileName = uploadPath + "/" + fileName;
        // 文件后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        // 响应图片
        response.setContentType("image/" + suffix);
        //将字节流写入到response中
        try (
              OutputStream os = response.getOutputStream(); //由于response由SpringMVC管理，因此这个输出流用完会自动关闭
              FileInputStream fis = new FileInputStream(fileName); //编译时自动加上finally，然后在finally中自动执行close方法
        ) {
            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败: " + e.getMessage());
        }
    }

    // 个人主页
    @GetMapping(path = "/profile/{userId}") //还可看别人的个人主页，因此需要传userId
    public String getProfilePage(@PathVariable("userId") int userId, Model model) {
        User user = userService.findUserById(userId);
        if (user == null){
            throw new RuntimeException("该用户不存在!") ;
        }

        //用户
        model.addAttribute("user", user) ;
        //点赞
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);

        //关注数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);//这里仅查关注的用户的数量，用户的实体常量为3
        model.addAttribute("followeeCount", followeeCount) ;
        //粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId) ;
        model.addAttribute("followerCount", followerCount) ;
        //当前登录用户对此用户是否已关注。明显这是当前用户查看别人的个人主页才有的功能
        boolean hasFollowed = false ;
        if (hostHolder.getUser() != null){//没登陆也可以查看别人的个人主页，因此需要判断是否登录
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed) ;
        return "/site/profile" ;
    }
}
