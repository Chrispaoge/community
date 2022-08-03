package com.nowcoder.community.dao;

import com.nowcoder.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {

    //首页上帖子的分页显示。userId为0时表示所有用户，就不拼到sql中，为其他数则拼，这是为实现查看某个用户的帖子所考虑的字段
    //orderMode，默认值为0，表示还是按照原先的方式排序。为1表示按照默认值排
    List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit, int orderMode) ;

    //分页查询中一共有多少页是通过计算而来的，下面的方法是查询帖子的总数
    //如果只有一个参数，并且在<if>中使用，则必须加别名
    //对于上面虽然也是动态拼sql，但是由于有三个形参，因此可以不加@Param（@Param注解用于给参数取别名）
    int selectDiscussPostRows(@Param("userId") int userId) ;

    //增加帖子
    int insertDiscussPost(DiscussPost discussPost) ;

    //查询帖子详情。根据帖子id查询出帖子的详细信息
    DiscussPost selectDiscussPostById(int id) ;

    //更新评论数量
    int updateCommentCount(int id, int commentCount) ;

    //修改帖子类型。是否置顶
    int updateType(int id, int type) ;

    //修改帖子的状态。加精，删除
    int updateStatus(int id, int status) ;

    //修改帖子的分数
    int updateScore(int id, double score) ;
}
