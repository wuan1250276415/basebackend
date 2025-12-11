package com.basebackend.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.basebackend.system.base.BaseServiceTest;
import com.basebackend.system.dto.ApplicationDTO;
import com.basebackend.system.entity.SysApplication;
import com.basebackend.system.mapper.SysApplicationMapper;
import com.basebackend.system.service.impl.ApplicationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;

/**
 * 应用管理服务测试
 */
@DisplayName("ApplicationService 应用管理服务测试")
class ApplicationServiceTest extends BaseServiceTest {

    @Mock
    private SysApplicationMapper applicationMapper;

    private ApplicationService applicationService;

    @BeforeEach
    void setUp() {
        applicationService = new ApplicationServiceImpl(applicationMapper);
    }

    @Test
    @DisplayName("listApplications - 应返回应用列表")
    void shouldReturnApplicationList() {
        // Given
        SysApplication app1 = createSysApplication(1L, "应用1", "APP1", "web", 1);
        given(applicationMapper.selectList(any())).willReturn(Arrays.asList(app1));

        // When
        var result = applicationService.listApplications();

        // Then
        assertThat(result).hasSize(1);
        verify(applicationMapper).selectList(any());
    }

    private SysApplication createSysApplication(Long id, String name, String code, String type, Integer status) {
        SysApplication app = new SysApplication();
        app.setId(id);
        app.setAppName(name);
        app.setAppCode(code);
        app.setAppType(type);
        app.setStatus(status);
        app.setDeleted(0);
        return app;
    }
}
