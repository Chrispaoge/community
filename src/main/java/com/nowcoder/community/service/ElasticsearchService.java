package com.nowcoder.community.service;

import com.alibaba.fastjson.JSONObject;
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
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ElasticsearchService {

    @Autowired
    private DiscussPostRepository discussPostRepository ;

    @Autowired
    private RestHighLevelClient restHighLevelClient ;

    public void saveDiscussPost(DiscussPost post){
        discussPostRepository.save(post) ;
    }

    public void deleteDiscussPost(int id){
        discussPostRepository.deleteById(id);
    }

    public Map<String, Object> searchDiscussPost(String keyword, int offset, int limit){
        Map<String, Object> map = new HashMap<>() ;
        //构造查询和排序的条件
        SearchRequest searchRequest = new SearchRequest("discusspost");//discusspost是索引名，就是表名
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(QueryBuilders.multiMatchQuery(keyword, "title", "content")) //查询条件
                .sort(SortBuilders.fieldSort("type").order(SortOrder.DESC)) //排序条件。type是否置顶，score加精贴的分高
                .sort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .sort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .from(offset).size(limit) //分页
                .highlighter(new HighlightBuilder().field("title").field("content")
                        .requireFieldMatch(false).preTags("<em>").postTags("</em>")//高亮显示
                ) ;
        searchRequest.source(searchSourceBuilder);
        List<DiscussPost> discussPosts;
        //查询
        try {
            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = response.getHits(); //此次搜索获得的数据
            discussPosts = new ArrayList<>();
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
                discussPosts.add(discussPost);
            }
            map.put("posts",discussPosts);
            map.put("rows",hits.getTotalHits().value);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return map ;
    }
}
