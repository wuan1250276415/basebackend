package com.basebackend.database.config;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
public class DatabaseVendorDetector {

    private final DatabaseEnhancedProperties properties;
    private final DataSource dataSource;
    private volatile DatabaseVendor cached;

    public DatabaseVendorDetector(DatabaseEnhancedProperties properties, DataSource dataSource) {
        this.properties = properties;
        this.dataSource = dataSource;
    }

    public DatabaseVendor detect() {
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (cached != null) {
                return cached;
            }
            String vendor = properties.getVendor();
            if (!"auto".equalsIgnoreCase(vendor)) {
                cached = DatabaseVendor.valueOf(vendor.toUpperCase());
                log.info("Database vendor configured explicitly: {}", cached);
                return cached;
            }
            try (Connection conn = dataSource.getConnection()) {
                String url = conn.getMetaData().getURL();
                cached = DatabaseVendor.fromJdbcUrl(url);
                log.info("Database vendor auto-detected from URL: {}", cached);
            } catch (SQLException e) {
                log.warn("Failed to detect database vendor, defaulting to MYSQL", e);
                cached = DatabaseVendor.MYSQL;
            }
            return cached;
        }
    }
}
