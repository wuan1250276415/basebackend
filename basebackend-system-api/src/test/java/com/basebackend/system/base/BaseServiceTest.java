package com.basebackend.system.base;

import com.basebackend.system.testutil.FixtureFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 服务层测试基类
 * <p>
 * 提供通用的测试工具和Mock支持。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseServiceTest {

    protected final FixtureFactory fixtures = FixtureFactory.standard();
}
