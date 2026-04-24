-- 字典表初始化数据
-- 创建时间: 2026-04-19

-- ==================== 字典类型初始化 ====================

INSERT INTO sys_dict_type (type_code, type_name, description, status, created_by, created_at, updated_at)
VALUES ('component_category', '组件分类', '技术组件的分类管理', true, 1, NOW(), NOW());

INSERT INTO sys_dict_type (type_code, type_name, description, status, created_by, created_at, updated_at)
VALUES ('process_category', '流程分类', '业务流程的分类管理', true, 1, NOW(), NOW());

INSERT INTO sys_dict_type (type_code, type_name, description, status, created_by, created_at, updated_at)
VALUES ('environment_type', '环境类型', '环境配置的类型管理', true, 1, NOW(), NOW());

INSERT INTO sys_dict_type (type_code, type_name, description, status, created_by, created_at, updated_at)
VALUES ('project_status', '项目状态', '项目的状态管理', true, 1, NOW(), NOW());

-- ==================== 字典数据初始化 ====================

INSERT INTO sys_dict_data (type_code, data_value, data_label, sort_order, status, remark, created_by, created_at, updated_at) VALUES
('component_category', 'Database', '数据库', 1, true, '关系型和非关系型数据库', 1, NOW(), NOW()),
('component_category', 'Cache', '缓存', 2, true, 'Redis、Memcached等缓存组件', 1, NOW(), NOW()),
('component_category', 'MessageQueue', '消息队列', 3, true, 'Kafka、RabbitMQ等消息中间件', 1, NOW(), NOW()),
('component_category', 'API', 'API服务', 4, true, 'REST API、GraphQL等接口服务', 1, NOW(), NOW()),
('component_category', 'Storage', '存储服务', 5, true, '文件存储、对象存储等', 1, NOW(), NOW()),
('component_category', 'Monitoring', '监控服务', 6, true, 'Prometheus、Grafana等监控组件', 1, NOW(), NOW()),
('component_category', 'Authentication', '认证服务', 7, true, 'OAuth、JWT等认证授权服务', 1, NOW(), NOW()),
('component_category', 'Other', '其他', 99, true, '其他未分类的组件', 1, NOW(), NOW());

INSERT INTO sys_dict_data (type_code, data_value, data_label, sort_order, status, remark, created_by, created_at, updated_at) VALUES
('process_category', 'release', '发布流程', 1, true, '应用发布、部署相关流程', 1, NOW(), NOW()),
('process_category', 'business', '业务流程', 2, true, '业务操作相关流程', 1, NOW(), NOW()),
('process_category', 'operation', '运维流程', 3, true, '系统运维、监控相关流程', 1, NOW(), NOW()),
('process_category', 'incident', '故障处理', 4, true, '故障响应、处理流程', 1, NOW(), NOW()),
('process_category', 'maintenance', '维护流程', 5, true, '系统维护、升级流程', 1, NOW(), NOW()),
('process_category', 'security', '安全流程', 6, true, '安全审计、漏洞处理流程', 1, NOW(), NOW()),
('process_category', 'other', '其他流程', 99, true, '其他未分类的流程', 1, NOW(), NOW());

INSERT INTO sys_dict_data (type_code, data_value, data_label, sort_order, status, remark, created_by, created_at, updated_at) VALUES
('environment_type', 'DEV', '开发环境', 1, true, '开发人员本地和开发服务器环境', 1, NOW(), NOW()),
('environment_type', 'TEST', '测试环境', 2, true, '功能测试、集成测试环境', 1, NOW(), NOW()),
('environment_type', 'STAGING', '预发布环境', 3, true, '上线前的最终验证环境', 1, NOW(), NOW()),
('environment_type', 'PROD', '生产环境', 4, true, '正式对外服务的生产环境', 1, NOW(), NOW()),
('environment_type', 'DEMO', '演示环境', 5, true, '产品演示和培训环境', 1, NOW(), NOW());

INSERT INTO sys_dict_data (type_code, data_value, data_label, sort_order, status, remark, created_by, created_at, updated_at) VALUES
('project_status', 'planning', '规划中', 1, true, '项目处于规划阶段', 1, NOW(), NOW()),
('project_status', 'developing', '开发中', 2, true, '项目正在开发中', 1, NOW(), NOW()),
('project_status', 'testing', '测试中', 3, true, '项目正在测试中', 1, NOW(), NOW()),
('project_status', 'released', '已发布', 4, true, '项目已发布上线', 1, NOW(), NOW()),
('project_status', 'maintenance', '维护中', 5, true, '项目处于维护阶段', 1, NOW(), NOW()),
('project_status', 'deprecated', '已废弃', 6, true, '项目已废弃不再维护', 1, NOW(), NOW());

-- ==================== 初始化完成 ====================

SELECT '字典数据初始化完成' as message;

SELECT
    (SELECT COUNT(*) FROM sys_dict_type) as dict_type_count,
    (SELECT COUNT(*) FROM sys_dict_data) as dict_data_count,
    '初始化验证' as description;
