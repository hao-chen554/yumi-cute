package com.yumi.cute.generator;

import com.yumi.cute.common.BizException;
import com.yumi.cute.common.ResultCode;
import com.yumi.cute.entity.PhotoOrder;
import com.yumi.cute.entity.PhotoTask;

import java.net.URI;

import com.yumi.cute.entity.StyleTemplate;
import com.yumi.cute.storage.FileStorage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.provider", havingValue = "dashscope")
public class AiPhotoGenerator implements PhotoGenerator {

    private final FileStorage fileStorage;

    // -------- AI配置 --------
    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.base-url}")
    private String baseUrl;

    @Value("${ai.model:qwen-image-3.0-pro}")
    private String model;

    @Value("${ai.size:1024x1024}")
    private String size;

    @Value("${ai.prompt-extend:false}")
    private boolean promptExtend;

    @Value("${ai.enable-thinking:false}")
    private boolean enableThinking;

    @Value("${ai.watermark:false}")
    private boolean watermark;

    @Value("${ai.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${ai.retry.backoff-ms:2000}")
    private long backoffMs;

    @Value("${ai.connect-timeout-ms:15000}")
    private long connectTimeoutMs;

    @Value("${ai.read-timeout-ms:600000}")
    private long readTimeoutMs;

    // -------- 本地存储配置（要把本地图片读出来转 base64）--------
    @Value("${storage.local.path}")
    private String storagePath;

    @Value("${storage.local.url-prefix}")
    private String urlPrefix;

    private RestClient restClient;

    /**
     * 让 RestClient 把 JSON 读成一个嵌套的 Map，不需要手工解析
     */
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {
            };

    /**
     * 构建带超时的 RestClient
     * 必须放在 @PostConstruct 里，因为超时值来自@Value注入，字段初始化时还没注入进来
     */
    @PostConstruct
    void init() {
        if (apiKey == null || apiKey.isBlank()) {
            log.error("ai.provider=dashscope, 但没读到API Key（ai.api-key 或 环境变量 DASHSCOPE_API_KEY)");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            log.error("ai.base-url 为空，无法调用 AI （需要环境变量 AI_BASE_URL，或直接在配置里写死）");
        }

        // 图像生成耗时较长，必须显式设置足够大的超时，否则连接会在出图前被客户端自己掐断，出现莫名其妙的IOException
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectTimeoutMs)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.restClient = RestClient.builder().requestFactory(factory).build();
        log.info("AI生图器就绪：model={} size={} promptExtend={} enableThinking={} 连接超时={}ms 读超时={}ms 最多尝试={}次", model, size, promptExtend,
                enableThinking, connectTimeoutMs, readTimeoutMs, maxAttempts);
    }

    @Override
    public List<String> generate(PhotoOrder order, StyleTemplate style) {
        // 检查模型key和base-url配置
        requireConfig();

        int count = order.getTotalCount() == null ? 1 : order.getTotalCount();

        // 1.两张参考图，顺序固定：先人后宠 -- 因为提示词中是这个顺序
        List<String> images = List.of(
                toDataUri(order.getPersonPhotoUrl(), "本人照片"),
                toDataUri(order.getPetPhotoUrl(), "宠物照片")
        );

        // 2.组装请求体
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("prompt", buildPrompt(style));
        body.put("image", images);
        body.put("size", size);
        body.put("prompt_extend", promptExtend);
        body.put("n", count);
        if (promptExtend) {
            body.put("enable_thinking", enableThinking);
        }
        body.put("watermark", watermark);
        // 在 body 组装里加一行
        body.put("negative_prompt",
                "拼图, 多格画面, 六宫格, 九宫格, 分格构图, 网格线, 分割线, 分屏, "
                        + "多张照片拼接, 画中画, 相框, 白边, 文字, 水印, logo, "
                        + "多余的手指, 手部畸形, 宠物变形, 宠物拟人化, 换脸, 多余的动物");

        // 3.发请求
        Map<String, Object> response = withRetry("调用AI出图", () -> post(body));

        // 4.取出全部结果，逐张下载并落库
        List<?> dataList = extractData(response);
        log.info("订单 {} 请求 {} 张，模型返回 {} 张", order.getOrderNo(), count, dataList.size());

        ArrayList<String> urls = new ArrayList<>(dataList.size());
        for (Object data : dataList) {
            String tempUrl = extractUrl(data);
            try {
                urls.add(withRetry("下载结果图", () -> downloadAndStore(tempUrl)));
            } catch (Exception e) {
                log.error("订单 {} 第 {} 张下载失败", order.getOrderNo(), urls.size() + 1, e);
                urls.add(null);
            }
        }
        // 模型少给时，补占位，确保size == count
        while(urls.size() < count){
            urls.add(null);
        }
        return urls;
    }

    /**
     * 解析响应
     */
    private List<?> extractData(Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            throw new RuntimeException("AI返回了空响应体");
        }

        // 兼容模式失败时一般走 HTTP 4xx/5xx，但 200 + error 对象也可能出现
        Object error = response.get("error");
        if (error != null) {
            throw new RuntimeException("AI 返回错误：" + abbreviate(error));
        }

        Object dataObj = response.get("data");
        if (!(dataObj instanceof List<?> list) || list.isEmpty()) {
            throw new RuntimeException("AI 返回里没有 data：" + abbreviate(dataObj));
        }
        return list;
    }

    /**
     * 取结果图地址
     * 只认url -- 官方文档说明兼容模式传 response_format=b64_json 会被忽略，永远回url
     */
    private String extractUrl(Object item) {
        if (item instanceof Map<?, ?> map) {
            Object url = map.get("url");
            if (url instanceof String s && !s.isBlank()) {
                return s;
            }
        }
        throw new RuntimeException("返回项里没有可用的 url：" + abbreviate(item));
    }

    /**
     * 下载结果图并转存。
     * 结果图 URL 只活 24 小时，必须立刻落到我们自己的存储里。
     */
    private String downloadAndStore(String url) {
        byte[] bytes = restClient.get().uri(URI.create(url)).retrieve().body(byte[].class);

        if (bytes == null || bytes.length == 0) {
            throw new RuntimeException("下载到的图片是空的：" + url);
        }
        return fileStorage.save(bytes, ".png");
    }

    /**
     * 通用重试。
     * <p>
     * 关键点：把"值不值得再试一次"和"怎么重试"分开。
     * - 429 限流、5xx 服务端故障、连接/读超时 → 过一会儿再试有希望
     * - 400 参数错、401 鉴权失败、403 无权限 → 重试一百次也是同样结果，立刻失败
     * 否则会把无效请求重试 3 遍，白等时间还没用。
     * <p>
     * （官方计费说明：失败的调用不计费，所以重试不用担心钱。）
     */
    private <T> T withRetry(String what, Supplier<T> action) {
        int attempt = 1;
        while (true) {
            try {
                return action.get();
            } catch (RestClientResponseException e) {
                boolean retryable = e.getStatusCode().value() == 429 || e.getStatusCode().is5xxServerError();
                if (!retryable || attempt >= maxAttempts) {
                    throw new RuntimeException(describeHttp(what, e), e);
                }
                log.warn("{} 第{}/{}次失败（HTTP{},可重试", what, attempt, maxAttempts, e.getStatusCode().value());
            } catch (ResourceAccessException e) {
                if (attempt >= maxAttempts) {
                    throw new RuntimeException(what + "连接或读取超时（已尝试" + attempt + "次）：" + e.getMessage(), e);
                }
                log.warn("{} 第 {}/{} 次超时（可重试）：{}", what, attempt, maxAttempts, e.getMessage());
            }
            long waitMs = backoffMs * (1L << (attempt - 1));   // 2s → 4s → 8s
            log.info("{} 毫秒后重试 {}", waitMs, what);
            sleep(waitMs);
            attempt++;
        }
    }

    /**
     * 把http错误翻译成人能看懂的原因，方便通过日志定位
     */
    private String describeHttp(String what, RestClientResponseException e) {
        int status = e.getStatusCode().value();
        String reason = switch (status) {
            case 400 -> "请求参数有误（图片尺寸/体积超限，或模型不支持某个参数）";
            case 401 -> "鉴权失败：API Key 不正确，或与base-url 不是同一地域";
            case 403 -> "无权限：API Key 没有开通这个模型";
            case 404 -> "接口不存在：检查 base-url 是否拼错";
            case 429 -> "触发限流";
            default -> e.getStatusCode().is5xxServerError() ? "AI 服务端异常 " : "AI 接口返回异常";
        };
        return what + "失败：HTTP " + status + " " + reason + "；原始响应" + abbreviate(e.getResponseBodyAsString());
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("重试等待被终端", e);
        }
    }

    /**
     * 发请求
     */
    private Map<String, Object> post(Map<String, Object> body) {
        log.info("调用AI：model={} n={}", model, body.get("n"));

        return restClient.post()
                .uri(baseUrl + "/images/generations")
                .header("Authorization", "Bearer" + apiKey)
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(MAP_TYPE);
    }

    /**
     * 提示词（目前只做到“能看出宠物”）
     */
    private String buildPrompt(StyleTemplate style) {
        StringBuilder sb = new StringBuilder();
        sb.append("参考图1是主人本人，参考图2是主人的宠物。");
        sb.append("生成一张人宠同框的摄影级写真照片：");
        sb.append("主人和宠物同时清晰出现在画面中，");
        sb.append("严格保留参考图1中人物的面部特征，");
        sb.append("严格保留参考图2中宠物的物种、毛色和花纹。");
        sb.append("画面是完整的一张照片，铺满整个画幅。");
        sb.append("风格是【").append(style.getName()).append("】：").append(style.getPrompt());
        return sb.toString();
    }

    /**
     * 本地图片 -> base64 （外部不能直接访问localhost，所以转成base64
     */
    private String toDataUri(String fileUrl, String roleName) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少" + roleName + "，无法生成人宠写真");
        }

        // 若不是本地存储的地址，当作公网url直接交给AI自己去下载
        if (!fileUrl.startsWith(urlPrefix)) {
            if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
                return fileUrl;
            }
            throw new BizException(ResultCode.PARAM_ERROR, roleName + "地址不合法：" + fileUrl);
        }

        // http://localhost:8080/files/upload/xxx.jpg
        //                        └── 这一截是相对路径 ──┘
        // 注意开头是 "/"，直接拼会拼出 "//"
        String relative = fileUrl.substring(urlPrefix.length()).replaceFirst("^/+", "");
        Path path = Paths.get(storagePath, relative);

        if (!Files.exists(path)) {
            log.error("{} 文件不存在。解析出的路径={}（storagePath={}，relative={}）",
                    roleName, path.toAbsolutePath(), storagePath, relative);
            throw new BizException(ResultCode.PARAM_ERROR, roleName + "文件不存在（可能已被清理），请重新上传");
        }

        try {
            byte[] bytes = Files.readAllBytes(path);
            log.info("{}已转为base64，{}KB", roleName, bytes.length / 1024);
            return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new BizException(ResultCode.SYSTEM_ERROR, "读取" + roleName + "失败：" + e.getMessage());
        }
    }

    /**
     * 杂项
     */
    private void requireConfig() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BizException(ResultCode.SYSTEM_ERROR, "AI生图服务未配置API Key");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new BizException(ResultCode.SYSTEM_ERROR, "AI生图服务未配置base-url");
        }
    }

    private String abbreviate(Object obj) {
        String s = String.valueOf(obj);
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }
}
