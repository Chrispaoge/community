package com.nowcoder.community.dao.elasticsearch;

import com.nowcoder.community.entity.DiscussPost;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 此接口不需要实现任何的方法，只需要继承ElasticsearchRepository即可，声明接口处理的实体类是DiscussPost
 * 实体类中的主键是Integer类型。此父接口中已定义好了对es的CRUD，继承了后，Spring会自动实现这些方法，我们只管调用
 */
@Repository
public interface DiscussPostRepository extends ElasticsearchRepository<DiscussPost, Integer> {
}
