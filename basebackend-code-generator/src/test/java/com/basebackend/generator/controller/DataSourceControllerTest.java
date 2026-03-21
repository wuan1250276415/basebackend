package com.basebackend.generator.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.common.model.Result;
import com.basebackend.generator.dto.GenDataSourceView;
import com.basebackend.generator.entity.GenDataSource;
import com.basebackend.generator.mapper.GenDataSourceMapper;
import com.basebackend.security.annotation.RequiresRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataSourceController 安全测试")
class DataSourceControllerTest {

    @Mock
    private GenDataSourceMapper dataSourceMapper;

    @InjectMocks
    private DataSourceController dataSourceController;

    @Test
    @DisplayName("分页查询结果不应返回明文密码")
    void shouldSanitizePasswordInPageResponse() {
        GenDataSource dataSource = buildDataSource();
        Page<GenDataSource> page = new Page<>(1, 10);
        page.setRecords(List.of(dataSource));
        page.setTotal(1);

        when(dataSourceMapper.selectPage(any(Page.class), any())).thenReturn(page);

        Result<Page<GenDataSourceView>> result = dataSourceController.page(1, 10);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getRecords()).hasSize(1);
        GenDataSourceView view = result.getData().getRecords().getFirst();
        assertThat(view.username()).isEqualTo("generator");
        assertThat(view.passwordConfigured()).isTrue();
    }

    @Test
    @DisplayName("按ID查询结果不应返回明文密码")
    void shouldSanitizePasswordInDetailResponse() {
        when(dataSourceMapper.selectById(1L)).thenReturn(buildDataSource());

        Result<GenDataSourceView> result = dataSourceController.getById(1L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().databaseName()).isEqualTo("basebackend");
        assertThat(result.getData().passwordConfigured()).isTrue();
    }

    @Test
    @DisplayName("数据源管理控制器应限制为管理员角色")
    void shouldRequireAdminRole() {
        RequiresRole annotation = DataSourceController.class.getAnnotation(RequiresRole.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.logical()).isEqualTo(RequiresRole.Logical.OR);
        assertThat(annotation.values()).containsExactlyInAnyOrder("admin", "sys_admin");
    }

    @Test
    @DisplayName("展示模型不应暴露密码字段")
    void viewModelShouldNotExposePasswordField() {
        Method[] methods = GenDataSourceView.class.getDeclaredMethods();

        assertThat(methods)
                .extracting(Method::getName)
                .doesNotContain("password");
    }

    private GenDataSource buildDataSource() {
        GenDataSource dataSource = new GenDataSource();
        dataSource.setId(1L);
        dataSource.setName("generator-ds");
        dataSource.setDbType("MYSQL");
        dataSource.setHost("127.0.0.1");
        dataSource.setPort(3306);
        dataSource.setDatabaseName("basebackend");
        dataSource.setUsername("generator");
        dataSource.setPassword("SuperSecret!");
        dataSource.setStatus(1);
        return dataSource;
    }
}
