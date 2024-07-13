package zju.cst.aces.api;

import zju.cst.aces.dto.Message;

import java.util.List;

/**
 * 提示构造器接口定义了生成消息列表的方法。
 */
public interface PromptConstructor {

    /**
     * 生成消息列表。
     *
     * @return 生成的消息列表
     */
    List<Message> generate();

}
