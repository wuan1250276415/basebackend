package com.basebackend.security.encryption;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * 字段级加密服务
 * 用于对实体类中的敏感字段进行自动加密和解密
 * 支持标注@Encrypted注解的字段
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FieldEncryptionService {

    private final AESEncryptionService aesEncryptionService;
    private final RSAEncryptionService rsaEncryptionService;

    /**
     * 加密实体对象中的敏感字段
     *
     * @param object 要加密的实体对象
     * @param masterKey 主密钥(Base64编码)
     * @return 加密后的实体对象
     */
    public Object encryptFields(Object object, String masterKey) {
        if (object == null) {
            return null;
        }

        try {
            Class<?> clazz = object.getClass();
            Field[] fields = clazz.getDeclaredFields();

            for (Field field : fields) {
                Encrypted encrypted = field.getAnnotation(Encrypted.class);
                if (encrypted != null) {
                    field.setAccessible(true);
                    Object value = field.get(object);

                    if (value != null && !value.toString().isEmpty()) {
                        String encryptedValue;
                        if (encrypted.algorithm() == EncryptionType.AES) {
                            encryptedValue = aesEncryptionService.encrypt(value.toString(), masterKey);
                        } else {
                            // RSA加密，需要获取公钥
                            encryptedValue = rsaEncryptionService.encrypt(value.toString(), encrypted.publicKey());
                        }

                        field.set(object, encryptedValue);
                        log.debug("字段 {} 加密成功", field.getName());
                    }
                }
            }

            return object;
        } catch (Exception e) {
            log.error("字段加密失败", e);
            throw new SecurityException("字段加密失败", e);
        }
    }

    /**
     * 解密实体对象中的敏感字段
     *
     * @param object 要解密的实体对象
     * @param masterKey 主密钥(Base64编码)
     * @return 解密后的实体对象
     */
    public Object decryptFields(Object object, String masterKey) {
        if (object == null) {
            return null;
        }

        try {
            Class<?> clazz = object.getClass();
            Field[] fields = clazz.getDeclaredFields();

            for (Field field : fields) {
                Encrypted encrypted = field.getAnnotation(Encrypted.class);
                if (encrypted != null) {
                    field.setAccessible(true);
                    Object value = field.get(object);

                    if (value != null && !value.toString().isEmpty()) {
                        String decryptedValue;
                        if (encrypted.algorithm() == EncryptionType.AES) {
                            decryptedValue = aesEncryptionService.decrypt(value.toString(), masterKey);
                        } else {
                            // RSA解密，需要获取私钥
                            decryptedValue = rsaEncryptionService.decrypt(value.toString(), encrypted.privateKey());
                        }

                        field.set(object, decryptedValue);
                        log.debug("字段 {} 解密成功", field.getName());
                    }
                }
            }

            return object;
        } catch (Exception e) {
            log.error("字段解密失败", e);
            throw new SecurityException("字段解密失败", e);
        }
    }

    /**
     * 加密实体对象中的特定字段
     *
     * @param object 要加密的实体对象
     * @param fieldNames 字段名列表
     * @param masterKey 主密钥
     * @return 加密后的实体对象
     */
    public Object encryptSpecificFields(Object object, String[] fieldNames, String masterKey) {
        if (object == null) {
            return null;
        }

        try {
            Class<?> clazz = object.getClass();

            for (String fieldName : fieldNames) {
                try {
                    Field field = clazz.getDeclaredField(fieldName);
                    Encrypted encrypted = field.getAnnotation(Encrypted.class);

                    if (encrypted != null) {
                        field.setAccessible(true);
                        Object value = field.get(object);

                        if (value != null && !value.toString().isEmpty()) {
                            String encryptedValue = aesEncryptionService.encrypt(value.toString(), masterKey);
                            field.set(object, encryptedValue);
                            log.debug("字段 {} 加密成功", fieldName);
                        }
                    }
                } catch (NoSuchFieldException e) {
                    log.warn("字段不存在: {}", fieldName);
                }
            }

            return object;
        } catch (Exception e) {
            log.error("字段加密失败", e);
            throw new SecurityException("字段加密失败", e);
        }
    }

    /**
     * 批量加密实体对象列表
     *
     * @param objects 实体对象列表
     * @param masterKey 主密钥
     * @return 加密后的实体对象列表
     */
    public <T> Iterable<T> encryptBatch(Iterable<T> objects, String masterKey) {
        if (objects == null) {
            return null;
        }

        try {
            for (Object object : objects) {
                encryptFields(object, masterKey);
            }
            return objects;
        } catch (Exception e) {
            log.error("批量字段加密失败", e);
            throw new SecurityException("批量字段加密失败", e);
        }
    }

    /**
     * 批量解密实体对象列表
     *
     * @param objects 实体对象列表
     * @param masterKey 主密钥
     * @return 解密后的实体对象列表
     */
    public <T> Iterable<T> decryptBatch(Iterable<T> objects, String masterKey) {
        if (objects == null) {
            return null;
        }

        try {
            for (Object object : objects) {
                decryptFields(object, masterKey);
            }
            return objects;
        } catch (Exception e) {
            log.error("批量字段解密失败", e);
            throw new SecurityException("批量字段解密失败", e);
        }
    }

    /**
     * 加密注解
     * 用于标注需要加密的字段
     */
    @java.lang.annotation.Target({java.lang.annotation.ElementType.FIELD})
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface Encrypted {
        /**
         * 加密算法
         */
        EncryptionType algorithm() default EncryptionType.AES;

        /**
         * RSA公钥(Base64编码)
         * 当算法为RSA时使用
         */
        String publicKey() default "";

        /**
         * RSA私钥(Base64编码)
         * 当算法为RSA时使用
         */
        String privateKey() default "";
    }

    /**
     * 加密类型枚举
     */
    public enum EncryptionType {
        AES,
        RSA
    }

    /**
     * 获取需要加密的字段列表
     *
     * @param clazz 类对象
     * @return 字段名列表
     */
    public static Map<String, EncryptionType> getEncryptedFields(Class<?> clazz) {
        Map<String, EncryptionType> encryptedFields = new HashMap<>();

        try {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                Encrypted encrypted = field.getAnnotation(Encrypted.class);
                if (encrypted != null) {
                    encryptedFields.put(field.getName(), encrypted.algorithm());
                }
            }
        } catch (Exception e) {
            log.error("获取加密字段失败", e);
        }

        return encryptedFields;
    }
}
