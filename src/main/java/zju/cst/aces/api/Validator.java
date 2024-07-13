package zju.cst.aces.api;

import org.junit.platform.launcher.listeners.TestExecutionSummary;
import zju.cst.aces.dto.PromptInfo;

import java.nio.file.Path;

/**
 * Validator接口定义了代码验证相关的方法。
 */
public interface Validator {

    /**
     * 语法验证方法。
     *
     * @param code 待验证的代码
     * @return 是否通过语法验证
     */
    boolean syntacticValidate(String code);

    /**
     * 语义验证方法。
     *
     * @param code       待验证的代码
     * @param className  类名
     * @param outputPath 输出路径
     * @param promptInfo 提示信息对象
     * @return 是否通过语义验证
     */
    boolean semanticValidate(String code, String className, Path outputPath, PromptInfo promptInfo);

    /**
     * 运行时验证方法。
     *
     * @param fullTestName 完整的测试名称
     * @return 是否通过运行时验证
     */
    boolean runtimeValidate(String fullTestName);

    /**
     * 编译方法。
     *
     * @param className  类名
     * @param outputPath 输出路径
     * @param promptInfo 提示信息对象
     * @return 是否编译成功
     */
    boolean compile(String className, Path outputPath, PromptInfo promptInfo);

    /**
     * 执行测试方法。
     *
     * @param fullTestName 完整的测试名称
     * @return 测试执行摘要信息
     */
    TestExecutionSummary execute(String fullTestName);
}
