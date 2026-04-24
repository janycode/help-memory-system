package com.example.mynewwork.config;

import com.example.mynewwork.model.entity.SysDictData;
import com.example.mynewwork.model.entity.SysDictType;
import com.example.mynewwork.model.entity.SystemConfig;
import com.example.mynewwork.model.entity.User;
import com.example.mynewwork.repository.SysDictDataRepository;
import com.example.mynewwork.repository.SysDictTypeRepository;
import com.example.mynewwork.repository.SystemConfigRepository;
import com.example.mynewwork.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SysDictTypeRepository dictTypeRepository;
    private final SysDictDataRepository dictDataRepository;
    private final SystemConfigRepository configRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initAdminUser();
        initDictData();
        initSystemConfig();
    }

    private void initAdminUser() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@example.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("系统管理员");
            admin.setDepartment("技术部");
            admin.setPosition("管理员");
            admin.setRoles(java.util.Set.of("ADMIN"));
            admin.setEnabled(true);
            admin.setCreatedAt(LocalDateTime.now());
            admin.setUpdatedAt(LocalDateTime.now());
            userRepository.save(admin);
            log.info("初始化管理员用户: admin/admin123");
        } else {
            User existing = userRepository.findByUsername("admin").orElse(null);
            if (existing != null && !passwordEncoder.matches("admin123", existing.getPassword())) {
                existing.setPassword(passwordEncoder.encode("admin123"));
                existing.setUpdatedAt(LocalDateTime.now());
                userRepository.save(existing);
                log.info("重置管理员用户密码: admin/admin123");
            }
        }
    }

    private void initDictData() {
        initDictType("component_category", "组件分类", "技术组件的分类管理");
        initDictType("process_category", "流程分类", "业务流程的分类管理");
        initDictType("environment_type", "环境类型", "环境配置的类型管理");
        initDictType("project_status", "项目状态", "项目的状态管理");

        initDictDataIfEmpty("component_category", List.of(
                dictItem("Database", "数据库", 1, "关系型和非关系型数据库"),
                dictItem("Cache", "缓存", 2, "Redis、Memcached等缓存组件"),
                dictItem("MessageQueue", "消息队列", 3, "Kafka、RabbitMQ等消息中间件"),
                dictItem("API", "API服务", 4, "REST API、GraphQL等接口服务"),
                dictItem("Storage", "存储服务", 5, "文件存储、对象存储等"),
                dictItem("Monitoring", "监控服务", 6, "Prometheus、Grafana等监控组件"),
                dictItem("Authentication", "认证服务", 7, "OAuth、JWT等认证授权服务"),
                dictItem("Other", "其他", 99, "其他未分类的组件")
        ));

        initDictDataIfEmpty("process_category", List.of(
                dictItem("release", "发布流程", 1, "应用发布、部署相关流程"),
                dictItem("business", "业务流程", 2, "业务操作相关流程"),
                dictItem("operation", "运维流程", 3, "系统运维、监控相关流程"),
                dictItem("incident", "故障处理", 4, "故障响应、处理流程"),
                dictItem("maintenance", "维护流程", 5, "系统维护、升级流程"),
                dictItem("security", "安全流程", 6, "安全审计、漏洞处理流程"),
                dictItem("other", "其他流程", 99, "其他未分类的流程")
        ));

        initDictDataIfEmpty("environment_type", List.of(
                dictItem("DEV", "开发环境", 1, "开发人员本地和开发服务器环境"),
                dictItem("TEST", "测试环境", 2, "功能测试、集成测试环境"),
                dictItem("STAGING", "预发布环境", 3, "上线前的最终验证环境"),
                dictItem("PROD", "生产环境", 4, "正式对外服务的生产环境"),
                dictItem("DEMO", "演示环境", 5, "产品演示和培训环境")
        ));

        initDictDataIfEmpty("project_status", List.of(
                dictItem("planning", "规划中", 1, "项目处于规划阶段"),
                dictItem("developing", "开发中", 2, "项目正在开发中"),
                dictItem("testing", "测试中", 3, "项目正在测试中"),
                dictItem("released", "已发布", 4, "项目已发布上线"),
                dictItem("maintenance", "维护中", 5, "项目处于维护阶段"),
                dictItem("deprecated", "已废弃", 6, "项目已废弃不再维护")
        ));

        log.info("字典数据初始化完成");
    }

    private void initSystemConfig() {
        initConfigIfEmpty("system.name", "新人筑基丹", "系统名称，用于登录页、首页和欢迎语");
        log.info("系统配置初始化完成");
    }

    private void initConfigIfEmpty(String key, String value, String description) {
        if (configRepository.findByConfigKey(key).isEmpty()) {
            SystemConfig config = new SystemConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setDescription(description);
            config.setEditable(true);
            config.setCreatedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());
            configRepository.save(config);
        }
    }

    private void initDictType(String typeCode, String typeName, String description) {
        if (dictTypeRepository.findByTypeCode(typeCode).isEmpty()) {
            SysDictType dictType = new SysDictType();
            dictType.setTypeCode(typeCode);
            dictType.setTypeName(typeName);
            dictType.setDescription(description);
            dictType.setStatus(true);
            dictType.setCreatedAt(LocalDateTime.now());
            dictType.setUpdatedAt(LocalDateTime.now());
            dictTypeRepository.save(dictType);
        }
    }

    private void initDictDataIfEmpty(String typeCode, List<SysDictData> items) {
        List<SysDictData> existing = dictDataRepository.findByTypeCodeOrderBySortOrderAsc(typeCode);
        boolean needsUpdate = existing.size() != items.size();
        if (!needsUpdate) {
            for (int i = 0; i < existing.size(); i++) {
                if (!existing.get(i).getDataValue().equals(items.get(i).getDataValue())) {
                    needsUpdate = true;
                    break;
                }
            }
        }
        if (needsUpdate) {
            for (SysDictData old : existing) {
                dictDataRepository.delete(old);
            }
            for (SysDictData item : items) {
                item.setTypeCode(typeCode);
                item.setCreatedAt(LocalDateTime.now());
                item.setUpdatedAt(LocalDateTime.now());
                dictDataRepository.save(item);
            }
            log.info("更新字典数据: {}", typeCode);
        }
    }

    private SysDictData dictItem(String value, String label, int sortOrder, String remark) {
        SysDictData data = new SysDictData();
        data.setDataValue(value);
        data.setDataLabel(label);
        data.setSortOrder(sortOrder);
        data.setStatus(true);
        data.setRemark(remark);
        return data;
    }
}
