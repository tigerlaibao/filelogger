package com.tiger.filelogger;

import com.tiger.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * 本地文件记录器基本实现
 * Created by laibao
 */
public class SimpleAsyncFileLogger extends AbstractAsyncWriterLogger {

    private static Logger LOG = LoggerFactory.getLogger(SimpleAsyncFileLogger.class);

    private String logFilePath ;

    private Writer writer ;

    private File file ;


    public SimpleAsyncFileLogger(String logFilePath, boolean append) throws IOException {
        this.logFilePath = logFilePath;
        file = FileUtil.buildFileIfAbsent(logFilePath);
        writer = new FileWriter(file , append);
    }

    @Override
    protected Writer getWriter() {
        return writer;
    }

    @Override
    protected void shutdown() {
        super.shutdown();
        if(writer != null){
            try {
                writer.close();
            } catch (IOException e) {
                LOG.error(e.getMessage() , e);
            }
        }
    }
}
