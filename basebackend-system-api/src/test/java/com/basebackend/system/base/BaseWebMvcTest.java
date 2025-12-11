package com.basebackend.system.base;

import com.basebackend.system.testutil.FixtureFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * WebMvc测试基类
 * <p>
 * 用于@Controller层的WebMvc测试。
 * </p>
 */
@AutoConfigureMockMvc
public abstract class BaseWebMvcTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected final FixtureFactory fixtures = FixtureFactory.standard();
}
