package com.basebackend.notification.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.common.model.Result;
import com.basebackend.feign.client.UserFeignClient;
import com.basebackend.observability.metrics.CustomMetrics;
import com.basebackend.notification.constants.NotificationConstants;
import com.basebackend.notification.dto.CreateNotificationDTO;
import com.basebackend.notification.dto.NotificationMessageDTO;
import com.basebackend.notification.dto.NotificationQueryDTO;
import com.basebackend.notification.dto.UserNotificationDTO;
import com.basebackend.notification.entity.UserNotification;
import com.basebackend.notification.mapper.UserNotificationMapper;
import com.basebackend.notification.service.NotificationService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知服务实现类
 *
 * @author Claude Code
 * @since 2025-10-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final UserNotificationMapper notificationMapper;
    @Lazy
    private final UserFeignClient userFeignClient;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final CustomMetrics customMetrics;
    private final RocketMQTemplate rocketMQTemplate;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void sendEmailNotification(String to, String subject, String content) {
        log.info("发送邮件通知: to={}, subject={}", to, subject);
        customMetrics.recordBusinessOperation("notification", "send_email");

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true); // true表示HTML格式

            mailSender.send(message);
            log.info("邮件发送成功: to={}", to);
        } catch (Exception e) {
            log.error("邮件发送失败: to={}, error={}", to, e.getMessage(), e);
            throw new BusinessException("邮件发送失败: " + e.getMessage());
        }
    }

    @Override
    public void sendEmailByTemplate(String to, String templateCode, Object variables) {
        log.info("使用模板发送邮件: to={}, templateCode={}", to, templateCode);

        try {
            // 创建模板上下文
            Context context = new Context();
            context.setVariable("data", variables);

            // 渲染模板
            String content = templateEngine.process("email/" + templateCode, context);

            // 获取主题（这里简化处理，实际应该从数据库模板表获取）
            String subject = getSubjectByTemplateCode(templateCode);

            sendEmailNotification(to, subject, content);
        } catch (Exception e) {
            log.error("模板邮件发送失败: to={}, templateCode={}, error={}", to, templateCode, e.getMessage(), e);
            throw new BusinessException("模板邮件发送失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createSystemNotification(CreateNotificationDTO dto) {
        log.info("创建系统通知: {}", dto);
        customMetrics.recordBusinessOperation("notification", "create_system");

        // 如果未指定用户ID，则发送给所有活跃用户
        if (dto.getUserId() == null) {
            createBroadcastNotification(dto);
            return;
        }

        // 单用户通知
        UserNotification notification = new UserNotification();
        BeanUtil.copyProperties(dto, notification);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());

        int result = notificationMapper.insert(notification);
        if (result <= 0) {
            throw new BusinessException("创建系统通知失败");
        }

        log.info("系统通知创建成功: notificationId={}", notification.getId());

        // 发送消息到 RocketMQ
        sendNotificationToMQ(notification);
    }

    /**
     * 创建群发通知（发送给所有活跃用户）
     *
     * @param dto 通知创建DTO
     */
    private void createBroadcastNotification(CreateNotificationDTO dto) {
        log.info("创建群发通知: title={}", dto.getTitle());
        customMetrics.recordBusinessOperation("notification", "broadcast");

        // 获取所有活跃用户ID
        Result<List<Long>> result = userFeignClient.getAllActiveUserIds();
        if (result == null || result.getCode() != 200 || result.getData() == null) {
            log.error("获取活跃用户列表失败: {}", result != null ? result.getMessage() : "null");
            throw new BusinessException("获取用户列表失败，无法发送群发通知");
        }

        List<Long> userIds = result.getData();
        if (userIds.isEmpty()) {
            log.warn("没有活跃用户，跳过群发通知");
            return;
        }

        log.info("群发通知目标用户数: {}", userIds.size());

        // 批量创建通知
        LocalDateTime now = LocalDateTime.now();
        int batchSize = 500;  // 每批处理500条
        int totalInserted = 0;

        for (int i = 0; i < userIds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, userIds.size());
            List<Long> batchUserIds = userIds.subList(i, endIndex);

            List<UserNotification> notifications = batchUserIds.stream()
                    .map(userId -> {
                        UserNotification notification = new UserNotification();
                        notification.setUserId(userId);
                        notification.setTitle(dto.getTitle());
                        notification.setContent(dto.getContent());
                        notification.setType(dto.getType() != null ? dto.getType() : "announcement");
                        notification.setLevel(dto.getLevel() != null ? dto.getLevel() : "info");
                        notification.setLinkUrl(dto.getLinkUrl());
                        notification.setIsRead(0);
                        notification.setCreateTime(now);
                        return notification;
                    })
                    .collect(Collectors.toList());

            // 批量插入
            for (UserNotification notification : notifications) {
                notificationMapper.insert(notification);
                totalInserted++;

                // 发送MQ消息（异步推送实时通知）
                try {
                    sendNotificationToMQ(notification);
                } catch (Exception e) {
                    // MQ发送失败不影响主流程
                    log.warn("群发通知MQ消息发送失败: userId={}, error={}", notification.getUserId(), e.getMessage());
                }
            }

            log.debug("群发通知批次完成: batch={}/{}, inserted={}", 
                    (i / batchSize) + 1, (userIds.size() + batchSize - 1) / batchSize, notifications.size());
        }

        log.info("群发通知创建完成: 总用户数={}, 成功插入={}", userIds.size(), totalInserted);
    }

    @Override
    public List<UserNotificationDTO> getCurrentUserNotifications(Integer limit) {
        log.info("获取当前用户通知列表: limit={}", limit);
        customMetrics.recordBusinessOperation("notification", "get_list");

        Long currentUserId = UserContextHolder.getUserId();

        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getUserId, currentUserId)
               .orderByDesc(UserNotification::getCreateTime)
               .last(limit != null && limit > 0 ? "LIMIT " + limit : "LIMIT 50");

        List<UserNotification> notifications = notificationMapper.selectList(wrapper);
        return notifications.stream()
                .map(notification -> BeanUtil.copyProperties(notification, UserNotificationDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public Long getUnreadCount() {
        log.debug("获取当前用户未读通知数量");

        Long currentUserId = UserContextHolder.getUserId();

        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getUserId, currentUserId)
               .eq(UserNotification::getIsRead, 0);

        return notificationMapper.selectCount(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long notificationId) {
        log.info("标记通知为已读: notificationId={}", notificationId);
        customMetrics.recordBusinessOperation("notification", "mark_read");

        Long currentUserId = UserContextHolder.getUserId();

        // 验证通知归属
        UserNotification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new BusinessException("通知不存在");
        }
        if (!notification.getUserId().equals(currentUserId)) {
            throw new BusinessException("无权限操作此通知");
        }

        LambdaUpdateWrapper<UserNotification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserNotification::getId, notificationId)
               .set(UserNotification::getIsRead, 1)
               .set(UserNotification::getReadTime, LocalDateTime.now());

        int result = notificationMapper.update(null, wrapper);
        if (result <= 0) {
            throw new BusinessException("标记已读失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(List<Long> notificationIds) {
        log.info("批量标记已读: count={}", notificationIds.size());
        customMetrics.recordBusinessOperation("notification", "mark_all_read");

        Long currentUserId = UserContextHolder.getUserId();

        LambdaUpdateWrapper<UserNotification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserNotification::getUserId, currentUserId)
               .in(UserNotification::getId, notificationIds)
               .set(UserNotification::getIsRead, 1)
               .set(UserNotification::getReadTime, LocalDateTime.now());

        notificationMapper.update(null, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotification(Long notificationId) {
        log.info("删除通知: notificationId={}", notificationId);
        customMetrics.recordBusinessOperation("notification", "delete");

        Long currentUserId = UserContextHolder.getUserId();

        // 验证通知归属
        UserNotification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new BusinessException("通知不存在");
        }
        if (!notification.getUserId().equals(currentUserId)) {
            throw new BusinessException("无权限操作此通知");
        }

        int result = notificationMapper.deleteById(notificationId);
        if (result <= 0) {
            throw new BusinessException("删除通知失败");
        }
    }


    /**
     * 根据模板编码获取邮件主题
     */
    private String getSubjectByTemplateCode(String templateCode) {
        // 简化处理，实际应该从数据库查询
        switch (templateCode) {
            case "welcome":
                return "欢迎加入系统";
            case "password_changed":
                return "您的密码已修改";
            case "profile_updated":
                return "资料更新通知";
            default:
                return "系统通知";
        }
    }

    @Override
    public Page<UserNotificationDTO> getNotificationPage(NotificationQueryDTO queryDTO) {
        log.info("分页查询通知列表: {}", queryDTO);
        customMetrics.recordBusinessOperation("notification", "page_query");

        Long currentUserId = UserContextHolder.getUserId();

        // 构建分页对象
        Page<UserNotification> page = new Page<>(queryDTO.getPage(), queryDTO.getPageSize());

        // 构建查询条件
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getUserId, currentUserId);

        // 类型筛选
        if (StrUtil.isNotBlank(queryDTO.getType()) && !"all".equals(queryDTO.getType())) {
            wrapper.eq(UserNotification::getType, queryDTO.getType());
        }

        // 级别筛选
        if (StrUtil.isNotBlank(queryDTO.getLevel()) && !"all".equals(queryDTO.getLevel())) {
            wrapper.eq(UserNotification::getLevel, queryDTO.getLevel());
        }

        // 已读状态筛选
        if (StrUtil.isNotBlank(queryDTO.getIsRead()) && !"all".equals(queryDTO.getIsRead())) {
            wrapper.eq(UserNotification::getIsRead, Integer.parseInt(queryDTO.getIsRead()));
        }

        // 关键词搜索
        if (StrUtil.isNotBlank(queryDTO.getKeyword())) {
            wrapper.and(w -> w.like(UserNotification::getTitle, queryDTO.getKeyword())
                    .or()
                    .like(UserNotification::getContent, queryDTO.getKeyword()));
        }

        // 按创建时间倒序
        wrapper.orderByDesc(UserNotification::getCreateTime);

        // 执行查询
        Page<UserNotification> notificationPage = notificationMapper.selectPage(page, wrapper);

        // 转换为 DTO
        Page<UserNotificationDTO> resultPage = new Page<>(notificationPage.getCurrent(), notificationPage.getSize(), notificationPage.getTotal());
        List<UserNotificationDTO> dtoList = notificationPage.getRecords().stream()
                .map(notification -> BeanUtil.copyProperties(notification, UserNotificationDTO.class))
                .collect(Collectors.toList());
        resultPage.setRecords(dtoList);

        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteNotifications(List<Long> notificationIds) {
        log.info("批量删除通知: count={}", notificationIds.size());
        customMetrics.recordBusinessOperation("notification", "batch_delete");

        if (notificationIds == null || notificationIds.isEmpty()) {
            throw new BusinessException("通知ID列表不能为空");
        }

        Long currentUserId = UserContextHolder.getUserId();

        // 验证所有通知归属
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getUserId, currentUserId)
                .in(UserNotification::getId, notificationIds);

        long count = notificationMapper.selectCount(wrapper);
        if (count != notificationIds.size()) {
            throw new BusinessException("部分通知不存在或无权限操作");
        }

        // 批量删除
        int result = notificationMapper.deleteBatchIds(notificationIds);
        log.info("批量删除完成: 删除数量={}", result);
    }

    /**
     * 发送通知消息到 RocketMQ
     *
     * @param notification 通知实体
     */
    private void sendNotificationToMQ(UserNotification notification) {
        try {
            // 构建消息 DTO
            NotificationMessageDTO messageDTO = NotificationMessageDTO.builder()
                    .id(notification.getId())
                    .userId(notification.getUserId())
                    .title(notification.getTitle())
                    .content(notification.getContent())
                    .type(notification.getType())
                    .level(notification.getLevel())
                    .linkUrl(notification.getLinkUrl())
                    .extraData(notification.getExtraData())
                    .createTime(notification.getCreateTime().format(DATE_TIME_FORMATTER))
                    .build();

            // 根据类型确定 Tag
            String tag = getTagByType(notification.getType());

            // 构建目的地 (topic:tag)
            String destination = NotificationConstants.NOTIFICATION_TOPIC + ":" + tag;

            // 构建消息
            String payload = JSON.toJSONString(messageDTO);
            org.springframework.messaging.Message<String> message = MessageBuilder
                    .withPayload(payload)
                    .setHeader("notificationId", notification.getId())
                    .setHeader("userId", notification.getUserId())
                    .build();

            // 发送消息
            SendResult sendResult = rocketMQTemplate.syncSend(destination, message);

            log.info("通知消息发送成功: notificationId={}, msgId={}, topic={}, tag={}",
                    notification.getId(), sendResult.getMsgId(), NotificationConstants.NOTIFICATION_TOPIC, tag);

        } catch (Exception e) {
            log.error("通知消息发送失败: notificationId={}, error={}",
                    notification.getId(), e.getMessage(), e);
            // 这里不抛异常，避免影响主流程
            // 即使 MQ 发送失败，数据库已保存，用户仍然可以通过轮询获取通知
        }
    }

    /**
     * 根据通知类型获取 RocketMQ Tag
     *
     * @param type 通知类型
     * @return Tag
     */
    private String getTagByType(String type) {
        if (type == null) {
            return NotificationConstants.TAG_SYSTEM;
        }

        switch (type.toLowerCase()) {
            case "announcement":
                return NotificationConstants.TAG_ANNOUNCEMENT;
            case "reminder":
                return NotificationConstants.TAG_REMINDER;
            case "system":
            default:
                return NotificationConstants.TAG_SYSTEM;
        }
    }
}
