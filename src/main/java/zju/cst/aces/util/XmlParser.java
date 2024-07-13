package zju.cst.aces.util;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.maven.shared.utils.StringUtils;
import org.objectweb.asm.Type;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
/**
 * XML解析器类，用于从Jacoco生成的XML报告中提取覆盖率信息。
 */
public class XmlParser {
    /**
     * 从指定的XML文件中获取指定类和方法的覆盖率信息。
     *
     * @param xmlFilePath XML文件路径
     * @param className 类名
     * @param methodName 方法名
     * @param methodSignature 方法签名
     * @return 覆盖率信息列表
     */
    public List<CoverageInfo> getCoverageInfo(String xmlFilePath, String className, String methodName, String methodSignature) {
        //设置返回值为json类型
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode resultNode = objectMapper.createObjectNode();
        ArrayNode coverageArray=objectMapper.createArrayNode();

        List<CoverageInfo> coverageInfoList=new ArrayList<>();

        String result="";
        try {
            // 创建DOM解析器工厂
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false); // 禁用DTD验证
            DocumentBuilder builder = factory.newDocumentBuilder();

            // 解析XML文件
            Document document = builder.parse(new File(xmlFilePath));
            // 查找对应的方法信息
            NodeList packageNodes = document.getElementsByTagName("package");
            for (int i = 0; i < packageNodes.getLength(); i++) {
                Element packageElement = (Element) packageNodes.item(i);
                    NodeList classNodes = packageElement.getElementsByTagName("class");
                    for (int j = 0; j < classNodes.getLength(); j++) {
                        Element classElement = (Element) classNodes.item(j);
                        if (classElement.getAttribute("name").equals(className)) {
                            NodeList methodNodes = classElement.getElementsByTagName("method");
                            for (int k = 0; k < methodNodes.getLength(); k++) {
                                Element methodElement = (Element) methodNodes.item(k);
                                String methodNameAttr = methodElement.getAttribute("name");
                                String methodDescAttr = methodElement.getAttribute("desc");
                                if (methodName.equals(methodNameAttr) && (methodNameAttr+parseMethodDescriptor(methodDescAttr)).replaceAll(" ","").equals(methodSignature.replaceAll(" ",""))) {
                                    // 找到了对应的方法信息
                                    NodeList counterNodes = methodElement.getElementsByTagName("counter");
                                    for (int l = 0; l < counterNodes.getLength(); l++) {
                                        Element counterElement = (Element) counterNodes.item(l);
                                        String type = counterElement.getAttribute("type");
                                        String missed = counterElement.getAttribute("missed");
                                        String covered = counterElement.getAttribute("covered");
                                        ObjectNode coverageNode = objectMapper.createObjectNode();
                                        coverageNode.put("type", type);
                                        coverageNode.put("missed", Integer.parseInt(missed));
                                        coverageNode.put("covered", Integer.parseInt(covered));
                                        coverageArray.add(coverageNode);
                                        CoverageInfo coverageInfo = new CoverageInfo(type, Integer.parseInt(missed), Integer.parseInt(covered));
                                        coverageInfoList.add(coverageInfo);
                                        result=result.concat ("<type=\"" + type + "\" missed=\"" + missed + "\" covered=\"" + covered + "\"/>"+"\n");
                                    }
                                }
                            }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        return result.equals("")?"Failure":result;
        return coverageInfoList;
    }
    /**
     * 解析方法描述符，返回方法参数的类型字符串。
     *
     * @param descriptor 方法描述符
     * @return 方法参数类型字符串
     */
    public static String parseMethodDescriptor(String descriptor) {
        Type[] argumentTypes = Type.getArgumentTypes(descriptor);
        StringBuilder result = new StringBuilder("(");
        for (int i = 0; i < argumentTypes.length; i++) {
            Type argumentType = argumentTypes[i];
            String typeName = argumentType.getClassName();
            int lastDotIndex = typeName.lastIndexOf('.');
            if (lastDotIndex != -1) {
                typeName = typeName.substring(lastDotIndex + 1);
            }
            result.append(typeName);

            if (i < argumentTypes.length - 1) {
                result.append(", ");
            }
        }

        result.append(")");
        return result.toString();
    }


    /**
     * 覆盖率信息类，用于存储单个方法的覆盖率数据。
     */
    public class CoverageInfo{
        /**
         * 覆盖率类型
         */
        private String type;
        /**
         * 未覆盖的数量
         */
        private Integer missed;
        /**
         * 覆盖的数量
         */
        private Integer covered;
        /**
         * 构造方法
         *
         * @param type 覆盖率类型
         * @param missed 未覆盖的数量
         * @param covered 覆盖的数量
         */
        public CoverageInfo(String type, Integer missed, Integer covered) {
            this.type = type;
            this.missed = missed;
            this.covered = covered;
        }

        public CoverageInfo() {
        }
        /**
         * 获取覆盖率类型
         *
         * @return 覆盖率类型
         */
        public String getType() {
            return type;
        }
        /**
         * 设置覆盖率类型
         *
         * @param type 覆盖率类型
         */
        public void setType(String type) {
            this.type = type;
        }

        public Integer getMissed() {
            return missed;
        }
        /**
         * 设置未覆盖的数量
         *
         * @param missed 未覆盖的数量
         */
        public void setMissed(Integer missed) {
            this.missed = missed;
        }

        public Integer getCovered() {
            return covered;
        }

        public void setCovered(Integer covered) {
            this.covered = covered;
        }

        @Override
        public String toString() {
            return "CoverageInfo{" +
                    "type='" + type + '\'' +
                    ", missed=" + missed +
                    ", covered=" + covered +
                    '}';
        }
    }

    public static void main(String[] args) {
        // 指定XML文件路径
        String xmlFilePath = "D:\\idea_plugin\\test_plugin\\target\\site\\jacoco\\jacoco.xml";

        // 指定要查找的信息
        String className = "com/hhh/plugin/MyTest1";
        String methodName = "sayHello3";
        String methodSignature = "sayHello3()";
        XmlParser xmlParser = new XmlParser();
        List<CoverageInfo> coverageInfoList = xmlParser.getCoverageInfo(xmlFilePath, className, methodName, methodSignature);
        for (CoverageInfo coverageInfo : coverageInfoList) {
            System.out.println(coverageInfo.toString());
        }
    }
}
