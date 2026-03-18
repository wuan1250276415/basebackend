package com.basebackend.api.model.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API Model DTO 单元测试
 */
class ApiModelTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static Validator validator;

    @BeforeAll
    static void setup() {
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try (ValidatorFactory factory = Validation.byDefaultProvider()
                .configure()
                .messageInterpolator(new ParameterMessageInterpolator())
                .buildValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    // ========== LoginRequest ==========

    @Nested
    @DisplayName("LoginRequest")
    class LoginRequestTest {

        @Test
        @DisplayName("正常构造")
        void shouldCreate() {
            var req = new LoginRequest("admin", "123456", "abcd", "captcha-1", true);
            assertThat(req.username()).isEqualTo("admin");
            assertThat(req.password()).isEqualTo("123456");
            assertThat(req.captcha()).isEqualTo("abcd");
            assertThat(req.rememberMe()).isTrue();
        }

        @Test
        @DisplayName("JSON 序列化/反序列化往返")
        void shouldRoundTrip() throws Exception {
            var req = new LoginRequest("user1", "pwd", null, null, false);
            String json = MAPPER.writeValueAsString(req);
            var restored = MAPPER.readValue(json, LoginRequest.class);
            assertThat(restored).isEqualTo(req);
        }

        @Test
        @DisplayName("用户名为空校验失败")
        void shouldFailValidationWhenUsernameBlank() {
            var req = new LoginRequest("", "pwd", null, null, null);
            var violations = validator.validate(req);
            assertThat(violations).isNotEmpty();
            assertThat(violations.stream().anyMatch(v -> v.getMessage().contains("用户名不能为空"))).isTrue();
        }

        @Test
        @DisplayName("密码为空校验失败")
        void shouldFailValidationWhenPasswordBlank() {
            var req = new LoginRequest("admin", "", null, null, null);
            var violations = validator.validate(req);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("用户名超长校验失败")
        void shouldFailWhenUsernameTooLong() {
            var req = new LoginRequest("a".repeat(65), "pwd", null, null, null);
            var violations = validator.validate(req);
            assertThat(violations).isNotEmpty();
        }
    }

    // ========== LoginResponse ==========

    @Nested
    @DisplayName("LoginResponse")
    class LoginResponseTest {

        @Test
        @DisplayName("完整构造")
        void shouldCreate() {
            var userInfo = new LoginResponse.UserInfo(1L, "admin", "管理员",
                    "admin@test.com", "13800138000", "/avatar.png", 1, 100L, "技术部", 0, 1);
            var resp = new LoginResponse("token123", "refresh123", "Bearer", 3600L, userInfo,
                    List.of("sys:user:list"), List.of("admin"));
            assertThat(resp.accessToken()).isEqualTo("token123");
            assertThat(resp.refreshToken()).isEqualTo("refresh123");
            assertThat(resp.tokenType()).isEqualTo("Bearer");
            assertThat(resp.expiresIn()).isEqualTo(3600L);
            assertThat(resp.userInfo().username()).isEqualTo("admin");
            assertThat(resp.permissions()).containsExactly("sys:user:list");
            assertThat(resp.roles()).containsExactly("admin");
        }

        @Test
        @DisplayName("标准构造")
        void shouldDefaultBearer() {
            var resp = new LoginResponse("token", "refresh", 7200L, null, List.of(), List.of());
            assertThat(resp.accessToken()).isEqualTo("token");
            assertThat(resp.refreshToken()).isEqualTo("refresh");
            assertThat(resp.tokenType()).isEqualTo("Bearer");
            assertThat(resp.expiresIn()).isEqualTo(7200L);
        }

        @Test
        @DisplayName("JSON 序列化/反序列化往返")
        void shouldRoundTrip() throws Exception {
            var resp = new LoginResponse("tk", "rtk", "Bearer", 100L, null, List.of("p1"), List.of("r1"));
            String json = MAPPER.writeValueAsString(resp);
            var restored = MAPPER.readValue(json, LoginResponse.class);
            assertThat(restored.accessToken()).isEqualTo("tk");
            assertThat(restored.refreshToken()).isEqualTo("rtk");
            assertThat(restored.permissions()).containsExactly("p1");
        }
    }

    // ========== UserBasicDTO ==========

    @Nested
    @DisplayName("UserBasicDTO")
    class UserBasicDTOTest {

        @Test
        @DisplayName("正常构造")
        void shouldCreate() {
            var dto = new UserBasicDTO(1L, "user1", "昵称", "真名", "a@b.com",
                    "13800000000", 1, "/avatar.png", 10L, "研发部", "工程师",
                    1, List.of(1L, 2L), List.of("admin", "user"),
                    LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 2, 26, 12, 0));
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.username()).isEqualTo("user1");
            assertThat(dto.deptName()).isEqualTo("研发部");
            assertThat(dto.roleIds()).hasSize(2);
            assertThat(dto.createTime().getYear()).isEqualTo(2026);
        }

        @Test
        @DisplayName("JSON 序列化忽略未知字段")
        void shouldIgnoreUnknownFields() throws Exception {
            String json = """
                    {
                        "id": 1,
                        "username": "test",
                        "unknownField": "should be ignored"
                    }
                    """;
            var dto = MAPPER.readValue(json, UserBasicDTO.class);
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.username()).isEqualTo("test");
        }

        @Test
        @DisplayName("LocalDateTime 序列化正确")
        void shouldSerializeLocalDateTime() throws Exception {
            var now = LocalDateTime.of(2026, 2, 26, 15, 30, 0);
            var dto = new UserBasicDTO(1L, "u", "nick", "real", "e@e.com", "133",
                    1, "/a.png", 10L, "dept", "post", 1,
                    List.of(1L), List.of("admin"), now, now);
            String json = MAPPER.writeValueAsString(dto);
            assertThat(json).contains("2026");
        }
    }
}
