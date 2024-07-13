# :mega: ChatUnitest Core

[English](./README.md) | [中文](./Readme_zh.md)

## 动机
相信很多人试过用ChatGPT帮助自己完成各种各样的编程任务，并且已经取得了不错的效果。但是，直接使用ChatGPT存在一些问题： 一是生成的代码很多时候不能正常执行，**“编程五分钟，调试两小时”**； 二是不方便跟现有工程进行集成，需要手动与ChatGPT进行交互，并且在不同页面间切换。为了解决这些问题，我们提出了 **“生成-验证-修复”** 框架，并实现了原型系统，同时为了方便大家使用，我们开发了一些插件，能够方便的集成到已有开发流程中。已完成Maven插件 开发，最新版1.4.1已发布到Maven中心仓库，欢迎试用和反馈。IDEA插件正在开发中，欢迎持续关注。

## 概览

![概览](docs/img/overview.jpg)

## 运行步骤

### 0. `pom.xml`文件配置
```xml
<dependency>
    <groupId>io.github.ZJU-ACES-ISE</groupId>
    <artifactId>chatunitest-core</artifactId>
    <version>1.0.2</version>
</dependency>
```

### 1. 将以下依赖项添加到`pom.xml`文件中
```xml
<dependency>
    <groupId>io.github.ZJU-ACES-ISE</groupId>
    <artifactId>chatunitest-starter</artifactId>
    <version>1.4.0</version>
    <type>pom</type>
</dependency>
```

## :email: 联系我们

如果您有任何问题，请随时通过电子邮件与我们联系，联系方式如下：

1. Corresponding author: `zjuzhichen AT zju.edu.cn`
2. Author: `yh_ch AT zju.edu.cn`, `xiezhuokui AT zju.edu.cn`









