/*
 * Decompiled with CFR 0.152.
 */
package com.basebackend.nacos.exception;

public class NacosInitializationException
extends RuntimeException {
    public NacosInitializationException(String message) {
        super(message);
    }

    public NacosInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}

