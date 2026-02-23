package com.basebackend.database.config;

public enum DatabaseVendor {
    MYSQL,
    POSTGRESQL;

    public static DatabaseVendor fromJdbcUrl(String url) {
        if (url == null) {
            return MYSQL;
        }
        String lower = url.toLowerCase();
        if (lower.startsWith("jdbc:postgresql:")) {
            return POSTGRESQL;
        }
        return MYSQL;
    }
}
