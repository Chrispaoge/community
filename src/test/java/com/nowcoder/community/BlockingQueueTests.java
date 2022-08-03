package com.nowcoder.community;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class BlockingQueueTests {
    public static void main(String[] args) {
        //实例化阻塞队列（生产者消费者共用一个阻塞队列）
        BlockingQueue queue = new ArrayBlockingQueue(10) ;
        new Thread(new Producer(queue)).start(); //生产者线程
        new Thread(new Consumer(queue)).start(); //消费者线程1
        new Thread(new Consumer(queue)).start(); //消费者线程2
        new Thread(new Consumer(queue)).start(); //消费者线程3
    }
}

//生产者
class Producer implements Runnable{

    //生产者线程需要交给阻塞队列管理，因此需要将阻塞队列注入进来
    private BlockingQueue<Integer> queue ;
    public Producer(BlockingQueue<Integer> queue){
        this.queue = queue ;
    }

    @Override
    public void run() {
        try {
            for (int i = 0 ; i < 100; i ++){ //生产者生产100个数据，每个数据都需要放到队列中
                Thread.sleep(20);
                queue.put(i); //将生产者生产的数据放到队列中
                System.out.println(Thread.currentThread().getName() + "生产：" + queue.size());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class Consumer implements Runnable{

    //消费者线程同样需要交给阻塞队列管理，因此需要将阻塞队列注入进来
    private BlockingQueue<Integer> queue ;
    public Consumer(BlockingQueue<Integer> queue){
        this.queue = queue ;
    }

    @Override
    public void run() {
        //消费逻辑：只要队列中有数据，就消费
        try{
            while (true){
                //消费者的消费速度随机且没有生产者生产的速度快，符合现实
                Thread.sleep(new Random().nextInt(1000));
                queue.take() ;
                System.out.println(Thread.currentThread().getName() + "消费：" + queue.size());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}