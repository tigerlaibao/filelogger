### 本地文件记录器

`core`

* `com.tiger.filelogger.SimpleAsyncFileLogger` 基本实现
* `com.tiger.filelogger.DailyRollingAsyncFileLogger` 按天滚动、自动清理历史记录实现

`test`

* `com.tiger.filelogger.test.SimpleAsycnLocalFileLoggerTest` 对基本异步实现进行测试
* `com.tiger.filelogger.test.DailyRollingAsyncFileLoggerTest` 对滚动实现进行测试
* `com.tiger.filelogger.test.LogbackTest` 对logback实现进行测试

MacPro [2.7 GHz Intel Core i5] [16GB] 环境下的测试结果

50条线程并行，每条线程写入10W条日志，共500W条日志， 最终文件100M左右

测试结果：

|实现|耗时|
|---|---|
|SimpleAsyncFileLogger|4s|
|Logback|46s|   
       