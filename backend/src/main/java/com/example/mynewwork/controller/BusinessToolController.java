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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
                    String bookingNo = prefix + dateStr + String.format("%02d", currentIndex);

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
                        creationLog.setEnvironment("dev");
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
     * RocketMQ消息发送
     */
    @PostMapping("/mq-send")
    public ApiResponse<?> sendMqMessage(@RequestBody Map<String, Object> request) {
        try {
            String env = (String) request.get("env");
            @SuppressWarnings("unchecked")
            Map<String, String> proxyHeaders = (Map<String, String>) request.get("proxyHeaders");
            String topic = (String) request.get("topic");
            String key = (String) request.get("key");
            String tag = (String) request.get("tag");
            String messageBody = (String) request.get("messageBody");

            String baseUrl;
            if ("dev".equals(env)) {
                baseUrl = "http://192.168.33.10:9880";
            } else {
                baseUrl = "https://devops.leaderrun.com/rocketmq";
            }
            String targetUrl = baseUrl + "/topic/sendTopicMessage.do";

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("topic", topic);
            payload.put("key", key);
            payload.put("tag", tag);
            payload.put("messageBody", messageBody);
            payload.put("traceEnabled", false);

            String postData = objectMapper.writeValueAsString(payload);
            log.info("[MQ] 发送消息: env={}, targetUrl={}", env, targetUrl);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .header("Content-Type", "application/json");

            // 只转发原始实现中指定的 headers
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