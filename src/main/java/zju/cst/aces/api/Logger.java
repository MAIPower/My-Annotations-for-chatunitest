package zju.cst.aces.api;

/**
 * 日志记录器接口，用于记录各种级别的日志信息
 */
public interface Logger {

    /**
     * 记录信息级别的日志消息
     *
     * @param msg 要记录的信息
     */
    void info(String msg);

    /**
     * 记录警告级别的日志消息
     *
     * @param msg 要记录的警告信息
     */
    void warn(String msg);

    /**
     * 记录错误级别的日志消息
     *
     * @param msg 要记录的错误信息
     */
    void error(String msg);

    /**
     * 记录调试级别的日志消息
     *
     * @param msg 要记录的调试信息
     */
    void debug(String msg);
}
