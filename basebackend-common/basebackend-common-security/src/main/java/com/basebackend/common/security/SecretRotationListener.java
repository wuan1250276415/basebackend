package com.basebackend.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 监听密钥轮换事件并刷新缓存
 */
@Component
@RequiredArgsConstructor
public class SecretRotationListener {

    private final SecretManager secretManager;

    @EventListener
    public void onSecretRotation(SecretRotationEvent event) {
        secretManager.refreshSecret(event.getKey());
    }
}
