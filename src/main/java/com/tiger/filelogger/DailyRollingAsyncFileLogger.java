package com.tiger.filelogger;

import com.tiger.util.FileUtil;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 按天滚动实现，自动按天切分、自动清理历史文件
 * Created by laibao
 */
public class DailyRollingAsyncFileLogger extends AbstractAsyncWriterLogger {

    private static Logger LOG = LoggerFactory.getLogger(DailyRollingAsyncFileLogger.class);

    private ReentrantLock writerLock = new ReentrantLock();

    private String fileDirPath ;

    private String baseFileName ;

    private String fileName ;

    private String filePath ;

    private String rollingFileDirName ;

    private File currentFile ;

    private File rollingFileDir ;

    private Writer currentWriter ;

    private Thread checkRollingStatusThread ;

    private long nextRollingTime ;

    private long currentDay ;

    /**
     * 历史日志文件最大保存数量
     */
    private int maxHistoryLogs ;

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public DailyRollingAsyncFileLogger(String fileDirPath , String fileName, String rollingFileDirName , int maxHistoryLogs) throws IOException {
        if(!fileDirPath.endsWith("/")){
            fileDirPath = fileDirPath + "/" ;
        }
        this.fileDirPath = fileDirPath ;
        this.fileName = fileName;
        this.baseFileName = FilenameUtils.getBaseName(fileName);
        this.filePath = this.fileDirPath + this.fileName ;
        this.rollingFileDirName = rollingFileDirName;
        this.maxHistoryLogs = maxHistoryLogs ;
        FileUtil.buildDirIfAbsent(fileDirPath);
        currentFile = FileUtil.buildFileIfAbsent(filePath);
        FileUtil.assertCanWrite(this.currentFile);
        rollingFileDir = FileUtil.buildDirIfAbsent(this.rollingFileDirName);
        FileUtil.assertCanWrite(this.rollingFileDir);
        currentWriter = new FileWriter(this.currentFile , true);
        calcNextRollingTime();
        checkAndDoRolling();
        startCheckRollingStatusThread();
    }

    private void calcNextRollingTime(){
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY , 0);
        today.set(Calendar.MINUTE , 0);
        today.set(Calendar.SECOND , 0);
        //当前时间
        currentDay = today.getTimeInMillis();
        //next day
        today.add(Calendar.HOUR_OF_DAY,24);
        long tomorrowTime = today.getTimeInMillis();
        nextRollingTime = tomorrowTime;
    }

    private void checkAndDoRolling(){
        long currentTime = System.currentTimeMillis() ;
        if(currentTime >= nextRollingTime){
            doRolling();
            calcNextRollingTime();
        }
    }

    private void doRolling(){
        LOG.info("rolling...");
        String formattedResult = dateFormat.format(currentDay);
        //最终名称
        String rollingFileName = fileName.replace(".log" , "-" + formattedResult + ".log");
        super.flush();
        File rollingFile = new File(rollingFileDir , rollingFileName);
        boolean renameResult = currentFile.renameTo(rollingFile);
        if(!renameResult){
            LOG.error("rename rolling file failed!");
        }
        try {
            currentFile = FileUtil.buildFileIfAbsent(filePath);
            writerLock.lock();
            try{
                Writer oldWriter = getWriter() ;
                currentWriter = new FileWriter(currentFile , true);
                oldWriter.close();
            }finally{
                writerLock.unlock();
            }
            LOG.info("rolling success [{}]!" , rollingFile);
        } catch (IOException e) {
            LOG.error(e.getMessage() , e);
        }
        deleteHistoryLogs();
    }

    /**
     * 删除历史日志文件
     */
    private void deleteHistoryLogs(){
        if(rollingFileDir == null){
            return ;
        }
        try {
            final Date currentDate = new Date();
            File[] needDeletedFiles = rollingFileDir.listFiles(new FilenameFilter() {
                Pattern historyFilePattern = Pattern.compile(baseFileName + "-(\\d{4}-\\d{2}-\\d{2})\\.log");
                @Override
                public boolean accept(File dir, String name) {
                    Matcher matcher = historyFilePattern.matcher(name);
                    if (matcher == null || !matcher.find()) {
                        return false;
                    }
                    String dateStr = matcher.group(1);
                    try {
                        Date fileDate = dateFormat.parse(dateStr);
                        fileDate.setDate(fileDate.getDate() + maxHistoryLogs);
                        return fileDate.compareTo(currentDate) < 0;
                    } catch (ParseException e) {
                        LOG.error("删除历史日志文件失败," + e.getMessage(), e);
                        return false;
                    }
                }
            });
            if (needDeletedFiles == null || needDeletedFiles.length <= 0) {
                return;
            }
            for (File needDeletedFile : needDeletedFiles) {
                boolean deleteResult = needDeletedFile.delete();
                if (deleteResult) {
                    LOG.info("删除历史日志文件{}成功", needDeletedFile.getPath());
                } else {
                    LOG.error("删除历史日志文件{}失败", needDeletedFile.getPath());
                }
            }
        }catch (Throwable e){
            LOG.error(e.getMessage() ,e);
        }
    }

    private void startCheckRollingStatusThread(){
        checkRollingStatusThread =  new Thread(){
            @Override
            public void run() {
                while(!isInterrupted()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        break;
                    }
                    checkAndDoRolling();
                }
            }
        };
        checkRollingStatusThread.setDaemon(true);
        checkRollingStatusThread.setName("dealy-rolling-logger-status-check-thread");
        checkRollingStatusThread.start();
    }

    @Override
    protected Writer getWriter() {
        writerLock.lock();
        try{
            return currentWriter;
        }finally{
            writerLock.unlock();
        }
    }

    @Override
    protected void shutdown() {
        super.shutdown();
        if(checkRollingStatusThread != null) {
            checkRollingStatusThread.interrupt();
            try {
                //等待状态线程结束
                checkRollingStatusThread.join();
            } catch (InterruptedException e) {
                LOG.error(e.getMessage(), e);
            }
            if (currentWriter != null) {
                try {
                    currentWriter.close();
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
    }

}
