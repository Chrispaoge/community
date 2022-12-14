package com.nowcoder.community;

import com.nowcoder.community.util.SensitiveFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = Application.class)
public class SensitiveTests {

    @Autowired
    private SensitiveFilter sensitiveFilter ;

    @Test
    public void testSensitiveFilter(){
        String str = "这里可以赌博，可以吸毒，可以开票" ;
        String res = sensitiveFilter.filter(str);
        System.out.println(res);
    }
}
