package com.basebackend.generator.handler;

import com.basebackend.database.security.service.EncryptionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 密码加密类型处理器
 * 用于MyBatis-Plus自动加密/解密密码字段
 * 
 * 数据入库时自动加密，读取时自动解密
 */
@Slf4j
@Component
@MappedTypes(String.class)
public class PasswordEncryptTypeHandler extends BaseTypeHandler<String> {

    private static EncryptionService encryptionService;

    /**
     * 通过Spring注入加密服务
     * 使用静态变量以支持MyBatis类型处理器的无参构造
     */
    @Autowired
    public void setEncryptionService(EncryptionService encryptionService) {
        PasswordEncryptTypeHandler.encryptionService = encryptionService;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        // 入库时加密
        if (encryptionService != null) {
            String encrypted = encryptionService.encrypt(parameter);
            ps.setString(i, encrypted);
        } else {
            log.warn("EncryptionService未注入，密码将以明文存储");
            ps.setString(i, parameter);
        }
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return decryptIfNeeded(value);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return decryptIfNeeded(value);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return decryptIfNeeded(value);
    }

    /**
     * 解密数据（如需要）
     */
    private String decryptIfNeeded(String value) {
        if (value == null || encryptionService == null) {
            return value;
        }
        try {
            // 加密服务会自动判断是否已加密
            return encryptionService.decrypt(value);
        } catch (Exception e) {
            log.warn("密码解密失败，返回原始值: {}", e.getMessage());
            return value;
        }
    }
}
