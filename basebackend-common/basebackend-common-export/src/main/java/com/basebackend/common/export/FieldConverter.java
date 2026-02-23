package com.basebackend.common.export;

@FunctionalInterface
public interface FieldConverter {

    String convert(Object value);
}
