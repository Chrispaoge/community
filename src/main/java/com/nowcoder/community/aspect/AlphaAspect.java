package com.nowcoder.community.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

//@Component
//@Aspect
public class AlphaAspect {

    /**定义切点
     * 这个表达式匹配了这个包下所有的方法
     * execution：固定的关键字
     * 第一个*：方法的返回值，*表示所有的返回值都行
     * com.nowcoder.community.service.*：这个包下的所有的类
     * *(..)：所有的方法的所有参数。可以写add*，就是表示所有以add开头的方法，很灵活
     */
    @Pointcut("execution(* com.nowcoder.community.service.*.*(..))")
    public void pointcut(){
    }

    //有了切点后就可以定义通知了。通知分为5类，通过注解声明，里面写连接点，就是写切点
    //1.前置通知
    @Before("pointcut()") //本通知针对pointcut()这个切点有效
    public void before(){
        System.out.println("before");
    }

    //2.后置通知
    @After("pointcut()")
    public void after(){
        System.out.println("after");
    }

    //3.返回值后通知
    @AfterReturning("pointcut()")
    public void afterReturning(){
        System.out.println("afterReturning");
    }

    //4.抛异常时织入代码
    @AfterThrowing("pointcut()")
    public void afterThrowing(){
        System.out.println("AfterThrowing");
    }

    //5.环绕织入。前后都织入，有参数有返回值，参数代表连接点
    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable{
        System.out.println("around before");
        Object obj = joinPoint.proceed(); //调用目标组件的方法。调用此方法的前后可以进行方法的增强
        System.out.println("around after");
        return obj ;
    }
}
