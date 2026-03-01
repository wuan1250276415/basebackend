package com.basebackend.ticket.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.event.DomainEventPublisher;
import com.basebackend.common.lock.annotation.DistributedLock;
import com.basebackend.ticket.dto.*;
import com.basebackend.ticket.entity.*;
import com.basebackend.ticket.enums.CommentType;
import com.basebackend.ticket.enums.TicketSource;
import com.basebackend.ticket.enums.TicketStatus;
import com.basebackend.ticket.event.TicketAssignedEvent;
import com.basebackend.ticket.event.TicketCreatedEvent;
import com.basebackend.ticket.event.TicketStatusChangedEvent;
import com.basebackend.ticket.mapper.*;
import com.basebackend.ticket.service.TicketService;
import com.basebackend.ticket.util.AuditHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 工单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketMapper ticketMapper;
    private final TicketCategoryMapper categoryMapper;
    private final TicketCommentMapper commentMapper;
    private final TicketAttachmentMapper attachmentMapper;
    private final TicketStatusLogMapper statusLogMapper;
    private final TicketApprovalMapper approvalMapper;
    private final TicketCcMapper ccMapper;
    private final AuditHelper auditHelper;
    private final StringRedisTemplate redisTemplate;
    private final DomainEventPublisher eventPublisher;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    @Override
    @Transactional
    public Ticket create(TicketCreateDTO dto) {
        log.info("创建工单: title={}", dto.title());

        Ticket ticket = new Ticket();
        BeanUtil.copyProperties(dto, ticket);

        // 生成工单编号
        ticket.setTicketNo(generateTicketNo());

        // 默认值
        ticket.setStatus(TicketStatus.OPEN.name());
        if (ticket.getSource() == null) {
            ticket.setSource(TicketSource.WEB.name());
        }
        if (ticket.getPriority() == null) {
            ticket.setPriority(3); // 默认中优先级
        }
        if (ticket.getReporterId() == null) {
            ticket.setReporterId(UserContextHolder.getUserId());
            ticket.setReporterName(UserContextHolder.getNickname());
        }
        if (ticket.getDeptId() == null) {
            ticket.setDeptId(UserContextHolder.getDeptId());
        }

        ticket.setCommentCount(0);
        ticket.setAttachmentCount(0);
        ticket.setSlaBreached(0);

        // 计算 SLA 截止时间
        TicketCategory category = categoryMapper.selectById(dto.categoryId());
        if (category != null && category.getSlaHours() != null) {
            ticket.setSlaDeadline(LocalDateTime.now().plusHours(category.getSlaHours()));
        }

        // 审计字段
        auditHelper.setCreateAuditFields(ticket);

        ticketMapper.insert(ticket);
        log.info("工单创建成功: ticketNo={}, id={}", ticket.getTicketNo(), ticket.getId());

        // 发布工单创建事件
        eventPublisher.publish(new TicketCreatedEvent(
                "ticket-service", ticket.getId(), ticket.getTicketNo(),
                ticket.getReporterId(), ticket.getReporterName(),
                ticket.getAssigneeId(), ticket.getAssigneeName(),
                ticket.getTitle(), ticket.getPriority()));

        return ticket;
    }

    @Override
    @Cacheable(value = "ticket", key = "#id", unless = "#result == null")
    public Ticket getById(Long id) {
        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new RuntimeException("工单不存在: " + id);
        }
        return ticket;
    }

    @Override
    @Cacheable(value = "ticket", key = "'no:' + #ticketNo", unless = "#result == null")
    public Ticket getByTicketNo(String ticketNo) {
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Ticket::getTicketNo, ticketNo);
        Ticket ticket = ticketMapper.selectOne(wrapper);
        if (ticket == null) {
            throw new RuntimeException("工单不存在: " + ticketNo);
        }
        return ticket;
    }

    @Override
    public TicketDetailVO getDetail(Long id) {
        Ticket ticket = getById(id);

        TicketDetailVO vo = new TicketDetailVO();
        BeanUtil.copyProperties(ticket, vo);

        // 分类名称
        TicketCategory category = categoryMapper.selectById(ticket.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
        }

        // 评论列表
        LambdaQueryWrapper<TicketComment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.eq(TicketComment::getTicketId, id).orderByAsc(TicketComment::getCreateTime);
        List<TicketComment> comments = commentMapper.selectList(commentWrapper);
        vo.setComments(comments.stream().map(c -> new TicketDetailVO.CommentItem(
                c.getId(), c.getContent(), c.getType(), c.getIsInternal(),
                c.getParentId(), c.getCreatorName(), c.getCreateTime()
        )).toList());

        // 附件列表
        LambdaQueryWrapper<TicketAttachment> attachWrapper = new LambdaQueryWrapper<>();
        attachWrapper.eq(TicketAttachment::getTicketId, id).orderByAsc(TicketAttachment::getCreateTime);
        List<TicketAttachment> attachments = attachmentMapper.selectList(attachWrapper);
        vo.setAttachments(attachments.stream().map(a -> new TicketDetailVO.AttachmentItem(
                a.getId(), a.getFileId(), a.getFileName(), a.getFileSize(),
                a.getFileType(), a.getFileUrl(), a.getCreateTime()
        )).toList());

        // 状态变更日志
        LambdaQueryWrapper<TicketStatusLog> logWrapper = new LambdaQueryWrapper<>();
        logWrapper.eq(TicketStatusLog::getTicketId, id).orderByDesc(TicketStatusLog::getCreateTime);
        List<TicketStatusLog> statusLogs = statusLogMapper.selectList(logWrapper);
        vo.setStatusLogs(statusLogs.stream().map(s -> new TicketDetailVO.StatusLogItem(
                s.getId(), s.getFromStatus(), s.getToStatus(),
                s.getOperatorName(), s.getRemark(), s.getCreateTime()
        )).toList());

        // 审批记录
        LambdaQueryWrapper<TicketApproval> approvalWrapper = new LambdaQueryWrapper<>();
        approvalWrapper.eq(TicketApproval::getTicketId, id).orderByDesc(TicketApproval::getCreateTime);
        List<TicketApproval> approvals = approvalMapper.selectList(approvalWrapper);
        vo.setApprovals(approvals.stream().map(a -> new TicketDetailVO.ApprovalItem(
                a.getId(), a.getTaskName(), a.getApproverName(),
                a.getAction(), a.getOpinion(), a.getDelegateToName(), a.getCreateTime()
        )).toList());

        // 抄送列表
        LambdaQueryWrapper<TicketCc> ccWrapper = new LambdaQueryWrapper<>();
        ccWrapper.eq(TicketCc::getTicketId, id).orderByDesc(TicketCc::getCreateTime);
        List<TicketCc> ccList = ccMapper.selectList(ccWrapper);
        vo.setCcList(ccList.stream().map(cc -> new TicketDetailVO.CcItem(
                cc.getId(), cc.getUserName(), cc.getIsRead(),
                cc.getReadTime(), cc.getCreateTime()
        )).toList());

        return vo;
    }

    @Override
    public IPage<TicketListVO> page(TicketQueryDTO query, Page<Ticket> page) {
        Page<TicketListVO> voPage = new Page<>(page.getCurrent(), page.getSize());
        return ticketMapper.selectTicketPage(voPage, query);
    }

    @Override
    @Transactional
    @CacheEvict(value = "ticket", key = "#id")
    public void update(Long id, TicketUpdateDTO dto) {
        Ticket ticket = getById(id);
        log.info("更新工单: id={}, ticketNo={}", id, ticket.getTicketNo());

        if (dto.title() != null) {
            ticket.setTitle(dto.title());
        }
        if (dto.description() != null) {
            ticket.setDescription(dto.description());
        }
        if (dto.categoryId() != null) {
            ticket.setCategoryId(dto.categoryId());
            // 重新计算 SLA
            TicketCategory category = categoryMapper.selectById(dto.categoryId());
            if (category != null && category.getSlaHours() != null) {
                ticket.setSlaDeadline(ticket.getCreateTime().plusHours(category.getSlaHours()));
            }
        }
        if (dto.priority() != null) {
            ticket.setPriority(dto.priority());
        }
        if (dto.tags() != null) {
            ticket.setTags(dto.tags());
        }

        auditHelper.setUpdateAuditFields(ticket);
        ticketMapper.updateById(ticket);
    }

    @Override
    @Transactional
    @CacheEvict(value = "ticket", key = "#id")
    public void changeStatus(Long id, TicketStatus toStatus, String remark) {
        Ticket ticket = getById(id);
        TicketStatus fromStatus = TicketStatus.valueOf(ticket.getStatus());

        if (!fromStatus.canTransitionTo(toStatus)) {
            throw new RuntimeException(
                    String.format("非法状态变更: %s -> %s", fromStatus.getDescription(), toStatus.getDescription()));
        }

        log.info("工单状态变更: id={}, {} -> {}", id, fromStatus, toStatus);

        ticket.setStatus(toStatus.name());

        if (toStatus == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(LocalDateTime.now());
        }
        if (toStatus == TicketStatus.CLOSED) {
            ticket.setClosedAt(LocalDateTime.now());
        }

        // 判断 SLA 是否已超时
        if (ticket.getSlaDeadline() != null && LocalDateTime.now().isAfter(ticket.getSlaDeadline())) {
            ticket.setSlaBreached(1);
        }

        auditHelper.setUpdateAuditFields(ticket);
        ticketMapper.updateById(ticket);

        // 记录状态变更日志
        TicketStatusLog statusLog = new TicketStatusLog();
        statusLog.setTenantId(ticket.getTenantId());
        statusLog.setTicketId(id);
        statusLog.setFromStatus(fromStatus.name());
        statusLog.setToStatus(toStatus.name());
        statusLog.setOperatorId(UserContextHolder.getUserId() != null ? UserContextHolder.getUserId() : 0L);
        statusLog.setOperatorName(UserContextHolder.getNickname() != null ? UserContextHolder.getNickname() : "");
        statusLog.setRemark(remark != null ? remark : "");
        statusLog.setCreateTime(LocalDateTime.now());
        statusLogMapper.insert(statusLog);

        // 发布状态变更事件
        eventPublisher.publish(new TicketStatusChangedEvent(
                "ticket-service", id, ticket.getTicketNo(),
                fromStatus.name(), toStatus.name(),
                statusLog.getOperatorId(), statusLog.getOperatorName(),
                remark));
    }

    @Override
    @Transactional
    @CacheEvict(value = "ticket", key = "#id")
    @DistributedLock(key = "'ticket:assign:' + #id", waitTime = 5, leaseTime = 30)
    public void assign(Long id, Long assigneeId, String assigneeName) {
        Ticket ticket = getById(id);
        log.info("分配工单处理人: id={}, assignee={}", id, assigneeName);

        String oldAssignee = ticket.getAssigneeName();
        ticket.setAssigneeId(assigneeId);
        ticket.setAssigneeName(assigneeName);

        // 如果是 OPEN 状态，自动转为 IN_PROGRESS
        if (TicketStatus.OPEN.name().equals(ticket.getStatus())) {
            ticket.setStatus(TicketStatus.IN_PROGRESS.name());

            TicketStatusLog statusLog = new TicketStatusLog();
            statusLog.setTenantId(ticket.getTenantId());
            statusLog.setTicketId(id);
            statusLog.setFromStatus(TicketStatus.OPEN.name());
            statusLog.setToStatus(TicketStatus.IN_PROGRESS.name());
            statusLog.setOperatorId(UserContextHolder.getUserId() != null ? UserContextHolder.getUserId() : 0L);
            statusLog.setOperatorName(UserContextHolder.getNickname() != null ? UserContextHolder.getNickname() : "");
            statusLog.setRemark("分配处理人: " + assigneeName);
            statusLog.setCreateTime(LocalDateTime.now());
            statusLogMapper.insert(statusLog);
        }

        auditHelper.setUpdateAuditFields(ticket);
        ticketMapper.updateById(ticket);

        // 发布工单分配事件
        Long operatorId = UserContextHolder.getUserId() != null ? UserContextHolder.getUserId() : 0L;
        String operatorName = UserContextHolder.getNickname() != null ? UserContextHolder.getNickname() : "";
        eventPublisher.publish(new TicketAssignedEvent(
                "ticket-service", id, ticket.getTicketNo(),
                assigneeId, assigneeName, operatorId, operatorName));
    }

    @Override
    @Transactional
    public void close(Long id, String remark) {
        changeStatus(id, TicketStatus.CLOSED, remark);
    }

    @Override
    @Transactional
    @CacheEvict(value = "ticket", key = "#id")
    public void delete(Long id) {
        Ticket ticket = getById(id);
        log.info("删除工单: id={}, ticketNo={}", id, ticket.getTicketNo());
        ticketMapper.deleteById(id);
    }

    @Override
    @DistributedLock(
            key = "'ticket:no:' + T(java.time.LocalDate).now().toString()",
            waitTime = 5, leaseTime = 10
    )
    public String generateTicketNo() {
        String dateStr = LocalDate.now().format(DATE_FMT);
        String key = "ticket:no:seq:" + dateStr;
        Long seq = redisTemplate.opsForValue().increment(key);
        if (seq != null && seq == 1L) {
            redisTemplate.expire(key, 2, TimeUnit.DAYS);
        }
        return String.format("TK-%s-%04d", dateStr, seq);
    }
}
