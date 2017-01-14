package com.tiger.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * 文件操作工具
 * Created by laibao
 */
public class FileUtil {

    private static Logger LOG = LoggerFactory.getLogger(FileUtil.class);

    public static File buildFileIfAbsent(String filePath) throws IOException {
        LOG.info("buildFileIfAbsent:{}" , filePath);
        File file = new File(filePath);
        if(!file.exists()){
            LOG.info("file[{}], not exists" ,file.getPath() );
            File dir = file.getParentFile();
            LOG.info("file dir:[{}]" , dir );
            if(!dir.exists()){
                boolean createDir ;
                try {
                    dir.setReadable(true,false);
                    createDir = dir.mkdirs();
                    LOG.info("mkdir [{}] result:{}" , dir ,createDir );
                }catch (Exception e){
                    throw new IOException("can not create dir " + dir.getPath() , e);
                }
                if(!createDir){
                    throw new IOException("create dir " + dir.getPath() + " failed");
                }
            }
            boolean createFile ;
            try {
                file.setReadable(true,true);
                createFile = file.createNewFile();
            }catch (Exception e){
                throw new IOException("can not create file " + file.getPath() , e);
            }
            if(!createFile){
                throw new IOException("create file " + file.getPath() + " failed");
            }
        }
        if(!file.isFile()){
            throw new IOException(file.getPath() +" is not a file !");
        }
        return file ;
    }

    public static File buildDirIfAbsent(String dirPath) throws IOException {
        File dir = new File(dirPath);
        if(!dir.exists()){
            boolean createDir ;
            try {
                dir.setReadable(true,false);
                createDir = dir.mkdirs();
            }catch (Exception e){
                throw new IOException("can not create dir " + dir.getPath() , e);
            }
            if(!createDir){
                throw new IOException("create dir " + dir.getPath() + " failed");
            }
        }
        return dir ;
    }

    public static void assertCanRead(String path){
        File file = new File(path);
        if(!file.canRead()){
            throw new RuntimeException("file [" + path + "] can not be read!" );
        }
    }

    public static void assertCanWrite(String path){
        File file = new File(path);
        if(!file.canWrite()){
            throw new RuntimeException("file [" + path + "] can not be read!" );
        }
    }


    public static void assertCanRead(File file){
        if(!file.canRead()){
            throw new RuntimeException("file [" + file.getPath() + "] can not be read!" );
        }
    }

    public static void assertCanWrite(File file){
        if(!file.canWrite()){
            throw new RuntimeException("file [" + file.getPath() + "] can not be read!" );
        }
    }



}
