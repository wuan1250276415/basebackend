package com.basebackend.database.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.basebackend.common.context.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis Plus 自动填充处理器
 */
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        String entityName = metaObject.getOriginalObject().getClass().getSimpleName();
        log.debug("开始插入填充, 实体: {}", entityName);

        LocalDateTime now = LocalDateTime.now();
        Long currentUserId = getCurrentUserId();

        // 填充创建时间
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);

        // 填充更新时间
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);

        // 填充创建人（从上下文获取，这里简化处理）
        this.strictInsertFill(metaObject, "createBy", Long.class, currentUserId);

        // 填充更新人
        this.strictInsertFill(metaObject, "updateBy", Long.class, currentUserId);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        String entityName = metaObject.getOriginalObject().getClass().getSimpleName();
        log.debug("开始更新填充, 实体: {}", entityName);

        // 填充更新时间
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

        // 填充更新人
        this.strictUpdateFill(metaObject, "updateBy", Long.class, getCurrentUserId());
    }

    /**
     * 获取当前用户ID，从线程上下文中读取；未登录场景（定时任务/初始化）返回 0。
     */
    private Long getCurrentUserId() {
        Long userId = UserContextHolder.getUserId();
        return userId != null ? userId : 0L;
    }
}
