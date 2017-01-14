package com.tiger.filelogger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 抽象异步文件记录器基础接口
 * Created by laibao
 */
public abstract class AbstractAsyncWriterLogger {

    private static Logger LOG = LoggerFactory.getLogger(AbstractAsyncWriterLogger.class);

    /**
     * 最多缓冲消息数量
     */
    private static final int MAX_CACHE_MESSAGE_SIZE = 1000 ;

    /**
     * 每{n(ms)}flush一次
     */
    private static final int MAX_WRITE_IDEL_MILLISECOND = 1000 ;

    /**
     * 最后写入文件的时间
     */
    private volatile long lastWriteTime = System.currentTimeMillis() ;

    /**
     * 每条消息分隔符
     */
    private static final String MESSAGE_SEPARATOR = "\n" ;

    private Object flushFileLock = new Object();

    /**
     * 缓冲消息队列
     */
    private Queue<String> cachedMessageQueue = new ConcurrentLinkedQueue();

    private Thread checkIdelThread ;

    private volatile boolean stopping = false ;


    public AbstractAsyncWriterLogger() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                AbstractAsyncWriterLogger.this.shutdown();
            }
        });
        startCheckIdelThread();
    }

    protected String beforeLog(String message){
        // 子类扩展
        return message ;
    }

    protected void afterLog(String message){
        // 子类扩展
    }

    public void log(String message) throws IOException{
        if(stopping){
            throw new IOException("logger is stopping...");
        }
        message = beforeLog(message);
        cachedMessageQueue.add(message);
        afterLog(message);
    }

    protected void flush(){
        lastWriteTime = System.currentTimeMillis();
        if(cachedMessageQueue.isEmpty()){
            return ;
        }
        Queue<String> newCachedMessageQueue = new ConcurrentLinkedQueue<String>();
        Queue<String> currentCachedMessageQueue = cachedMessageQueue ;
        cachedMessageQueue = newCachedMessageQueue ;
        doFlush(currentCachedMessageQueue);
    }

    /**
     * 将缓冲区的消息写入文件
     * @throws IOException
     */
    private void doFlush(Queue<String> messageQueue){
        if(messageQueue == null || messageQueue.size() <= 0){
            return ;
        }
        Iterator<String> messageIter = messageQueue.iterator();
        StringBuilder mergedMessage = new StringBuilder();
        int messageCount = 0 ;
        while(messageIter.hasNext()){
            mergedMessage.append(messageIter.next()).append(MESSAGE_SEPARATOR);
            messageCount++ ;
            if(messageCount == 500){
                synchronized (flushFileLock) {
                    try {
                        Writer writer = getWriter() ;
                        writer.write(mergedMessage.toString()) ;
                        writer.flush() ;
                    } catch (IOException e) {
                        LOG.error(e.getMessage() , e);
                    }
                }
                messageCount = 0 ;
                mergedMessage = new StringBuilder();
            }
        }
        if(messageCount > 0) {
            synchronized (flushFileLock) {
                try {
                    Writer writer = getWriter() ;
                    writer.write(mergedMessage.toString());
                    writer.flush();
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
        LOG.debug("write log {} to file success!", messageQueue.hashCode() + "," + messageQueue.size());
    }

    /**
     * 定时刷新缓冲区消息到文件线程
     */
    private void startCheckIdelThread(){
        checkIdelThread = new Thread(){
            @Override
            public void run() {
                LOG.info("asynclog-checkidel-thread start...");
                while(!stopping){
                    //如果缓冲区消息超过指定时间没写入过  则写入
                    boolean needFlush = (cachedMessageQueue.size() >= MAX_CACHE_MESSAGE_SIZE)  || (AbstractAsyncWriterLogger.this.lastWriteTime != -1L &&
                            ( System.currentTimeMillis() - AbstractAsyncWriterLogger.this.lastWriteTime > MAX_WRITE_IDEL_MILLISECOND));
                    if(needFlush){
                        LOG.debug("[asynclog-checkidel-thread] putCurrentQueueToPenddingQueue..");
                        AbstractAsyncWriterLogger.this.flush();
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        continue ;
                    }
                }
                LOG.info("asynclog-checkidel-thread stoped");
            }
        };
        checkIdelThread.setName("asynclog-checkidel-thread");
        checkIdelThread.setDaemon(true);
        checkIdelThread.start();
    }

    protected void shutdown(){
        stopping = true ;
        if(checkIdelThread != null) {
            checkIdelThread.interrupt();
        }
        try {
            //等待任务线程执行完
            checkIdelThread.join();
            //将缓冲区剩余消息刷入文件
            flush();
        } catch (InterruptedException e) {
            LOG.error(e.getMessage() , e);
        }
    }

    protected abstract Writer getWriter();



}
