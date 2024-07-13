package zju.cst.aces.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import zju.cst.aces.api.config.Config;
import zju.cst.aces.dto.ClassInfo;
import zju.cst.aces.dto.MethodInfo;
import zju.cst.aces.parser.ClassParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * {@code Counter} 类用于统计指定路径下的类和方法信息。
 */
public class Counter {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /**
     * 主方法，执行类方法统计。
     *
     * @param args 命令行参数
     * @throws IOException 如果发生IO错误
     */
    public static void main(String[] args) throws IOException {
        countClassMethod(Paths.get("/private/tmp/chatunitest-info/chatgpt-spring-boot-starter/class-info"));
    }

    /**
     * 统计指定路径下的类和方法信息。
     *
     * @param parseOutputPath 解析输出路径
     * @return 包含类名和方法列表的映射
     * @throws IOException 如果发生IO错误
     */
    public static Map<String, List<String>> countClassMethod(Path parseOutputPath) throws IOException {
        Map<String, List<String>> testMap = new HashMap<>();
        // 获取所有名为 "class.json" 的 JSON 文件
        List<String> classJsonFiles = Files.walk(parseOutputPath)
                .filter(Files::isRegularFile)
                .map(Path::toString)
                .filter(f -> f.endsWith("class.json"))
                .collect(Collectors.toList());

        for (String classJsonFile : classJsonFiles) {
            File classInfoFile = new File(classJsonFile);
            ClassInfo classInfo = GSON.fromJson(Files.readString(classInfoFile.toPath(), StandardCharsets.UTF_8), ClassInfo.class);

            if (!filter(classInfo)) {
                continue;
            }
            List<String> methodList = new ArrayList<>();
            for (String mSig : classInfo.methodSigs.keySet()) {
                MethodInfo methodInfo = getMethodInfo(parseOutputPath, classInfo, mSig);
                if (!filter(methodInfo)) {
                    continue;
                }
                methodList.add(mSig);
            }
            testMap.put(classInfo.fullClassName, methodList);
        }

        // 打印 testMap
        for (String className : testMap.keySet()) {
            System.out.println("-----------------------");
            System.out.println(className + ":\n");
            testMap.get(className).forEach(m -> {
                System.out.println(m + "\n");
            });
            System.out.println("\n");
        }

        System.out.println("Total class count: " + testMap.size());
        System.out.println("Total method count: " + testMap.values().stream().mapToInt(List::size).sum());
        return testMap;
    }

    /**
     * 获取指定类和方法签名的 {@code MethodInfo} 对象。
     *
     * @param parseOutputPath 解析输出路径
     * @param info            类信息
     * @param mSig            方法签名
     * @return 方法信息对象，如果文件不存在则返回 {@code null}
     * @throws IOException 如果发生IO错误
     */
    public static MethodInfo getMethodInfo(Path parseOutputPath, ClassInfo info, String mSig) throws IOException {
        String packagePath = info.getPackageName()
                .replace("package ", "")
                .replace(".", File.separator)
                .replace(";", "");
        Path depMethodInfoPath = parseOutputPath
                .resolve(packagePath)
                .resolve(info.className)
                .resolve(ClassParser.getFilePathBySig(mSig, info));
        if (!depMethodInfoPath.toFile().exists()) {
            return null;
        }
        return GSON.fromJson(Files.readString(depMethodInfoPath, StandardCharsets.UTF_8), MethodInfo.class);
    }

    /**
     * 过滤不符合条件的类信息。
     *
     * @param classInfo 类信息对象
     * @return 如果类信息符合条件则返回 {@code true}，否则返回 {@code false}
     */
    public static boolean filter(ClassInfo classInfo) {
        if (!classInfo.isPublic || classInfo.isAbstract || classInfo.isInterface) {
            return false;
        }
        return true;
    }

    /**
     * 过滤不符合条件的方法信息。
     *
     * @param methodInfo 方法信息对象
     * @return 如果方法信息符合条件则返回 {@code true}，否则返回 {@code false}
     */
    public static boolean filter(MethodInfo methodInfo) {
        if (methodInfo == null
                || methodInfo.isConstructor || methodInfo.isGetSet || methodInfo.isBoolean || !methodInfo.isPublic) {
            return false;
        }
        return true;
    }
}
