package com.nowcoder.community;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.elasticsearch.DiscussPostRepository;
import com.nowcoder.community.entity.DiscussPost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = Application.class)
public class ElasticsearchTests {

    @Autowired
    private DiscussPostMapper discussPostMapper ;//从MySQL中取出数据，转存到es中

    @Autowired
    private DiscussPostRepository discussPostRepository ; //es的CRUD可以用这个，简单

    @Autowired
    private RestHighLevelClient restHighLevelClient; //使用es来搜索

    @Test
    public void testInsert(){
        discussPostRepository.save(discussPostMapper.selectDiscussPostById(241)) ; //插入一条数据
        discussPostRepository.save(discussPostMapper.selectDiscussPostById(242)) ;
        discussPostRepository.save(discussPostMapper.selectDiscussPostById(243)) ;
    }

    @Test
    public void testUpdate(){
        DiscussPost post = discussPostMapper.selectDiscussPostById(231);
        post.setContent("我是新人，使劲灌水!");
        discussPostRepository.save(post) ;
    }

    @Test
    public void testDelete(){
        discussPostRepository.deleteById(231);
        //discussPostRepository.deleteAll();
    }

    //搜索：es最核心的功能
    @Test
    public void testSearchByRepository(){
        //构造查询和排序的条件
        SearchRequest searchRequest = new SearchRequest("discusspost");//discusspost是索引名，就是表名
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content")) //查询条件
                .sort(SortBuilders.fieldSort("type").order(SortOrder.DESC)) //排序条件。type是否置顶，score加精贴的分高
                .sort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .sort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .from(1) //分页
                .size(10)
                .highlighter(new HighlightBuilder()
                        .field("title") //高亮显示
                        .field("content")
                        .requireFieldMatch(false)
                        .preTags("<span style='color:red'>")
                        .postTags("</span>")
                ) ;
        searchRequest.source(searchSourceBuilder);

        //查询
        try {
            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = response.getHits(); //此次搜索获得的数据
            System.out.println(hits.getTotalHits()); //111 hits
            System.out.println(response.getTook()); //116ms。搜索速度还是很快的
            for (SearchHit hit : hits) {
                DiscussPost discussPost = JSONObject.parseObject(hit.getSourceAsString(), DiscussPost.class);
                //处理高亮显示的结果。用高亮显示的覆盖原来的
                HighlightField title = hit.getHighlightFields().get("title"); //获取与title有关的高亮显示的内容
                if (title != null){
                    //可能匹配多段，这里只将第一段高亮。例如title为互联网xxx寒冬xxxx互联网，那么会匹配3段，只高亮显示第一段
                    discussPost.setTitle(title.getFragments()[0].toString());
                }
                HighlightField content = hit.getHighlightFields().get("content");
                if (content != null){
                    discussPost.setContent(content.getFragments()[0].toString());
                }
                System.out.println(discussPost);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
