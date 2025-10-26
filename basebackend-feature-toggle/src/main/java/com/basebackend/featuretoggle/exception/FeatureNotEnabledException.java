package com.basebackend.featuretoggle.exception;

/**
 * 特性未启用异常
 *
 * @author BaseBackend
 */
public class FeatureNotEnabledException extends RuntimeException {

    private final String featureName;

    public FeatureNotEnabledException(String featureName) {
        super("Feature '" + featureName + "' is not enabled");
        this.featureName = featureName;
    }

    public FeatureNotEnabledException(String featureName, String message) {
        super(message);
        this.featureName = featureName;
    }

    public String getFeatureName() {
        return featureName;
    }
}
