package com.basebackend.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.basebackend.admin.dto.notification.CreateNotificationDTO;
import com.basebackend.admin.dto.notification.UserNotificationDTO;
import com.basebackend.admin.entity.SysUser;
import com.basebackend.admin.entity.UserNotification;
import com.basebackend.admin.mapper.SysUserMapper;
import com.basebackend.admin.mapper.UserNotificationMapper;
import com.basebackend.admin.service.NotificationService;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.observability.metrics.CustomMetrics;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
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
    private final SysUserMapper userMapper;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final CustomMetrics customMetrics;

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

        UserNotification notification = new UserNotification();
        BeanUtil.copyProperties(dto, notification);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());

        // 如果未指定用户ID，则发送给所有用户（这里简化处理，实际可能需要批量插入）
        if (dto.getUserId() == null) {
            // TODO: 实现群发逻辑
            throw new BusinessException("暂不支持群发通知");
        }

        int result = notificationMapper.insert(notification);
        if (result <= 0) {
            throw new BusinessException("创建系统通知失败");
        }

        log.info("系统通知创建成功: notificationId={}", notification.getId());
    }

    @Override
    public List<UserNotificationDTO> getCurrentUserNotifications(Integer limit) {
        log.info("获取当前用户通知列表: limit={}", limit);
        customMetrics.recordBusinessOperation("notification", "get_list");

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
        customMetrics.recordBusinessOperation("notification", "mark_read");

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
        customMetrics.recordBusinessOperation("notification", "mark_all_read");

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
        customMetrics.recordBusinessOperation("notification", "delete");

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

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("未登录或登录已过期");
        }

        String username = authentication.getName();
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        SysUser user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        return user.getId();
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
}
