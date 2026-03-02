package com.basebackend.ticket.ai;

import com.basebackend.ai.client.AiClient;
import com.basebackend.ai.client.AiMessage;
import com.basebackend.ai.client.AiRequest;
import com.basebackend.ai.client.AiResponse;
import com.basebackend.ticket.ai.dto.TicketClassifyResult;
import com.basebackend.ticket.entity.Ticket;
import com.basebackend.ticket.entity.TicketCategory;
import com.basebackend.ticket.entity.TicketComment;
import com.basebackend.ticket.mapper.TicketCategoryMapper;
import com.basebackend.ticket.mapper.TicketCommentMapper;
import com.basebackend.ticket.mapper.TicketMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 工单 AI 智能服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "basebackend.ai.enabled", havingValue = "true", matchIfMissing = false)
public class TicketAiServiceImpl implements TicketAiService {

    private final AiClient aiClient;
    private final TicketMapper ticketMapper;
    private final TicketCategoryMapper categoryMapper;
    private final TicketCommentMapper commentMapper;
    private final ObjectMapper objectMapper;

    @Override
    public TicketClassifyResult classifyTicket(String title, String description) {
        List<TicketCategory> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<TicketCategory>().eq(TicketCategory::getStatus, 1));

        String categoryList = categories.stream()
                .map(c -> String.format("- ID: %d, 名称: %s", c.getId(), c.getName()))
                .collect(Collectors.joining("\n"));

        String systemPrompt = """
                你是一个工单分类助手。根据工单标题和描述，从给定的分类列表中选择最合适的分类。

                可用分类列表:
                %s

                请严格以JSON格式回复，包含以下字段:
                {"categoryId": 分类ID数字, "categoryName": "分类名称", "confidence": 0.0到1.0之间的置信度, "reasoning": "分类理由"}
                只返回JSON，不要有其他内容。
                """.formatted(categoryList);

        String userMessage = "工单标题: %s\n工单描述: %s".formatted(title, description != null ? description : "无描述");

        AiRequest request = AiRequest.builder()
                .addMessage(AiMessage.system(systemPrompt))
                .addMessage(AiMessage.user(userMessage))
                .temperature(0.3)
                .maxTokens(500)
                .build();

        AiResponse response = aiClient.chat(request);
        return parseClassifyResult(response.content(), categories);
    }

    @Override
    public String summarizeTicket(Long ticketId) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new RuntimeException("工单不存在: " + ticketId);
        }

        LambdaQueryWrapper<TicketComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TicketComment::getTicketId, ticketId)
                .eq(TicketComment::getIsInternal, 0)
                .orderByAsc(TicketComment::getCreateTime);
        List<TicketComment> comments = commentMapper.selectList(wrapper);

        String commentContext = comments.stream()
                .map(c -> String.format("[%s] %s: %s", c.getCreateTime(), c.getCreatorName(), c.getContent()))
                .collect(Collectors.joining("\n"));

        String systemPrompt = "你是一个工单摘要助手。请根据工单信息和对话记录，生成一段简洁的摘要（不超过200字）。";

        String userMessage = """
                工单号: %s
                标题: %s
                状态: %s
                描述: %s

                对话记录:
                %s
                """.formatted(ticket.getTicketNo(), ticket.getTitle(), ticket.getStatus(),
                ticket.getDescription() != null ? ticket.getDescription() : "无",
                commentContext.isEmpty() ? "暂无对话" : commentContext);

        AiRequest request = AiRequest.builder()
                .addMessage(AiMessage.system(systemPrompt))
                .addMessage(AiMessage.user(userMessage))
                .temperature(0.5)
                .maxTokens(400)
                .build();

        AiResponse response = aiClient.chat(request);
        return response.content();
    }

    @Override
    public List<String> suggestReply(Long ticketId) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new RuntimeException("工单不存在: " + ticketId);
        }

        LambdaQueryWrapper<TicketComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TicketComment::getTicketId, ticketId)
                .orderByDesc(TicketComment::getCreateTime)
                .last("LIMIT 10");
        List<TicketComment> recentComments = commentMapper.selectList(wrapper);

        String commentContext = recentComments.stream()
                .map(c -> String.format("[%s] %s: %s", c.getType(), c.getCreatorName(), c.getContent()))
                .collect(Collectors.joining("\n"));

        String systemPrompt = """
                你是一个工单回复助手。根据工单信息和最近的对话记录，推荐3条合适的回复。
                回复应专业、简洁、有帮助。

                请严格以JSON数组格式回复: ["回复1", "回复2", "回复3"]
                只返回JSON数组，不要有其他内容。
                """;

        String userMessage = """
                工单标题: %s
                工单状态: %s
                工单描述: %s

                最近对话:
                %s
                """.formatted(ticket.getTitle(), ticket.getStatus(),
                ticket.getDescription() != null ? ticket.getDescription() : "无",
                commentContext.isEmpty() ? "暂无对话" : commentContext);

        AiRequest request = AiRequest.builder()
                .addMessage(AiMessage.system(systemPrompt))
                .addMessage(AiMessage.user(userMessage))
                .temperature(0.7)
                .maxTokens(600)
                .build();

        AiResponse response = aiClient.chat(request);
        return parseReplySuggestions(response.content());
    }

    private TicketClassifyResult parseClassifyResult(String content, List<TicketCategory> categories) {
        try {
            String json = extractJson(content);
            JsonNode node = objectMapper.readTree(json);
            long categoryId = node.get("categoryId").asLong();
            String categoryName = node.get("categoryName").asText();
            double confidence = node.has("confidence") ? node.get("confidence").asDouble() : 0.8;
            String reasoning = node.has("reasoning") ? node.get("reasoning").asText() : "";

            // 验证分类ID是否有效
            boolean valid = categories.stream().anyMatch(c -> c.getId().equals(categoryId));
            if (!valid && !categories.isEmpty()) {
                TicketCategory fallback = categories.getFirst();
                return new TicketClassifyResult(fallback.getId(), fallback.getName(), 0.3, "AI 分类ID无效，使用默认分类");
            }
            return new TicketClassifyResult(categoryId, categoryName, confidence, reasoning);
        } catch (Exception e) {
            log.warn("解析AI分类结果失败: {}", content, e);
            return new TicketClassifyResult(null, null, 0.0, "AI 分类结果解析失败");
        }
    }

    private List<String> parseReplySuggestions(String content) {
        try {
            String json = extractJson(content);
            return objectMapper.readValue(json, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            log.warn("解析AI推荐回复失败: {}", content, e);
            List<String> fallback = new ArrayList<>();
            fallback.add("感谢您的反馈，我们正在处理中。");
            fallback.add("已收到您的问题，我们会尽快为您解决。");
            fallback.add("请提供更多详细信息，以便我们更好地协助您。");
            return fallback;
        }
    }

    private String extractJson(String content) {
        String trimmed = content.trim();
        int start = trimmed.indexOf('{');
        int startArr = trimmed.indexOf('[');
        if (start == -1 && startArr == -1) {
            return trimmed;
        }
        if (startArr != -1 && (start == -1 || startArr < start)) {
            int end = trimmed.lastIndexOf(']');
            return end > startArr ? trimmed.substring(startArr, end + 1) : trimmed;
        }
        int end = trimmed.lastIndexOf('}');
        return end > start ? trimmed.substring(start, end + 1) : trimmed;
    }
}
