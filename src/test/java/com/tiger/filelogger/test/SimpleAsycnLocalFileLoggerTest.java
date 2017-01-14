package com.tiger.filelogger.test;

import com.tiger.filelogger.SimpleAsyncFileLogger;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * 基本日志记录器－测试
 * Created by laibao
 */
public class SimpleAsycnLocalFileLoggerTest {

    public static void main(String[] args) throws IOException, InterruptedException {

        final SimpleAsyncFileLogger logger = new SimpleAsyncFileLogger("/Users/zhoufeng/aaa.log" ,true);

        int threadCount = 50 ;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        long start = System.currentTimeMillis();
        for(int t = 1 ; t <= threadCount ; t++){
            Thread thread = new Thread(){
                @Override
                public void run() {
                    for(int i = 0 ; i < 100000 ; i++) {
                        try {
                            logger.log(Thread.currentThread().getName() + ",hahaha," + i);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    latch.countDown();
                    System.out.println(Thread.currentThread().getName() + " over");
                }
            };
            thread.start();
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println( "time:" + (end - start));

    }

}
