package com.basebackend.common.idempotent.token;

import com.basebackend.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 幂等 Token 控制器
 * <p>
 * 暴露端点供前端获取幂等 Token。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/idempotent")
@RequiredArgsConstructor
public class IdempotentTokenController {

    private final IdempotentTokenService idempotentTokenService;

    /**
     * 获取幂等 Token
     *
     * @return 幂等 Token
     */
    @GetMapping("/token")
    public Result<String> getToken() {
        return Result.success(idempotentTokenService.generateToken());
    }
}
