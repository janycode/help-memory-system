package com.example.mynewwork.controller;

import com.example.mynewwork.model.dto.ApiResponse;
import com.example.mynewwork.model.entity.SoCreationLog;
import com.example.mynewwork.repository.SoCreationLogRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/tools")
public class BusinessToolController {

    private static final Logger log = LoggerFactory.getLogger(BusinessToolController.class);
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SoCreationLogRepository soCreationLogRepository;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public BusinessToolController(SoCreationLogRepository soCreationLogRepository) {
        this.soCreationLogRepository = soCreationLogRepository;
    }

    /**
     * 批量创建进仓单（SO）
     */
    @PostMapping(value = "/batch-so/submit", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter batchCreateSO(@RequestBody Map<String, Object> request) {
        SseEmitter emitter = new SseEmitter(300000L);

        executorService.execute(() -> {
            try {
                String apiUrl = (String) request.get("apiUrl");
                int batchCount = (int) request.get("batchCount");
                int startNumber = request.get("startNumber") != null ? (int) request.get("startNumber") : 1;
                String prefix = request.get("prefix") != null ? (String) request.get("prefix") : "TEST";
                boolean satellite = request.get("satellite") != null && (boolean) request.get("satellite");
                String authorization = (String) request.get("authorization");
                String environment = request.get("environment") != null ? (String) request.get("environment") : "dev";
                @SuppressWarnings("unchecked")
                Map<String, Object> modifiedFields = request.get("modifiedFields") != null
                        ? (Map<String, Object>) request.get("modifiedFields")
                        : new HashMap<>();

                URI uri = URI.create(apiUrl);
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : ("https".equals(uri.getScheme()) ? 443 : 80);
                String origin = uri.getScheme() + "://" + host + (port != 80 && port != 443 ? ":" + port : "");

                String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

                emitter.send(SseEmitter.event().name("start").data(Map.of("totalCount", batchCount)));

                List<Map<String, Object>> results = new ArrayList<>();
                List<Map<String, Object>> errors = new ArrayList<>();

                for (int i = 0; i < batchCount; i++) {
                    int currentIndex = startNumber + i;
                    int padLen = currentIndex > 99 ? 3 : 2;
                    String bookingNo = prefix + dateStr + String.format("%0" + padLen + "d", currentIndex);

                    Map<String, Object> defaultFields = new LinkedHashMap<>();
                    defaultFields.put("satellite", satellite);
                    defaultFields.put("batteryGoods", false);
                    defaultFields.put("forwarderCode", "KN");
                    defaultFields.put("consigneeCode", "KN-SEX");
                    defaultFields.put("payerCode", "KNSZ");
                    defaultFields.put("type", 0);
                    defaultFields.put("warehouseCode", "PLA");
                    defaultFields.put("bookingNo", bookingNo);
                    if (satellite) {
                        defaultFields.put("satelliteWarehouseCode", "CAN");
                    }
                    defaultFields.put("freightForwarder", "张三");
                    defaultFields.put("packageQty", "100");
                    defaultFields.put("packageType", "CTN");
                    defaultFields.put("totalBoxQty", null);
                    defaultFields.put("volume", "2");
                    defaultFields.put("grossWeight", "500");
                    defaultFields.put("destinationCountry", "ABW");
                    defaultFields.put("destinationPort", null);
                    defaultFields.put("markCode", "TEST");
                    defaultFields.put("shippingOrderFormData", new HashMap<>());
                    defaultFields.put("expenseConfigs", new ArrayList<>());

                    defaultFields.putAll(modifiedFields);
                    // 强制覆盖前端可能传入的错误类型字段，确保类型正确
                    defaultFields.put("batteryGoods", false);
                    defaultFields.put("type", 0);
                    defaultFields.put("bookingNo", bookingNo);

                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("Cookie", "Authorization=" + authorization);
                    headers.put("Host", uri.getHost() + (port != 80 && port != 443 ? ":" + port : ""));
                    headers.put("Origin", origin);

                    try {
                        String requestBody = objectMapper.writeValueAsString(defaultFields);
                        log.info("[SO] 发起请求: bookingNo={}, url={}", bookingNo, apiUrl);
                        log.info("[SO] 请求体: {}", requestBody);

                        HttpRequest httpRequest = HttpRequest.newBuilder()
                                .uri(uri)
                                .header("Content-Type", "application/json")
                                .header("Cookie", "Authorization=" + authorization)
                                .header("Origin", origin)
                                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                                .build();

                        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                        log.info("[SO] 响应: bookingNo={}, status={}, body={}", bookingNo, response.statusCode(), response.body());

                        Map<String, Object> result = new HashMap<>();
                        result.put("bookingNo", bookingNo);
                        result.put("success", response.statusCode() >= 200 && response.statusCode() < 300);
                        result.put("url", apiUrl);
                        result.put("headers", headers);
                        result.put("body", defaultFields);
                        result.put("response", parseJsonRaw(response.body()));

                        if ((boolean) result.get("success")) {
                            results.add(result);
                            emitter.send(SseEmitter.event().name("success").data(result));
                        } else {
                            errors.add(result);
                            emitter.send(SseEmitter.event().name("error").data(result));
                        }

                        SoCreationLog creationLog = new SoCreationLog();
                        creationLog.setBookingNo(bookingNo);
                        creationLog.setStatus((boolean) result.get("success") ? "success" : "error");
                        creationLog.setRequestBody(requestBody);
                        creationLog.setResponse(response.body());
                        creationLog.setEnvironment(environment);
                        soCreationLogRepository.save(creationLog);
                    } catch (Exception e) {
                        log.error("[SO] 请求失败: bookingNo={}, error={}", bookingNo, e.getMessage(), e);
                        Map<String, Object> errorData = new HashMap<>();
                        errorData.put("bookingNo", bookingNo);
                        errorData.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
                        errorData.put("url", apiUrl);
                        errorData.put("headers", headers);
                        errorData.put("body", defaultFields);
                        errors.add(errorData);
                        emitter.send(SseEmitter.event().name("error").data(errorData));
                    }

                    if (i < batchCount - 1) {
                        Thread.sleep(500);
                    }
                }

                Map<String, Object> completeData = new HashMap<>();
                completeData.put("success", errors.isEmpty());
                completeData.put("results", results);
                completeData.put("errors", errors);
                completeData.put("totalCount", batchCount);
                completeData.put("successCount", results.size());
                completeData.put("errorCount", errors.size());
                emitter.send(SseEmitter.event().name("complete").data(completeData));
                emitter.complete();

            } catch (Exception e) {
                log.error("[SO] 批量创建异常", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data(Map.of("error", e.getMessage())));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 批量接单
     */
    @PostMapping(value = "/batch-so/receive", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter batchReceiveSO(@RequestBody Map<String, Object> request) {
        SseEmitter emitter = new SseEmitter(300000L);

        executorService.execute(() -> {
            try {
                String apiUrl = (String) request.get("apiUrl");
                @SuppressWarnings("unchecked")
                List<String> bookingNos = (List<String>) request.get("bookingNos");
                boolean satellite = request.get("satellite") != null && (boolean) request.get("satellite");
                String authorization = (String) request.get("authorization");

                URI uri = URI.create(apiUrl);
                String origin = uri.getScheme() + "://" + uri.getHost();
                String detailUrl = origin + "/api/om/v1/inbound-order/receive-order/get";
                String receiveUrl = origin + "/api/om/v1/inbound-order/receive";

                List<Map<String, Object>> results = new ArrayList<>();
                List<Map<String, Object>> errors = new ArrayList<>();

                for (String bookingNo : bookingNos) {
                    try {
                        String detailBody = objectMapper.writeValueAsString(Map.of("keyword", bookingNo, "apptVersion", ""));
                        HttpRequest detailRequest = HttpRequest.newBuilder()
                                .uri(URI.create(detailUrl))
                                .header("Content-Type", "application/json")
                                .header("Cookie", "Authorization=" + authorization)
                                .POST(HttpRequest.BodyPublishers.ofString(detailBody))
                                .build();

                        HttpResponse<String> detailResponse = httpClient.send(detailRequest, HttpResponse.BodyHandlers.ofString());
                        Map<String, Object> detailData = parseJson(detailResponse.body(), Map.class);

                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> inboundItems = (List<Map<String, Object>>) detailData.get("inboundItems");

                        if (inboundItems == null || inboundItems.isEmpty()) {
                            throw new RuntimeException("未找到单号详情");
                        }

                        Map<String, Object> resultData = new HashMap<>();
                        resultData.put("bookingNo", bookingNo);
                        resultData.put("detail", inboundItems.get(0));

                        if (satellite) {
                            String receiveBody = objectMapper.writeValueAsString(Map.of("inboundVehicles", new ArrayList<>(), "inboundItems", inboundItems));
                            HttpRequest receiveRequest = HttpRequest.newBuilder()
                                    .uri(URI.create(receiveUrl))
                                    .header("Content-Type", "application/json")
                                    .header("Cookie", "Authorization=" + authorization)
                                    .POST(HttpRequest.BodyPublishers.ofString(receiveBody))
                                    .build();

                            HttpResponse<String> receiveResponse = httpClient.send(receiveRequest, HttpResponse.BodyHandlers.ofString());
                            resultData.put("receiveResponse", parseJsonRaw(receiveResponse.body()));
                            resultData.put("receiveSuccess", true);
                        } else {
                            Map<String, Object> vehicle = new LinkedHashMap<>();
                            vehicle.put("associatedOrderNos", List.of(bookingNo));
                            vehicle.put("driverPhone", "18911112222");
                            vehicle.put("vehicleNo", "粤B" + bookingNo.substring(bookingNo.length() - 6));
                            vehicle.put("vehicleType", 0);

                            String receiveBody = objectMapper.writeValueAsString(Map.of("inboundVehicles", List.of(vehicle), "inboundItems", inboundItems));
                            HttpRequest receiveRequest = HttpRequest.newBuilder()
                                    .uri(URI.create(receiveUrl))
                                    .header("Content-Type", "application/json")
                                    .header("Cookie", "Authorization=" + authorization)
                                    .POST(HttpRequest.BodyPublishers.ofString(receiveBody))
                                    .build();

                            HttpResponse<String> receiveResponse = httpClient.send(receiveRequest, HttpResponse.BodyHandlers.ofString());
                            @SuppressWarnings("unchecked")
                            List<Object> receiveResult = parseJson(receiveResponse.body(), List.class);
                            String orderNo = (String) receiveResult.get(0);
                            resultData.put("receiveResponse", receiveResult);

                            String paymentUrl = origin + "/api/om/v1/inbound-self-service/pc/payment";
                            String paymentBody = objectMapper.writeValueAsString(Map.of(
                                    "orderNo", orderNo,
                                    "expenseIds", new ArrayList<>(),
                                    "paymentMethod", "CASH",
                                    "authCode", null,
                                    "transferName", null
                            ));
                            HttpRequest paymentRequest = HttpRequest.newBuilder()
                                    .uri(URI.create(paymentUrl))
                                    .header("Content-Type", "application/json")
                                    .header("Cookie", "Authorization=" + authorization)
                                    .POST(HttpRequest.BodyPublishers.ofString(paymentBody))
                                    .build();

                            HttpResponse<String> paymentResponse = httpClient.send(paymentRequest, HttpResponse.BodyHandlers.ofString());
                            resultData.put("paymentResponse", parseJsonRaw(paymentResponse.body()));
                            resultData.put("receiveSuccess", true);
                        }

                        results.add(resultData);
                        emitter.send(SseEmitter.event().name("receive-success").data(resultData));

                    } catch (Exception e) {
                        log.error("[SO] 接单失败: bookingNo={}", bookingNo, e);
                        Map<String, Object> errorData = new HashMap<>();
                        errorData.put("bookingNo", bookingNo);
                        errorData.put("error", e.getMessage());
                        errors.add(errorData);
                        emitter.send(SseEmitter.event().name("receive-error").data(errorData));
                    }

                    Thread.sleep(500);
                }

                Map<String, Object> completeData = new HashMap<>();
                completeData.put("success", errors.isEmpty());
                completeData.put("results", results);
                completeData.put("errors", errors);
                completeData.put("totalCount", bookingNos.size());
                completeData.put("successCount", results.size());
                completeData.put("errorCount", errors.size());
                emitter.send(SseEmitter.event().name("receive-complete").data(completeData));
                emitter.complete();

            } catch (Exception e) {
                log.error("[SO] 批量接单异常", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data(Map.of("error", e.getMessage())));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 获取RocketMQ Topic列表
     */
    @PostMapping("/mq-topics")
    public ApiResponse<?> getMqTopics(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> proxyHeaders = (Map<String, String>) request.get("proxyHeaders");
            String targetUrl = (String) request.get("targetUrl");
            String env = (String) request.get("env");

            log.info("[MQ] 获取Topic列表: targetUrl={}, env={}", targetUrl, env);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .GET();

            if (proxyHeaders != null) {
                String[] forwardKeys = {"Cookie", "User-Agent", "Accept", "x-xsrf-token"};
                for (String key2 : forwardKeys) {
                    if (proxyHeaders.containsKey(key2)) {
                        requestBuilder.header(key2, proxyHeaders.get(key2));
                    }
                }
            }

            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            Object rawData = parseJsonRaw(response.body());

            List<String> topicList = new ArrayList<>();
            if (rawData instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) rawData;
                Object dataObj = dataMap.get("data");
                if (dataObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> innerData = (Map<String, Object>) dataObj;
                    Object topicsObj = innerData.get("topicList");
                    if (topicsObj == null) {
                        topicsObj = innerData.get("topicNameList");
                    }
                    if (topicsObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Object> topics = (List<Object>) topicsObj;
                        String prefix = "dev".equalsIgnoreCase(env) ? "dev%" : "staging%";
                        for (Object topic : topics) {
                            String topicStr = String.valueOf(topic);
                            if (topicStr.startsWith(prefix)) {
                                topicList.add(topicStr);
                            }
                        }
                    }
                }
            }

            Collections.sort(topicList);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("topics", topicList);
            result.put("rawResponse", rawData);

            return ApiResponse.success(result);

        } catch (Exception e) {
            log.error("[MQ] 获取Topic列表失败", e);
            return ApiResponse.error("获取Topic列表失败: " + e.getMessage());
        }
    }

    /**
     * 预览Java文件内容
     */
    @PostMapping("/mq-preview-java")
    public ApiResponse<?> previewJavaFile(@RequestBody Map<String, Object> request) {
        try {
            String filePath = (String) request.get("filePath");
            if (filePath == null || filePath.trim().isEmpty()) {
                return ApiResponse.error("文件路径不能为空");
            }

            String content = Files.readString(Paths.get(filePath));
            log.info("[MQ] 预览Java文件: {}", filePath);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content", content);
            result.put("filePath", filePath);

            return ApiResponse.success(result);

        } catch (java.nio.file.NoSuchFileException e) {
            log.error("[MQ] Java文件不存在: {}", e.getMessage());
            return ApiResponse.error("文件不存在: " + e.getMessage());
        } catch (java.nio.file.AccessDeniedException e) {
            log.error("[MQ] Java文件访问权限不足: {}", e.getMessage());
            return ApiResponse.error("文件访问权限不足: " + e.getMessage());
        } catch (Exception e) {
            log.error("[MQ] 预览Java文件失败", e);
            return ApiResponse.error("预览Java文件失败: " + e.getMessage());
        }
    }

    /**
     * 解析Java文件，提取tag和字段
     */
    @PostMapping("/mq-parse-java")
    public ApiResponse<?> parseJavaFile(@RequestBody Map<String, Object> request) {
        try {
            String filePath = (String) request.get("filePath");
            if (filePath == null || filePath.trim().isEmpty()) {
                return ApiResponse.error("文件路径不能为空");
            }

            String content = Files.readString(Paths.get(filePath));
            log.info("[MQ] 解析Java文件: {}", filePath);

            String tag = extractTag(content);
            Map<String, Object> messageBody = extractFields(content);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("tag", tag);
            result.put("messageBody", messageBody);

            return ApiResponse.success(result);

        } catch (java.nio.file.NoSuchFileException e) {
            log.error("[MQ] Java文件不存在: {}", e.getMessage());
            return ApiResponse.error("文件不存在: " + e.getMessage());
        } catch (java.nio.file.AccessDeniedException e) {
            log.error("[MQ] Java文件访问权限不足: {}", e.getMessage());
            return ApiResponse.error("文件访问权限不足: " + e.getMessage());
        } catch (Exception e) {
            log.error("[MQ] 解析Java文件失败", e);
            return ApiResponse.error("解析Java文件失败: " + e.getMessage());
        }
    }

    /**
     * 解析上传的Java文件（内存处理，不落盘），提取tag和字段
     * 适用于局域网多人使用：浏览器选择本地文件后上传解析
     */
    @PostMapping("/mq-parse-java-upload")
    public ApiResponse<?> parseJavaFileUpload(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ApiResponse.error("请选择要解析的Java文件");
            }
            if (!file.getOriginalFilename().toLowerCase().endsWith(".java")) {
                return ApiResponse.error("仅支持解析 .java 文件");
            }

            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            log.info("[MQ] 解析上传的Java文件: {}", file.getOriginalFilename());

            String tag = extractTag(content);
            Map<String, Object> messageBody = extractFields(content);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("tag", tag);
            result.put("messageBody", messageBody);
            result.put("fileName", file.getOriginalFilename());

            return ApiResponse.success(result);

        } catch (Exception e) {
            log.error("[MQ] 解析上传的Java文件失败", e);
            return ApiResponse.error("解析Java文件失败: " + e.getMessage());
        }
    }

    /**
     * 预览上传的Java文件内容（内存处理，不落盘）
     */
    @PostMapping("/mq-preview-java-upload")
    public ApiResponse<?> previewJavaFileUpload(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ApiResponse.error("请选择要预览的Java文件");
            }

            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            log.info("[MQ] 预览上传的Java文件: {}", file.getOriginalFilename());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content", content);
            result.put("fileName", file.getOriginalFilename());

            return ApiResponse.success(result);

        } catch (Exception e) {
            log.error("[MQ] 预览上传的Java文件失败", e);
            return ApiResponse.error("预览Java文件失败: " + e.getMessage());
        }
    }

    private String extractTag(String content) {
        Pattern pattern = Pattern.compile("public\\s+static\\s+final\\s+String\\s+\\w+\\s*=\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private Map<String, Object> extractFields(String content) {
        Map<String, Object> fields = new LinkedHashMap<>();
        java.util.Random random = new java.util.Random();

        Pattern pattern = Pattern.compile("\\n\\s*(private|public|protected)\\s+(?!static\\s+final\\s+String)([^;=]+?)\\s+(\\w+)\\s*[;=]");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String fieldType = matcher.group(2).trim();
            String fieldName = matcher.group(3);

            if (fieldName.equals("serialVersionUID")) {
                continue;
            }

            if (fieldType.equals("String")) {
                fields.put(fieldName, "mock_" + fieldName + "_" + random.nextInt(10000));
            } else if (fieldType.equals("Integer") || fieldType.equals("int")) {
                fields.put(fieldName, random.nextInt(9999) + 1);
            } else if (fieldType.equals("Long") || fieldType.equals("long")) {
                fields.put(fieldName, random.nextLong(9999999999L) + 1);
            } else if (fieldType.equals("Boolean") || fieldType.equals("boolean")) {
                fields.put(fieldName, random.nextBoolean());
            } else if (fieldType.equals("BigDecimal")) {
                fields.put(fieldName, String.format("%.2f", random.nextDouble() * 1000));
            } else if (fieldType.equals("LocalDateTime") || fieldType.equals("Date")) {
                fields.put(fieldName, java.time.LocalDateTime.now().toString());
            } else {
                fields.put(fieldName, "");
            }
        }

        return fields;
    }

    /**
     * RocketMQ消息发送
     */
    @PostMapping("/mq-send")
    public ApiResponse<?> sendMqMessage(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> proxyHeaders = (Map<String, String>) request.get("proxyHeaders");
            String topic = (String) request.get("topic");
            String key = (String) request.get("key");
            String tag = (String) request.get("tag");
            String messageBody = (String) request.get("messageBody");
            boolean traceEnabled = request.get("traceEnabled") != null && (boolean) request.get("traceEnabled");
            String targetUrl = (String) request.get("targetUrl");

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("topic", topic);
            payload.put("key", key);
            payload.put("tag", tag);
            payload.put("messageBody", messageBody);
            payload.put("traceEnabled", traceEnabled);

            String postData = objectMapper.writeValueAsString(payload);
            log.info("[MQ] 发送消息: targetUrl={}, topic={}", targetUrl, topic);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .header("Content-Type", "application/json");

            if (proxyHeaders != null) {
                String[] forwardKeys = {"Cookie", "User-Agent", "Accept", "x-xsrf-token"};
                for (String key2 : forwardKeys) {
                    if (proxyHeaders.containsKey(key2)) {
                        requestBuilder.header(key2, proxyHeaders.get(key2));
                    }
                }
            }

            requestBuilder.POST(HttpRequest.BodyPublishers.ofString(postData));
            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            log.info("[MQ] 响应: status={}", response.statusCode());
            return ApiResponse.success(parseJsonRaw(response.body()));

        } catch (Exception e) {
            log.error("[MQ] 发送失败", e);
            return ApiResponse.error("发送失败: " + e.getMessage());
        }
    }

    /**
     * 查询SO创建记录
     */
    @GetMapping("/so-records")
    public ApiResponse<?> getSoRecords(@RequestParam(defaultValue = "dev") String env) {
        List<SoCreationLog> records = soCreationLogRepository.findByEnvironmentOrderByCreatedAtDesc(env);
        List<Map<String, Object>> result = new ArrayList<>();
        for (SoCreationLog record : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", record.getId());
            item.put("bookingNo", record.getBookingNo());
            item.put("status", record.getStatus());
            item.put("requestBody", parseJsonRaw(record.getRequestBody()));
            item.put("response", parseJsonRaw(record.getResponse()));
            item.put("createdAt", record.getCreatedAt() != null ? record.getCreatedAt().toString() : null);
            result.add(item);
        }
        return ApiResponse.success(result);
    }

    /**
     * 查询成功的SO号（创建历史）
     */
    @GetMapping("/so-history")
    public ApiResponse<?> getSoHistory(@RequestParam(defaultValue = "dev") String env) {
        List<SoCreationLog> records = soCreationLogRepository.findByStatusAndEnvironmentOrderByCreatedAtDesc("success", env);
        List<Map<String, String>> result = new ArrayList<>();
        for (SoCreationLog record : records) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("no", record.getBookingNo());
            item.put("time", record.getCreatedAt() != null ? record.getCreatedAt().toString().replace("T", " ").substring(0, 19) : "");
            result.add(item);
        }
        return ApiResponse.success(result);
    }

    /**
     * 删除SO创建记录
     */
    @DeleteMapping("/so-records")
    public ApiResponse<?> clearSoRecords() {
        soCreationLogRepository.deleteAll();
        return ApiResponse.success("已清空");
    }

    /**
     * HashId 编码
     */
    @PostMapping("/hashid/encode")
    public ApiResponse<?> hashidEncode(@RequestBody Map<String, Object> request) {
        try {
            String salt = (String) request.get("salt");
            int length = (int) request.get("length");
            long input = Long.parseLong(request.get("input").toString());
            com.example.mynewwork.util.HashidsCompat hashids = new com.example.mynewwork.util.HashidsCompat(salt, length);
            return ApiResponse.success(Map.of("result", hashids.encode(input)));
        } catch (Exception e) {
            log.error("[HashId] 编码失败", e);
            return ApiResponse.error("编码失败: " + e.getMessage());
        }
    }

    /**
     * HashId 解码
     */
    @PostMapping("/hashid/decode")
    public ApiResponse<?> hashidDecode(@RequestBody Map<String, Object> request) {
        try {
            String salt = (String) request.get("salt");
            int length = (int) request.get("length");
            String input = (String) request.get("input");
            com.example.mynewwork.util.HashidsCompat hashids = new com.example.mynewwork.util.HashidsCompat(salt, length);
            return ApiResponse.success(Map.of("result", hashids.decode(input)));
        } catch (Exception e) {
            log.error("[HashId] 解码失败", e);
            return ApiResponse.error("解码失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T parseJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    private Object parseJsonRaw(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }
}