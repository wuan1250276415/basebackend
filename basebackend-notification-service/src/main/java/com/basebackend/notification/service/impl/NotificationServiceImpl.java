package com.basebackend.notification.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.common.exception.BusinessException;
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
 * @author BaseBackend Team
 * @since 2025-11-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final UserNotificationMapper notificationMapper;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final RocketMQTemplate rocketMQTemplate;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void sendEmailNotification(String to, String subject, String content) {
        log.info("发送邮件通知: to={}, subject={}", to, subject);

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

            // 获取主题
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

        UserNotification notification = new UserNotification();
        BeanUtil.copyProperties(dto, notification);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());

        // 如果未指定用户ID，则发送给所有用户（这里简化处理）
        if (dto.getUserId() == null) {
            throw new BusinessException("暂不支持群发通知");
        }

        int result = notificationMapper.insert(notification);
        if (result <= 0) {
            throw new BusinessException("创建系统通知失败");
        }

        log.info("系统通知创建成功: notificationId={}", notification.getId());

        // 发送消息到 RocketMQ
        sendNotificationToMQ(notification);
    }

    @Override
    public List<UserNotificationDTO> getCurrentUserNotifications(Integer limit) {
        log.info("获取当前用户通知列表: limit={}", limit);

        Long currentUserId = getCurrentUserId();

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

        Long currentUserId = getCurrentUserId();

        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getUserId, currentUserId)
               .eq(UserNotification::getIsRead, 0);

        return notificationMapper.selectCount(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long notificationId) {
        log.info("标记通知为已读: notificationId={}", notificationId);

        Long currentUserId = getCurrentUserId();

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

        Long currentUserId = getCurrentUserId();

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

        Long currentUserId = getCurrentUserId();

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

    @Override
    public Page<UserNotificationDTO> getNotificationPage(NotificationQueryDTO queryDTO) {
        log.info("分页查询通知列表: {}", queryDTO);

        Long currentUserId = getCurrentUserId();

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

        if (notificationIds == null || notificationIds.isEmpty()) {
            throw new BusinessException("通知ID列表不能为空");
        }

        Long currentUserId = getCurrentUserId();

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
     * 获取当前登录用户ID
     * TODO: 从JWT或请求头获取用户ID
     */
    private Long getCurrentUserId() {
        // 简化处理，实际应该从JWT token或请求头获取
        // 这里暂时返回模拟的用户ID
        return 1L;
    }

    /**
     * 根据模板编码获取邮件主题
     */
    private String getSubjectByTemplateCode(String templateCode) {
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

    /**
     * 发送通知消息到 RocketMQ
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
        }
    }

    /**
     * 根据通知类型获取 RocketMQ Tag
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
