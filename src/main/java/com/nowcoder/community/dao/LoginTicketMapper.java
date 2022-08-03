package com.nowcoder.community.dao;

import com.nowcoder.community.entity.LoginTicket;
import org.apache.ibatis.annotations.*;

@Mapper
@Deprecated
public interface LoginTicketMapper {

    //ticket是核心，整张表都是围绕ticket设计，剩下的几个字段属于为查询用户数据而存的

    //插入数据
    @Insert({
            "insert into login_ticket(user_id,ticket,status,expired) ", //id是主键自动生成，这里不用写
            "values(#{userId},#{ticket},#{status},#{expired})" //和loginTicket字段名保持一致
    })
    @Options(useGeneratedKeys = true, keyProperty = "id") //主键生成的值会自动注入给loginTicket，需要指定注入给哪个属性
    int insertLoginTicket(LoginTicket loginTicket);


    //查
    @Select({
            "select id,user_id,ticket,status,expired ",
            "from login_ticket where ticket=#{ticket}"
    })
    LoginTicket selectByTicket(String ticket);

    //修改状态（删除数据）。互联网行业真正在数据表中删除数据很少，一般都是改变状态
    //注解中也可以写动态sql，例如if，和xml中写一样的，不过前面需要套上<script>标签
    @Update({
            "<script>",
            "update login_ticket set status=#{status} where ticket=#{ticket} ",
            "<if test=\"ticket!=null\"> ",
            "and 1=1 ",  //这里仅为了演示注解中写if，实际上这条sql不需要用到if
            "</if>",
            "</script>"
    })
    int updateStatus(String ticket, int status); //status表示需要改变的状态
}
