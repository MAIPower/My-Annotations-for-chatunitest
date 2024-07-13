package zju.cst.aces.api;

import java.io.IOException;

/**
 * 修复接口定义了基于规则和基于LLM的修复方法。
 */
public interface Repair {

    /**
     * 基于规则的修复方法。
     *
     * @param code 待修复的代码
     * @return 修复后的代码
     */
    String ruleBasedRepair(String code);

    /**
     * 基于LLM的修复方法。
     *
     * @param code 待修复的代码
     * @return 修复后的代码
     */
    String LLMBasedRepair(String code);

    /**
     * 基于LLM的修复方法，指定修复轮数。
     *
     * @param code   待修复的代码
     * @param rounds 修复的轮数
     * @return 修复后的代码
     */
    String LLMBasedRepair(String code, int rounds);
}
