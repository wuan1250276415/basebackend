package com.basebackend.ticket.controller;

import com.basebackend.security.annotation.RequiresPermission;
import com.basebackend.ticket.realtime.TicketRealtimeController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class TicketPermissionAnnotationsTest {

    @Test
    @DisplayName("审批查询接口应声明权限注解")
    void approvalQueryEndpointsShouldRequirePermission() throws Exception {
        Method listMethod = TicketApprovalController.class.getDeclaredMethod("list", Long.class);
        Method tasksMethod = TicketApprovalController.class.getDeclaredMethod("activeTasks", Long.class);

        RequiresPermission listPermission = listMethod.getAnnotation(RequiresPermission.class);
        RequiresPermission tasksPermission = tasksMethod.getAnnotation(RequiresPermission.class);

        assertThat(listPermission).isNotNull();
        assertThat(tasksPermission).isNotNull();
    }

    @Test
    @DisplayName("实时订阅接口应声明权限注解")
    void realtimeEndpointsShouldRequirePermission() throws Exception {
        Method subscribeMethod = TicketRealtimeController.class.getDeclaredMethod("subscribe", Long.class);
        Method unsubscribeMethod = TicketRealtimeController.class.getDeclaredMethod("unsubscribe", Long.class);

        RequiresPermission subscribePermission = subscribeMethod.getAnnotation(RequiresPermission.class);
        RequiresPermission unsubscribePermission = unsubscribeMethod.getAnnotation(RequiresPermission.class);

        assertThat(subscribePermission).isNotNull();
        assertThat(unsubscribePermission).isNotNull();
    }
}
