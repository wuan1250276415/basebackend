package com.basebackend.user.util;

import com.basebackend.api.model.dept.DeptBasicDTO;
import com.basebackend.common.model.Result;
import com.basebackend.service.client.DeptServiceClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("部门信息工具测试")
class DeptInfoHelperTest {

    @Mock
    private ObjectProvider<DeptServiceClient> deptServiceClientProvider;

    @Mock
    private DeptServiceClient deptServiceClient;

    @InjectMocks
    private DeptInfoHelper deptInfoHelper;

    @Test
    @DisplayName("单个部门名称查询走批量接口")
    void testGetDeptName_UsesBatchEndpoint() {
        when(deptServiceClientProvider.getIfAvailable()).thenReturn(deptServiceClient);
        when(deptServiceClient.getBatchByIds("2"))
                .thenReturn(Result.success("查询成功", List.of(buildDept(2L, "技术部"))));

        String deptName = deptInfoHelper.getDeptName(2L);

        assertEquals("技术部", deptName);
        verify(deptServiceClient).getBatchByIds("2");
        verify(deptServiceClient, never()).getById(2L);
    }

    @Test
    @DisplayName("批量部门名称查询只发起一次远程调用")
    void testGetDeptNameBatch_SingleRemoteCall() {
        LinkedHashSet<Long> deptIds = new LinkedHashSet<>(Arrays.asList(1L, 2L));
        when(deptServiceClientProvider.getIfAvailable()).thenReturn(deptServiceClient);
        when(deptServiceClient.getBatchByIds("1,2"))
                .thenReturn(Result.success("查询成功", List.of(
                        buildDept(1L, "总公司"),
                        buildDept(2L, "技术部")
                )));

        Map<Long, String> deptNameMap = deptInfoHelper.getDeptNameBatch(deptIds);

        assertEquals("总公司", deptNameMap.get(1L));
        assertEquals("技术部", deptNameMap.get(2L));
        verify(deptServiceClient).getBatchByIds("1,2");
    }

    private DeptBasicDTO buildDept(Long id, String deptName) {
        return new DeptBasicDTO(
                id,
                0L,
                deptName,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                1,
                null,
                null,
                null,
                null
        );
    }
}
