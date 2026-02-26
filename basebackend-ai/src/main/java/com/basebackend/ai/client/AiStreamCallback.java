package com.basebackend.ai.client;

/**
 * 流式回调接口
 */
public interface AiStreamCallback {

    /** 收到一个文本片段 */
    void onToken(String token);

    /** 流式完成 */
    default void onComplete(AiResponse response) {}

    /** 发生错误 */
    default void onError(Throwable throwable) {}
}
