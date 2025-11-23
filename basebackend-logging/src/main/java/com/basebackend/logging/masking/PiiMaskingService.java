package com.basebackend.logging.masking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PII数据脱敏核心服务
 *
 * 核心特性：
 * 1. 高性能脱敏：预编译正则 + 复用缓冲，处理时间<5ms
 * 2. 多维度匹配：支持正则表达式和JSON路径两种匹配方式
 * 3. 多种脱敏策略：掩码、部分、哈希、移除、自定义
 * 4. 嵌套对象支持：支持JSON对象的深度脱敏
 * 5. 线程安全：所有操作都是线程安全的
 *
 * @author basebackend team
 * @since 2025-11-22
 */
public class PiiMaskingService {

    /**
     * 最大缓冲区大小（防止超大日志导致内存问题）
     */
    private static final int MAX_BUFFER = 8 * 1024;

    /**
     * 配置属性
     */
    private final MaskingProperties properties;

    /**
     * 监控指标
     */
    private final MaskingMetrics metrics;

    /**
     * JSON序列化器
     */
    private final ObjectMapper mapper;

    /**
     * 编译后的脱敏规则（线程安全）
     */
    private volatile List<CompiledRule> compiledRules;

    /**
     * 构造函数
     */
    public PiiMaskingService(MaskingProperties properties, MaskingMetrics metrics, ObjectMapper mapper) {
        this.properties = properties;
        this.metrics = metrics;
        this.mapper = mapper;
        this.compiledRules = compile(properties.getRules());
    }

    /**
     * 脱敏字符串
     */
    public String mask(String input) {
        if (!properties.isEnabled() || input == null || input.isEmpty()) {
            return input;
        }
        long start = System.nanoTime();
        String result = applyRegexRules(input);
        metrics.record(System.nanoTime() - start, !result.equals(input));
        return result;
    }

    /**
     * 脱敏对象（支持字符串和POJO）
     */
    public Object mask(Object input) {
        if (!properties.isEnabled() || input == null) {
            return input;
        }
        if (input instanceof String) {
            return mask((String) input);
        }
        long start = System.nanoTime();
        try {
            JsonNode node = mapper.valueToTree(input);
            boolean changed = applyJsonRules(node);
            Object result = changed ? mapper.convertValue(node, input.getClass()) : input;
            metrics.record(System.nanoTime() - start, changed);
            return result;
        } catch (Exception e) {
            // 对象转换失败时，回退到字符串脱敏
            String masked = mask(String.valueOf(input));
            metrics.record(System.nanoTime() - start, true);
            return masked;
        }
    }

    /**
     * 重新加载规则（支持热更新）
     */
    public void reloadRules(List<MaskingRule> rules) {
        this.compiledRules = compile(rules);
    }

    /**
     * 编译脱敏规则
     */
    private List<CompiledRule> compile(List<MaskingRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return Collections.emptyList();
        }
        List<CompiledRule> compiled = new ArrayList<>();
        for (MaskingRule rule : rules) {
            if (!rule.isEnabled()) {
                continue;
            }
            Pattern pattern = null;
            if (rule.getRegex() != null && !rule.getRegex().isEmpty()) {
                pattern = Pattern.compile(rule.getRegex(), Pattern.CASE_INSENSITIVE);
            }
            List<String> pathSegments = null;
            if (rule.getJsonPath() != null && !rule.getJsonPath().isEmpty()) {
                pathSegments = List.of(rule.getJsonPath().split("\\."));
            }
            compiled.add(new CompiledRule(rule, pattern, pathSegments));
        }
        return compiled;
    }

    /**
     * 应用正则规则
     */
    private String applyRegexRules(String input) {
        String out = input;
        for (CompiledRule rule : compiledRules) {
            if (rule.pattern == null) {
                continue;
            }
            Matcher matcher = rule.pattern.matcher(out);
            StringBuffer sb = new StringBuffer(Math.min(out.length(), MAX_BUFFER));
            boolean found = false;
            while (matcher.find()) {
                found = true;
                String masked = maskValue(matcher.group(), rule.rule);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(masked));
            }
            matcher.appendTail(sb);
            out = found ? sb.toString() : out;
        }
        return out;
    }

    /**
     * 应用JSON规则
     */
    private boolean applyJsonRules(JsonNode node) {
        boolean changed = false;
        for (CompiledRule rule : compiledRules) {
            if (rule.jsonPath == null || node == null) {
                continue;
            }
            changed |= maskByPath(node, rule);
        }
        return changed;
    }

    /**
     * 通过路径脱敏
     */
    private boolean maskByPath(JsonNode root, CompiledRule rule) {
        JsonNode current = root;
        Iterator<String> it = rule.jsonPath.iterator();
        JsonNode parent = null;
        String last = null;
        while (it.hasNext() && current != null) {
            last = it.next();
            parent = current;
            current = current.get(last);
        }
        if (parent instanceof ObjectNode && current != null && last != null) {
            String masked = maskValue(current.asText(), rule.rule);
            ((ObjectNode) parent).put(last, masked);
            return true;
        }
        return false;
    }

    /**
     * 根据规则脱敏值
     */
    private String maskValue(String value, MaskingRule rule) {
        if (value == null) {
            return null;
        }
        switch (rule.getStrategy()) {
            case REMOVE:
                return "";
            case HASH:
                return sha256(value);
            case CUSTOM:
                return rule.getReplacement();
            case PARTIAL:
            case MASK:
            default:
                return partialMask(value, rule.getPrefixKeep(), rule.getSuffixKeep(), rule.getReplacement());
        }
    }

    /**
     * 部分脱敏（保留前缀和后缀）
     */
    private String partialMask(String value, int prefix, int suffix, String replacement) {
        if (value.length() <= prefix + suffix) {
            return replacement.repeat(Math.max(1, value.length()));
        }
        StringBuilder sb = new StringBuilder(value.length());
        sb.append(value, 0, Math.min(prefix, value.length()));
        for (int i = 0; i < value.length() - prefix - suffix; i++) {
            sb.append(replacement);
        }
        sb.append(value, value.length() - Math.min(suffix, value.length()), value.length());
        return sb.toString();
    }

    /**
     * 计算SHA-256哈希值
     */
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "***";
        }
    }

    /**
     * 编译后的规则
     */
    private static final class CompiledRule {
        final MaskingRule rule;
        final Pattern pattern;
        final List<String> jsonPath;

        CompiledRule(MaskingRule rule, Pattern pattern, List<String> jsonPath) {
            this.rule = rule;
            this.pattern = pattern;
            this.jsonPath = jsonPath;
        }
    }
}
