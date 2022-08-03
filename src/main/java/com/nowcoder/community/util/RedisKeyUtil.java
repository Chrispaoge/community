package com.nowcoder.community.util;

public class RedisKeyUtil {

    private static final String SPLIT = ":" ;
    private static final String PREFIX_ENTITY_LIKE = "like:entity" ;
    private static final String PREFIX_USER_LIKE = "like:user" ;
    private static final String PREFIX_FOLLOWEE = "followee" ;
    private static final String PREFIX_FOLLOWER = "follower" ;
    private static final String PREFIX_KAPTCHA = "kaptcha" ;
    private static final String PREFIX_TICKET = "ticket" ;
    private static final String PREFIX_USER = "user" ;
    private static final String PREFIX_UV = "uv" ;
    private static final String PREFIX_DAU = "dau" ;
    private static final String PREFIX_POST = "post" ;

    /**
     * 根据实体类型和实体id生成对应的key。
     * 最终的key为：like:entity:entityType:entityId，value为set集合，里面存的是userId，表示谁给这个key点的赞
     * @param entityType：实体类型
     * @param entityId：实体id
     * @return：对应的key
     */
    public static String getEntityLikeKey(int entityType, int entityId){
        return PREFIX_ENTITY_LIKE + SPLIT + entityType + SPLIT + entityId ;
    }

    //某个用户获取的总赞数
    public static String getUserLikeKey(int userId){
        return PREFIX_USER_LIKE + SPLIT + userId ;
    }

    //某个用户关注的实体(用户可以关注用户，帖子，课程等等)
    //followee:userId:entityType -> zset(entityId, 关注时间作为分数)
    public static String getFolloweeKey(int userId, int entityType){
        return PREFIX_FOLLOWEE + SPLIT + userId + SPLIT + entityType ;
    }

    //某个实体拥有的粉丝
    //follower:entityType:entityId -> zset(userId, 关注时间作为分数)
    public static String getFollowerKey(int entityType, int entityId){
        return PREFIX_FOLLOWER + SPLIT + entityType + SPLIT + entityId ;
    }


    /**
     * 登录验证码
     * 获取登录验证码时，验证码和某个用户是相关的，不同用户的验证码应该是不一样的。由于用户在登录页面看到登录的验证码时，此时用户还没登录，
     * 因此要想实现验证码和用户绑定，使用userId是不行的。可行的做法是在用户访问登录页面时，给其发一个凭证（随机生成的字符串）存在cookie中
     * 以这个凭证来临时标识这个用户，然后很快让字符串过期即可
     * @param owner:用户的临时凭证（未登录）
     * @return
     */
    public static String getKaptchaKey(String owner){
        return PREFIX_KAPTCHA + SPLIT + owner;
    }

    //登录的凭证
    public static String getTicketKey(String ticket){
        return PREFIX_TICKET + SPLIT + ticket ;
    }

    //用户
    public static String getUserKey(int userId){
        return PREFIX_USER + SPLIT + userId ;
    }

    // 单日UV
    public static String getUVKey(String date){
        return PREFIX_UV + SPLIT + date ;
    }

    // 区间UV
    public static String getUVKey(String startDate, String endDate){
        return PREFIX_UV + SPLIT + startDate + SPLIT + endDate ;
    }

    // 单日活跃用户
    public static String getDAUKey(String date){
        return PREFIX_DAU + SPLIT + date ;
    }

    // 区间活跃用户
    public static String getDAUKey(String startDate, String endDate){
        return PREFIX_DAU + SPLIT + startDate + SPLIT + endDate ;
    }

    // 帖子分数
    public static String getPostScoreKey(){
        return PREFIX_POST + SPLIT + "score" ;
    }

    // 热门帖子列表
    public static String getHotPostList(){
        return PREFIX_POST + SPLIT + "hot" ;
    }

    // 热门帖子总数
    public static String getHotPostRows(){
        return PREFIX_POST + SPLIT + "rows" ;
    }
}
