package com.nowcoder.community.util;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

public class CommunityUtil {

    //生成随机字符串
    public static String generateUUID(){
        return UUID.randomUUID().toString().replace("-", "") ;
    }

    //MD5加密
    public static String md5(String key){
        if (StringUtils.isBlank(key)){ //key为null，空格，空串""，都认为是空
            return null ;
        }
        return DigestUtils.md5DigestAsHex(key.getBytes(StandardCharsets.UTF_8)) ;
    }

    /**
     * 根据传入的参数，得到JSON格式的字符串
     * 服务器往浏览器返回的json数据，往往需要包含几部分的内容：编码+提示信息+业务数据
     * @param code：编码。如404等状态码
     * @param msg：提示信息，如成功
     * @param map：业务数据
     * @return：JSON格式的字符串
     */
    public static String getJSONString(int code, String msg, Map<String, Object> map){
        //将这三部分封装到json对象中
        JSONObject json = new JSONObject() ;
        json.put("code", code);
        json.put("msg", msg) ;
        if (map != null){
            for (String key : map.keySet()) {
                json.put(key, map.get(key)) ;
            }
        }
        return json.toJSONString() ;
    }

    public static String getJSONString(int code, String msg){
        return getJSONString(code, msg, null) ;
    }

    public static String getJSONString(int code){
        return getJSONString(code, null, null) ;
    }
}
