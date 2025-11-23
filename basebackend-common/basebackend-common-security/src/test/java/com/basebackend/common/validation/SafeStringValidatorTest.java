package com.basebackend.common.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SafeStringValidatorTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldPassWithSafeContent() {
        SampleBean bean = new SampleBean();
        bean.value = "Normal_Text-123";

        assertThat(validator.validate(bean)).isEmpty();
    }

    @Test
    void shouldFailWithUnsafeContent() {
        SampleBean bean = new SampleBean();
        bean.value = "<script>alert(1)</script>";

        assertThat(validator.validate(bean)).isNotEmpty();
    }

    @Test
    void shouldFailWhenExceedingLength() {
        SampleBean bean = new SampleBean();
        bean.value = "a".repeat(102);

        assertThat(validator.validate(bean)).isNotEmpty();
    }

    @Test
    void requiredFieldShouldRejectBlank() {
        RequiredBean bean = new RequiredBean();

        assertThat(validator.validate(bean)).isNotEmpty();
    }

    private static class SampleBean {
        @SafeString(maxLength = 100)
        private String value;
    }

    private static class RequiredBean {
        @SafeString(required = true)
        @NotBlank
        private String value;
    }
}
