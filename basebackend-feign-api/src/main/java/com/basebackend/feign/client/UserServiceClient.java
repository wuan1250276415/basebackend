package com.basebackend.feign.client;

import com.basebackend.common.model.Result;
import com.basebackend.feign.config.FileFeignConfig;
import com.basebackend.feign.constant.FeignServiceConstants;
import com.basebackend.feign.dto.user.LoginResponse;
import com.basebackend.feign.fallback.FileFeignFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 用户服务Feign客户端
 */
@FeignClient(
        name = FeignServiceConstants.USER_SERVICE,
        contextId = "UserServiceClient",
        path = "/api/user/auth"
)
public interface UserServiceClient {

    /**
     * 微信单点登录
     * 根据手机号查询用户，如果存在则返回token，如果不存在则创建用户后返回token
     *
     * @param phone 手机号
     * @return 登录响应信息（包含token和用户信息）
     */
    @PostMapping("/auth/wechat-login")
    Result<LoginResponse> wechatLogin(@RequestParam("phone") String phone);
}
