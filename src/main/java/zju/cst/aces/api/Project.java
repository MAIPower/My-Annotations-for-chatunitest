package zju.cst.aces.api;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * 项目接口定义了项目的基本信息和属性。
 */
public interface Project {

    /**
     * 获取项目的父项目。
     *
     * @return 父项目对象
     */
    Project getParent();

    /**
     * 获取项目的根目录。
     *
     * @return 项目的根目录
     */
    File getBasedir();

    /**
     * 获取项目的打包类型。
     *
     * @return 项目的打包类型
     */
    String getPackaging();

    /**
     * 获取项目的 Group Id。
     *
     * @return 项目的 Group Id
     */
    String getGroupId();

    /**
     * 获取项目的 Artifact Id。
     *
     * @return 项目的 Artifact Id
     */
    String getArtifactId();

    /**
     * 获取项目的编译源路径列表。
     *
     * @return 编译源路径列表
     */
    List<String> getCompileSourceRoots();

    /**
     * 获取项目的构建产物路径。
     *
     * @return 构建产物路径
     */
    Path getArtifactPath();

    /**
     * 获取项目的构建路径。
     *
     * @return 构建路径
     */
    Path getBuildPath();
}
