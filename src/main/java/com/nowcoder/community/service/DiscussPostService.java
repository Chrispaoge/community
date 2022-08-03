package com.nowcoder.community.service;

import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.RedisKeyUtil;
import com.nowcoder.community.util.SensitiveFilter;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService {

    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class) ;

    @Autowired
    private DiscussPostMapper discussPostMapper ;

    @Autowired
    private SensitiveFilter sensitiveFilter ;

    //二级缓存
    @Autowired
    private RedisTemplate redisTemplate ;

    @Value("${caffeine.posts.max-size}")
    private int maxSize ;

    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds ;

    //Caffeine核心接口：Cache, LoadingCache, AsyncLoadingCache

    //帖子列表缓存
    private LoadingCache<String, List<DiscussPost>> postListCache ;

    //帖子总数缓存
    private LoadingCache<Integer, Integer> postRowsCache ;

    @PostConstruct
    public void init(){
        // 初始化帖子列表缓存。方式是固定的
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() { //缓存中不存在查询的数据时，执行下面的代码，即从数据库查询数据
                    @Override
                    public @Nullable List<DiscussPost> load(@NonNull String key) throws Exception {
                        if (key == null || key.length() == 0){
                            throw new IllegalArgumentException("参数错误！") ;
                        }
                        String[] params = key.split(":");
                        if (params == null || params.length != 2){
                            throw new IllegalArgumentException("参数错误！") ;
                        }
                        int offset = Integer.valueOf(params[0]) ;
                        int limit = Integer.valueOf(params[1]) ;

                        // 二级缓存：redis
                        String redisKey = RedisKeyUtil.getHotPostList() ;
                        Object obj = redisTemplate.opsForHash().get(redisKey, key);
                        if (obj != null){
                            if (obj instanceof List){
                                List<DiscussPost> posts = (List<DiscussPost>) obj;
                                Map<String, List<DiscussPost>> map = new HashMap<>() ;
                                map.put(key, posts) ;
                                logger.debug("load post list from Redis.");
                                return posts ;
                            }
                        }

                        //二级缓存为空，构建二级缓存
                        logger.debug("load post list from DB.");
                        List<DiscussPost> discussPosts = discussPostMapper.selectDiscussPosts(0, offset, limit, 1);
                        redisTemplate.opsForHash().put(redisKey, key, discussPosts);
                        return discussPosts ;
                    }
                }) ;
        // 初始化帖子总数缓存
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Override
                    public @Nullable Integer load(@NonNull Integer key) throws Exception {
                        // 二级缓存：redis
                        String redisKey = RedisKeyUtil.getHotPostRows() ;
                        Integer postRows = (Integer) redisTemplate.opsForValue().get(redisKey);
                        if (postRows != null){
                            logger.debug("load post rows from Redis.");
                            return postRows ;
                        }

                        //redis缓存为空，构建redis缓存
                        logger.debug("load post rows from DB.");
                        int rows = discussPostMapper.selectDiscussPostRows(key);
                        redisTemplate.opsForValue().set(redisKey, rows);
                        return rows ;
                    }
                }) ;


    }

    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode){
        if (userId == 0 && orderMode == 1){ // 只缓存热门帖子
            return postListCache.get(offset + ":" + limit) ;
        }

        logger.debug("load post list from DB.");
        return discussPostMapper.selectDiscussPosts(userId, offset, limit, orderMode) ;
    }

    public int findDiscussPostRows(int userId){
        if (userId == 0){ //说明是访问了首页，查询帖子（userId=0表示查询所有帖子）
            return postRowsCache.get(userId) ;
        }

        logger.debug("load post rows from DB.");
        return discussPostMapper.selectDiscussPostRows(userId) ;
    }

    public int addDiscussPost(DiscussPost post){
        if (post == null){
            throw new IllegalArgumentException("参数不能为空！") ;
        }

        //1.去掉标签。使得浏览器认为<script>abc</script>这种，是文本，而不是标签。Spring中自带的工具HtmlUtils就可以进行处理
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));
        //2.对post中的title和content内容进行敏感词过滤
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));

        return discussPostMapper.insertDiscussPost(post) ;
    }

    public DiscussPost findDiscussPostById(int id){
        return discussPostMapper.selectDiscussPostById(id) ;
    }

    //更新帖子评论的数量
    public int updateCommentCount(int id, int commentCount){
        return discussPostMapper.updateCommentCount(id, commentCount) ;
    }

    public int updateType(int id, int type){
        return discussPostMapper.updateType(id, type) ;
    }

    public int updateStatus(int id, int type){
        return discussPostMapper.updateStatus(id, type) ;
    }

    public int updateScore(int id, double score){
        return discussPostMapper.updateScore(id, score) ;
    }
}
