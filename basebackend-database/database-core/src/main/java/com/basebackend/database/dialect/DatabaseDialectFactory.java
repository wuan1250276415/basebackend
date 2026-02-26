package com.basebackend.database.dialect;

import com.basebackend.database.config.DatabaseVendor;
import com.basebackend.database.config.DatabaseVendorDetector;
import org.springframework.stereotype.Component;

@Component
public class DatabaseDialectFactory {

    private final DatabaseVendorDetector vendorDetector;
    private volatile DatabaseDialect cached;

    public DatabaseDialectFactory(DatabaseVendorDetector vendorDetector) {
        this.vendorDetector = vendorDetector;
    }

    public DatabaseDialect getDialect() {
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (cached != null) {
                return cached;
            }
            DatabaseVendor vendor = vendorDetector.detect();
            cached = switch (vendor) {
                case POSTGRESQL -> new PostgreSqlDialect();
                default -> new MySqlDialect();
            };
            return cached;
        }
    }
}
