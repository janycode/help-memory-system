import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.mynewwork.model.entity.Environment;

import java.time.LocalDateTime;

/**
 * 测试环境实体序列化
 */
public class TestEnvironmentSerialization {

    public static void main(String[] args) throws JsonProcessingException {
        // 创建测试环境对象
        Environment env = new Environment();
        env.setId(1L);
        env.setName("测试环境");
        env.setType("DEV");
        env.setUrl("https://test.example.com");
        env.setUsername("testuser");
        env.setPassword("testpass");
        env.setDescription("测试环境描述");
        env.setCreatedAt(LocalDateTime.now());
        env.setUpdatedAt(LocalDateTime.now());

        // 使用默认 ObjectMapper 序列化
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(env);

        System.out.println("序列化结果:");
        System.out.println(json);

        // 检查是否包含 url 字段
        if (json.contains(""url"