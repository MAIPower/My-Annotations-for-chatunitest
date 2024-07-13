package zju.cst.aces.api;

import zju.cst.aces.api.config.Config;
import zju.cst.aces.dto.Message;

import java.util.List;

/**
 * 生成器接口，用于生成字符串内容
 */
public interface Generator {

    /**
     * 根据消息列表生成字符串内容
     *
     * @param messages 消息列表
     * @return 生成的字符串内容
     */
    String generate(List<Message> messages);

}
