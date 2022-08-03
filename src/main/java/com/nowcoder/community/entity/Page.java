package com.nowcoder.community.entity;

import lombok.Getter;

//封装分页相关的信息。这个类只在表现层中使用
@Getter
public class Page {

    // 当前页码
    private int current = 1 ; //默认第一页

    //显示上限。即每页显示的数据
    private int limit = 10 ;

    //数据总数:用于计算总的页数
    private int rows ;

    //查询路径:用来复用分页链接
    private String path ;

    //get方法可以使用注解，但是set方法的话，需要规避掉传入是负数的情况
    public void setCurrent(int current) {
        if (current >= 1){
            this.current = current;
        }
    }

    public void setLimit(int limit) {
        if (limit >= 1 && limit <= 100){
            this.limit = limit;
        }
    }

    public void setRows(int rows) {
        if(rows >= 1){
            this.rows = rows;
        }
    }

    public void setPath(String path) {
        this.path = path;
    }

    //分页实体类还需要补充几个条件，这几个是由上面的字段计算得到的
    //数据库在分页查的时候，需要传入offset当前页的起始行，需要计算得到

    public int getOffset(){ //获取当前页的起始行
        return (current-1) * limit ;
    }

    public int getTotal(){ //获取总的页数。页面上显示页码的时候用
        if (rows % limit == 0){ //可以整除
            return rows / limit ;
        }else {
            return rows / limit + 1 ;
        }
    }

    //根据当前页显示的页码，例如当前页是第3页，那么显示的页码为1,2,3,4,5
    //获取启示页码
    public int getFrom(){
        int from = current - 2 ;
        return Math.max(from, 1);
    }

    //获取结束页码
    public int getTo(){
        int to = current+2 ;
        int total = getTotal() ;
        return Math.min(to, total);
    }
}
