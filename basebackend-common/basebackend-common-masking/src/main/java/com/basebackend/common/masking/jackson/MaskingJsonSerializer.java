package com.basebackend.common.masking.jackson;

import com.basebackend.common.masking.Mask;
import com.basebackend.common.masking.MaskType;
import com.basebackend.common.masking.MaskingStrategy;
import com.basebackend.common.masking.MaskingStrategyRegistry;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;

public class MaskingJsonSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private MaskType maskType;
    private char maskChar;

    public MaskingJsonSerializer() {
    }

    public MaskingJsonSerializer(MaskType maskType, char maskChar) {
        this.maskType = maskType;
        this.maskChar = maskChar;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null || maskType == null || maskType == MaskType.NONE) {
            gen.writeString(value);
            return;
        }
        MaskingStrategyRegistry registry = MaskingStrategyRegistryHolder.getRegistry();
        if (registry == null) {
            gen.writeString(value);
            return;
        }
        MaskingStrategy strategy = registry.get(maskType);
        if (strategy == null) {
            gen.writeString(value);
            return;
        }
        gen.writeString(strategy.mask(value, maskChar));
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        if (property == null) {
            return this;
        }
        Mask mask = property.getAnnotation(Mask.class);
        if (mask == null) {
            mask = property.getContextAnnotation(Mask.class);
        }
        if (mask != null) {
            return new MaskingJsonSerializer(mask.value(), mask.maskChar());
        }
        return this;
    }
}
