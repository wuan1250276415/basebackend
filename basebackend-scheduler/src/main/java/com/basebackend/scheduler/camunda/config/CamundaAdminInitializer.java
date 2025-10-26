package com.basebackend.scheduler.camunda.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;
import org.springframework.boot.CommandLineRunner;

/**
 * Camunda管理员初始化器
 */
@Slf4j
@RequiredArgsConstructor
public class CamundaAdminInitializer implements CommandLineRunner {

    private final ProcessEngine processEngine;
    private final CamundaProperties camundaProperties;

    @Override
    public void run(String... args) {
        CamundaProperties.Admin admin = camundaProperties.getAdmin();

        try {
            // 检查管理员用户是否存在
            User existingUser = processEngine.getIdentityService()
                    .createUserQuery()
                    .userId(admin.getId())
                    .singleResult();

            if (existingUser == null) {
                // 创建管理员用户
                User newUser = processEngine.getIdentityService().newUser(admin.getId());
                newUser.setPassword(admin.getPassword());
                newUser.setFirstName(admin.getFirstName());
                newUser.setLastName(admin.getLastName());
                newUser.setEmail(admin.getEmail());
                processEngine.getIdentityService().saveUser(newUser);
                log.info("Camunda 管理员用户创建成功: {}", admin.getId());

                // 检查camunda-admin组是否存在
                Group adminGroup = processEngine.getIdentityService()
                        .createGroupQuery()
                        .groupId("camunda-admin")
                        .singleResult();

                if (adminGroup == null) {
                    // 创建管理员组
                    adminGroup = processEngine.getIdentityService().newGroup("camunda-admin");
                    adminGroup.setName("Camunda Administrators");
                    adminGroup.setType("SYSTEM");
                    processEngine.getIdentityService().saveGroup(adminGroup);
                    log.info("Camunda 管理员组创建成功");
                }

                // 将用户添加到管理员组
                processEngine.getIdentityService()
                        .createMembership(admin.getId(), "camunda-admin");
                log.info("管理员用户已添加到 camunda-admin 组");
            } else {
                log.info("Camunda 管理员用户已存在: {}", admin.getId());
            }
        } catch (Exception e) {
            log.error("初始化 Camunda 管理员失败", e);
        }
    }
}
