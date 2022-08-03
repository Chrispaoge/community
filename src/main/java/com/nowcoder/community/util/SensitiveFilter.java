package com.nowcoder.community.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class) ;

    //当检测到有敏感词以后，需要替换成的符号做成一个常量
    private static final String REPLACEMENT = "***" ;

    //根节点
    private TrieNode rootNode = new TrieNode() ;

    //1.定义前缀树。这个结构只在本类中使用，其余地方不用，因此定义为内部类
    private class TrieNode{ //树型结构，就是描述节点与节点之间的关系
        //关键词结束标识
        private boolean isKeywordEnd = false ;

        //子节点。一个节点的子节点可能有多个，因此类型为map。key是下级字符，value是下级节点
        private Map<Character, TrieNode> subNodes = new HashMap<>() ;

        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }

        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }

        // 添加子节点
        public void addSubNode(Character c, TrieNode node){
            subNodes.put(c, node) ;
        }

        //获取子节点。通过字符拿到节点
        public TrieNode getSubNode(Character c){
            return subNodes.get(c) ;
        }
    }


    //2.根据敏感词，初始化前缀树
    @PostConstruct //当容器实例化SensitiveFilter这个Bean时(服务启动时就会初始化这个Bean)，含有@PostConstruct注解的方法将自动被调用
    public void init(){
        try(
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is)) ;//字节流中读取文件不太方便，转换为字符流，然后转换为缓冲流，效率高
        ){
            String keyword ;
            while ((keyword = reader.readLine()) != null){
                //添加到前缀树
                this.addKeyword(keyword) ;
            }
        }catch (IOException e){
            logger.error("加载敏感词文件失败：" + e.getMessage());
        }
    }

    //将一个敏感词添加到前缀树中
    private void addKeyword(String keyword) {
        TrieNode tempNode = rootNode ;
        for (int i = 0 ; i < keyword.length() ; i ++){
            char c = keyword.charAt(i);
            TrieNode subNode = tempNode.getSubNode(c);
            if (subNode == null){
                //初始化子节点
                subNode = new TrieNode() ;
                tempNode.addSubNode(c, subNode);
            }
            //指向子节点，进入下一轮循环
            tempNode = subNode ;
            if (i == keyword.length() - 1){
                tempNode.setKeywordEnd(true);
            }
        }
    }


    //3.编写过滤敏感词的方法
    /**
     * 过滤敏感词
     * @param text 待过滤的文本
     * @return 过滤后的文本
     */
    public String filter(String text){
        if (StringUtils.isBlank(text)){
            return null ;
        }
        TrieNode tempNode = rootNode ; //指针1
        int begin = 0 ;//指针2
        int position = 0 ;//指针3
        StringBuilder res = new StringBuilder() ;
        while (position < text.length()){
            char c = text.charAt(position) ;
            //跳过符号
            if (isSymbol(c)){
                //若指针1指向根节点，那么将此符号记入结果（正常字符），让指针2往下走一步
                if (tempNode == rootNode){
                    res.append(c) ;
                    begin ++ ;
                }
                //无论符号出现在开头还是中间，指针3都往后走一个
                position ++ ;
                continue;
            }
            //不是字符的话，那么检查下级节点
            tempNode = tempNode.getSubNode(c) ;
            if (tempNode == null){
                //以begin开头的字符串不是敏感词
                res.append(text.charAt(begin)) ;
                //进入下一个位置
                position = ++begin ;
                //重新指向根节点
                tempNode = rootNode ;
            }else if (tempNode.isKeywordEnd()){
                //发现敏感词，将begin-position字符串替换掉
                res.append(REPLACEMENT) ;
                //进入下一个位置
                begin = ++position ;
                //重新指向根节点
                tempNode = rootNode ;
            }else { //检查下一个字符
                position ++ ;
            }
        }
        //将最后一批字符计入结果
        res.append(text.substring(begin)) ;

        return res.toString() ;
    }

    //判断是否为符号
    private boolean isSymbol(Character c){
        //0x2E80 ~ 0x9FFF是东亚文字范围
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF) ;
    }
}
