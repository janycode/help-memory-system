import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 检查是否有敏感字段过滤配置
 */
public class CheckSensitiveFieldFilter {

    public static void main(String[] args) throws Exception {
        // 检查所有 Java 文件，查找敏感字段过滤逻辑
        File backendDir = new File("./backend/src/main/java");

        System.out.println("=== 检查敏感字段过滤配置 ===");

        // 搜索包含敏感关键词的文件
        searchFiles(backendDir, "password");
        searchFiles(backendDir, "username");
        searchFiles(backendDir, "url");
        searchFiles(backendDir, "sensitive");
        searchFiles(backendDir, "filter");
        searchFiles(backendDir, "mask");
        searchFiles(backendDir, "hide");

        System.out.println("\n=== 检查 application.yml 配置 ===");
        String yamlContent = Files.readString(Paths.get("./backend/src/main/resources/application.yml"));
        if (yamlContent.contains("sensitive-fields")) {
            System.out.println("发现敏感字段配置: sensitive-fields");
            // 提取配置行
            String[] lines = yamlContent.split("\n");
            for (String line : lines) {
                if (line.contains("sensitive-fields")) {
                    System.out.println("配置行: " + line.trim());
                }
            }
        }
    }

    private static void searchFiles(File dir, String keyword) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    searchFiles(file, keyword);
                } else if (file.getName().endsWith(".java")) {
                    try {
                        List<String> lines = Files.readAllLines(file.toPath());
                        for (int i = 0; i < lines.size(); i++) {
                            String line = lines.get(i);
                            if (line.toLowerCase().contains(keyword.toLowerCase()) &&
                                (line.contains("filter") || line.contains("mask") || line.contains("hide") ||
                                 line.contains("ignore") || line.contains("sensitive"))) {
                                System.out.println("文件: " + file.getPath() + " 第" + (i+1) + "行: " + line.trim());
                            }
                        }
                    } catch (Exception e) {
                        // 忽略读取错误
                    }
                }
            }
        }
    }
}