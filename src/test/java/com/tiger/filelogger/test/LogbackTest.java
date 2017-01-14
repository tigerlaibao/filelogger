package com.tiger.filelogger.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * 使用logback测试性能
 * Created by laibao
 */
public class LogbackTest {

    public static void main(String[] args)  throws Exception{

        final Logger LOG = LoggerFactory.getLogger(LogbackTest.class);


        int threadCount = 50;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        long start = System.currentTimeMillis();
        for (int t = 1; t <= threadCount; t++) {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    for (int i = 0; i < 100000; i++) {
                        try {
                            LOG.info(Thread.currentThread().getName() + ",hahaha," + i);
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
        System.out.println(end - start);

    }

}
