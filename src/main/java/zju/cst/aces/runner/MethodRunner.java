package zju.cst.aces.runner;

import lombok.Data;
import okhttp3.Response;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import zju.cst.aces.api.PromptConstructor;
import zju.cst.aces.api.Repair;
import zju.cst.aces.api.Validator;
import zju.cst.aces.api.config.Config;
import zju.cst.aces.api.impl.ChatGenerator;
import zju.cst.aces.api.impl.PromptConstructorImpl;
import zju.cst.aces.api.impl.RepairImpl;
import zju.cst.aces.api.impl.obfuscator.Obfuscator;
import zju.cst.aces.dto.*;
import zju.cst.aces.util.CodeExtractor;
import zju.cst.aces.util.TestProcessor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
/**
 * 方法执行器类，用于生成和验证单元测试方法。
 */
public class MethodRunner extends ClassRunner {
    /**
     * 方法信息
     */
    public MethodInfo methodInfo;
    /**
     * 构造方法
     *
     * @param config 配置对象
     * @param fullClassName 完整类名
     * @param methodInfo 方法信息
     * @throws IOException IO异常
     */
    public MethodRunner(Config config, String fullClassName, MethodInfo methodInfo) throws IOException {
        super(config, fullClassName);
        this.methodInfo = methodInfo;
    }
    /**
     * 开始执行测试生成
     *
     * @throws IOException IO异常
     */
    @Override
    public void start() throws IOException {
        if (!config.isStopWhenSuccess() && config.isEnableMultithreading()) {
            ExecutorService executor = Executors.newFixedThreadPool(config.getTestNumber());
            List<Future<String>> futures = new ArrayList<>();
            for (int num = 0; num < config.getTestNumber(); num++) {
                int finalNum = num;
                Callable<String> callable = new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        startRounds(finalNum);
                        return "";
                    }
                };
                Future<String> future = executor.submit(callable);
                futures.add(future);
            }
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    executor.shutdownNow();
                }
            });

            for (Future<String> future : futures) {
                try {
                    String result = future.get();
                    System.out.println(result);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

            executor.shutdown();
        } else {
            for (int num = 0; num < config.getTestNumber(); num++) {
                if (startRounds(num) && config.isStopWhenSuccess()) {
                    break;
                }
            }
        }
    }
    /**
     * 开始执行测试生成的轮次
     *
     * @param num 轮次编号
     * @return 是否生成成功
     * @throws IOException IO异常
     */
    public boolean startRounds(final int num) throws IOException {
        String testName = className + separator + methodInfo.methodName + separator
                + classInfo.methodSigs.get(methodInfo.methodSignature) + separator + num + separator + "Test";
        String fullTestName = fullClassName + separator + methodInfo.methodName + separator
                + classInfo.methodSigs.get(methodInfo.methodSignature) + separator + num + separator + "Test";
        config.getLog().info("\n==========================\n[ChatUniTest] Generating test for method < "
                + methodInfo.methodName + " > number " + num + "...\n");

        ChatGenerator generator = new ChatGenerator(config);
        PromptConstructorImpl pc = new PromptConstructorImpl(config);
        RepairImpl repair = new RepairImpl(config, pc);

        if (methodInfo.dependentMethods.size() > 0) {
            pc.setPromptInfoWithDep(classInfo, methodInfo);
        } else {
            pc.setPromptInfoWithoutDep(classInfo, methodInfo);
        }
        pc.setFullTestName(fullTestName);
        pc.setTestName(testName);

        PromptInfo promptInfo = pc.getPromptInfo();
        promptInfo.setFullTestName(fullTestName);
        Path savePath = config.getTestOutput().resolve(fullTestName.replace(".", File.separator) + ".java");
        promptInfo.setTestPath(savePath);

        for (int rounds = 0; rounds < config.getMaxRounds(); rounds++) {
            if (rounds == 0) {
                config.getLog().info("Generating test for method < " + methodInfo.methodName + " > round " + rounds + " ...");
            } else {
                config.getLog().info("Fixing test for method < " + methodInfo.methodName + " > round " + rounds + " ...");
            }
            promptInfo.addRecord(new RoundRecord(rounds));
            RoundRecord record = promptInfo.getRecords().get(rounds);
            record.setAttempt(num);
            if (generateTest(generator, pc, repair, record)) {
                exportRecord(promptInfo, classInfo, record.getAttempt());
                return true;
            }
        }
        exportRecord(pc.getPromptInfo(), classInfo, num);
        return false;
    }

    /**
     * 生成单元测试
     *
     * @param generator Chat生成器
     * @param pc 提示生成器
     * @param repair 修复器
     * @param record 轮次记录
     * @return 是否生成成功
     * @throws IOException IO异常
     */
    public boolean generateTest(ChatGenerator generator, PromptConstructorImpl pc, RepairImpl repair, RoundRecord record) throws IOException {
        PromptInfo promptInfo = pc.getPromptInfo();
        Obfuscator obfuscator = new Obfuscator(config);
        PromptInfo obfuscatedPromptInfo = new PromptInfo(promptInfo);
        if (config.isEnableObfuscate()) {
            obfuscator.obfuscatePromptInfo(obfuscatedPromptInfo);
        }

        List<Message> prompt = promptGenerator.generateMessages(obfuscatedPromptInfo);
        if (isExceedMaxTokens(config, prompt)) {
            config.getLog().error("Exceed max prompt tokens: " + methodInfo.methodName + " Skipped.");
            return false;
        }
        config.getLog().debug("[Prompt]:\n" + prompt.toString());

        Response response = generator.chat(config, prompt);
        String content = generator.getContentByResponse(response);
        config.getLog().debug("[Response]:\n" + content);
        String code = generator.extractCodeByContent(content);

        record.setPrompt(prompt);
        record.setResponse(content);
        if (code.isEmpty()) {
            config.getLog().info("Test for method < " + methodInfo.methodName + " > extract code failed");
            record.setHasCode(false);
            return false;
        }
        record.setHasCode(true);

        if (config.isEnableObfuscate()) {
            code = obfuscator.deobfuscateJava(code);
        }

        if (CodeExtractor.isTestMethod(code)) {
            TestSkeleton skeleton = new TestSkeleton(promptInfo); // test skeleton to wrap a test method
            code = skeleton.build(code);
        } else {
            code = repair.ruleBasedRepair(code);
        }
        promptInfo.setUnitTest(code);

        record.setCode(code);
        repair.LLMBasedRepair(code, record.getRound());
        if (repair.isSuccess()) {
            record.setHasError(false);
            return true;
        }
        record.setHasError(true);
        record.setErrorMsg(promptInfo.getErrorMsg());
        return false;
    }
    /**
     * 生成单元测试
     *
     * @param generator Chat生成器
     * @param pc 提示生成器
     * @param repair 修复器
     * @return 是否生成成功
     * @throws IOException IO异常
     */
    public boolean generateTest(ChatGenerator generator, PromptConstructorImpl pc, RepairImpl repair) throws IOException {
        PromptInfo promptInfo = pc.getPromptInfo();
        Obfuscator obfuscator = new Obfuscator(config);
        PromptInfo obfuscatedPromptInfo = new PromptInfo(promptInfo);
        if (config.isEnableObfuscate()) {
            obfuscator.obfuscatePromptInfo(obfuscatedPromptInfo);
        }

        List<Message> prompt = promptGenerator.generateMessages(obfuscatedPromptInfo);
        if (isExceedMaxTokens(config, prompt)) {
            config.getLog().error("Exceed max prompt tokens: " + methodInfo.methodName + " Skipped.");
            return false;
        }
        config.getLog().debug("[Prompt]:\n" + prompt.toString());

        Response response = generator.chat(config, prompt);
        String content = generator.getContentByResponse(response);
        String code = generator.extractCodeByContent(content);

        if (code.isEmpty()) {
            config.getLog().info("Test for method < " + methodInfo.methodName + " > extract code failed");
            return false;
        }

        if (config.isEnableObfuscate()) {
            code = obfuscator.deobfuscateJava(code);
        }

        code = repair.ruleBasedRepair(code);
        promptInfo.setUnitTest(code);

        repair.LLMBasedRepair(code);
        return repair.isSuccess();
    }
    /**
     * 运行测试
     *
     * @param config 配置对象
     * @param fullTestName 完整测试名称
     * @param promptInfo 提示信息
     * @param rounds 轮次数
     * @return 是否成功
     */
    public static boolean runTest(Config config, String fullTestName, PromptInfo promptInfo, int rounds) {
        String testName = fullTestName.substring(fullTestName.lastIndexOf(".") + 1);
        Path savePath = config.getTestOutput().resolve(fullTestName.replace(".", File.separator) + ".java");
        if (promptInfo.getTestPath() == null) {
            promptInfo.setTestPath(savePath);
        }

        TestProcessor testProcessor = new TestProcessor(fullTestName);
        String code = promptInfo.getUnitTest();
        if (rounds >= 1) {
            code = testProcessor.addCorrectTest(promptInfo);
        }

        // Compilation
        Path compilationErrorPath = config.getErrorOutput().resolve(testName + "_CompilationError_" + rounds + ".txt");
        Path executionErrorPath = config.getErrorOutput().resolve(testName + "_ExecutionError_" + rounds + ".txt");
        boolean compileResult = config.getValidator().semanticValidate(code, testName, compilationErrorPath, promptInfo);
        if (!compileResult) {
            config.getLog().info("Test for method < " + promptInfo.getMethodInfo().getMethodName() + " > compilation failed round " + rounds);
            return false;
        }
        if (config.isNoExecution()) {
            exportTest(code, savePath);
            config.getLog().info("Test for method < " + promptInfo.getMethodInfo().getMethodName() + " > generated successfully round " + rounds);
            return true;
        }

        // Execution
        TestExecutionSummary summary = config.getValidator().execute(fullTestName);
        if (summary.getTestsSucceededCount() == 0) {
            config.getLog().info("Test for method < " + promptInfo.getMethodInfo().getMethodName() + " > execution failed round " + rounds);
            return false;
        }
        if (summary.getTestsFailedCount() > 0) {
            String testProcessed = testProcessor.removeErrorTest(promptInfo, summary);

            // Remove errors successfully, recompile and re-execute test
            if (testProcessed != null) {
                config.getLog().debug("[Original Test]:\n" + code);
                if (config.getValidator().semanticValidate(testProcessed, testName, compilationErrorPath, null)) {
                    if (config.getValidator().runtimeValidate(fullTestName)) {
                        exportTest(testProcessed, savePath);
                        config.getLog().debug("[Processed Test]:\n" + testProcessed);
                        config.getLog().info("Processed test for method < " + promptInfo.getMethodInfo().getMethodName() + " > generated successfully round " + rounds);
                        return true;
                    }
                }
                testProcessor.removeCorrectTest(promptInfo, summary);
            }

            // Set promptInfo error message
            TestMessage testMessage = new TestMessage();
            List<String> errors = new ArrayList<>();
            summary.getFailures().forEach(failure -> {
                for (StackTraceElement st : failure.getException().getStackTrace()) {
                    if (st.getClassName().contains(fullTestName)) {
                        errors.add("Error in " + failure.getTestIdentifier().getLegacyReportingName()
                                + ": line " + st.getLineNumber() + " : "
                                + failure.getException().toString());
                    }
                }
            });
            testMessage.setErrorType(TestMessage.ErrorType.RUNTIME_ERROR);
            testMessage.setErrorMessage(errors);
            promptInfo.setErrorMsg(testMessage);
            exportError(code, errors, executionErrorPath);
            testProcessor.removeCorrectTest(promptInfo, summary);
            config.getLog().info("Test for method < " + promptInfo.getMethodInfo().getMethodName() + " > execution failed round " + rounds);
            return false;
        }
//            summary.printTo(new PrintWriter(System.out));
        exportTest(code, savePath);
        config.getLog().info("Test for method < " + promptInfo.getMethodInfo().getMethodName() + " > compile and execute successfully round " + rounds);
        return true;
    }


    public static void exportError(String code, List<String> errors, Path outputPath) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath.toFile()));
            writer.write(code);
            writer.write("\n--------------------------------------------\n");
            writer.write(errors.stream().collect(Collectors.joining("\n")));
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException("In TestCompiler.exportError: " + e);
        }
    }
}