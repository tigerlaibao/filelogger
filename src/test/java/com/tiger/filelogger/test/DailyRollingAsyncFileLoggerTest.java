package com.tiger.filelogger.test;

import com.tiger.filelogger.DailyRollingAsyncFileLogger;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * 滚动日志记录器－测试
 * Created by laibao
 */
public class DailyRollingAsyncFileLoggerTest {

    public static void main(String[] args) throws IOException, InterruptedException {

        final DailyRollingAsyncFileLogger logger = new DailyRollingAsyncFileLogger("/Users/zhoufeng/logs/" ,"aaa.log", "/Users/zhoufeng/logs" , 10);

        int threadCount = 50 ;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        long start = System.currentTimeMillis();
        for(int t = 1 ; t <= threadCount ; t++){
            Thread thread = new Thread(){
                @Override
                public void run() {
                    for(int i = 0 ; i < 100000 ; i++) {
                        try {
                            Thread.sleep(1);
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
