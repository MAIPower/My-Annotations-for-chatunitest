package zju.cst.aces.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import zju.cst.aces.api.Project;
import zju.cst.aces.api.config.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ProjectParser {

    public static final JavaParser parser = new JavaParser();
    public Path srcFolderPath;
    public Path outputPath;
    public Map<String, Set<String>> classMap = new HashMap<>();
    public static Config config;
    public int classCount = 0;
    public int methodCount = 0;

    public ProjectParser(Config config) {
        this.srcFolderPath = Paths.get(config.getProject().getBasedir().getAbsolutePath(), "src", "main", "java");
        this.config = config;
        this.outputPath = config.getParseOutput();
        JavaSymbolSolver symbolSolver = getSymbolSolver();
        parser.getParserConfiguration().setSymbolResolver(symbolSolver);
        setLanguageLevel(parser.getParserConfiguration());
        if (config.parser == null) {
            config.setParser(parser);
        }
    }

    /**
     * Parse the project.
     */
    public void parse() {
        List<String> classPaths = scanSourceDirectory(config.getProject());
        if (classPaths.isEmpty()) {
            config.getLog().warn("No java file found in " + srcFolderPath);
            return;
        }
        for (String classPath : classPaths) {
            try {
                String packagePath = classPath.substring(srcFolderPath.toString().length() + 1);
                Path output = outputPath.resolve(packagePath).getParent();
                ClassParser classParser = new ClassParser(config, output);
                int classNum = classParser.extractClass(classPath);

                if (classNum == 0) {
                    continue;
                }
                addClassMap(outputPath, packagePath);
                classCount += classNum;
                methodCount += classParser.methodCount;
            } catch (Exception e) {
                throw new RuntimeException("In ProjectParser.parse: " + e);
            }
        }
        exportClassMapping();
        exportJson(config.getClassNameMapPath(), classMap);
        config.getLog().info("\nParsed classes: " + classCount + "\nParsed methods: " + methodCount);
    }

    public void addClassMap(Path outputPath, String packagePath) {
        if (Paths.get(packagePath).getParent() == null) {
            return;
        }
        Path path = outputPath.resolve(packagePath).getParent();
        String packageDeclaration = path.toString().substring(outputPath.toString().length() + 1).replace(File.separator, ".");
        // list the directories in the path
        File[] files = path.toFile().listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                String className = file.getName();
                String fullClassName = packageDeclaration + "." + className;
                if (classMap.containsKey(className)) {
                    classMap.get(className).add(fullClassName);
                } else {
                    Set<String> fullClassNames = new HashSet<>();
                    fullClassNames.add(fullClassName);
                    classMap.put(className, fullClassNames);
                }
            }
        }
    }

    public static void exportJson(Path path, Object obj) {
        if (!Files.exists(path.getParent())) {
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(path.toFile()), StandardCharsets.UTF_8)){
            writer.write(config.getGSON().toJson(obj));
        } catch (Exception e) {
            throw new RuntimeException("In ProjectParser.exportJson: " + e);
        }
    }

    public static List<String> scanSourceDirectory(Project project) {
        List<String> classPaths = new ArrayList<>();
        File[] files = Paths.get(project.getCompileSourceRoots().get(0)).toFile().listFiles();
        if (files != null) {
            for (File file : files) {
                try {
                    Files.walk(file.toPath()).forEach(path -> {
                        if (path.toString().endsWith(".java")) {
                            classPaths.add(path.toString());
                        }
                    });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return classPaths;
    }

    public JavaSymbolSolver getSymbolSolver() {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        for (String dep : config.getClassPaths()) {
            try {
                File depFile = new File(dep);
                if (!depFile.exists() || !dep.endsWith("jar")) {
                    continue;
                }
                combinedTypeSolver.add(new JarTypeSolver(depFile));
            } catch (Exception e) {
                config.getLog().warn(e.getMessage());
                config.getLog().debug(e.getMessage());
            }
        }
        for (String src : config.getProject().getCompileSourceRoots()) { // TODO: remove MavenProject
            if (new File(src).exists()) {
                combinedTypeSolver.add(new JavaParserTypeSolver(src));
            }
        }
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        config.setParserFacade(JavaParserFacade.get(combinedTypeSolver));
        return symbolSolver;
    }

    public static void walkDep(DependencyNode node, Set<DependencyNode> depSet) {
        depSet.add(node);
        for (DependencyNode dep : node.getChildren()) {
            walkDep(dep, depSet);
        }
    }

    public void exportClassMapping() {
        Path savePath = config.tmpOutput.resolve("classMapping.json");
        exportJson(savePath, config.classMapping);
    }

    private void setLanguageLevel(ParserConfiguration configuration) {
        int version = Runtime.version().feature();
//        int versionPrefix = Integer.parseInt(System.getProperty("java.version").split("\\.")[0]);
        switch (version) {
            case 8: // java 8
                configuration.setLanguageLevel(LanguageLevel.JAVA_8);
                break;
            case 9:
                configuration.setLanguageLevel(LanguageLevel.JAVA_9);
                break;
            case 10:
                configuration.setLanguageLevel(LanguageLevel.JAVA_10);
                break;
            case 11:
                configuration.setLanguageLevel(LanguageLevel.JAVA_11);
                break;
            case 12:
                configuration.setLanguageLevel(LanguageLevel.JAVA_12);
                break;
            case 13:
                configuration.setLanguageLevel(LanguageLevel.JAVA_13);
                break;
            case 14:
                configuration.setLanguageLevel(LanguageLevel.JAVA_14);
                break;
            case 15:
                configuration.setLanguageLevel(LanguageLevel.JAVA_15);
                break;
            case 16:
                configuration.setLanguageLevel(LanguageLevel.JAVA_16);
                break;
            default:
                configuration.setLanguageLevel(LanguageLevel.JAVA_17);
                break;
        }
    }
}
