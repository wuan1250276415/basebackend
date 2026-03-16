/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 */
package com.basebackend.nacos.service;

import com.basebackend.nacos.model.ConfigInfo;
import com.basebackend.nacos.service.NacosConfigService;
import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConfigPublisher {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(ConfigPublisher.class);
    private final NacosConfigService nacosConfigService;

    public PublishResult publishConfig(ConfigInfo configInfo, boolean force) {
        try {
            if (!force && Boolean.TRUE.equals(configInfo.getIsCritical())) {
                log.info("\u914d\u7f6e{}\u4e3a\u5173\u952e\u914d\u7f6e\uff0c\u9700\u8981\u624b\u52a8\u5ba1\u6838\u53d1\u5e03", (Object)configInfo.getDataId());
                return PublishResult.pending("\u914d\u7f6e\u4e3a\u5173\u952e\u914d\u7f6e\uff0c\u9700\u8981\u624b\u52a8\u5ba1\u6838\u53d1\u5e03");
            }
            String md5 = this.nacosConfigService.calculateMd5(configInfo.getContent());
            configInfo.setMd5(md5);
            boolean success = this.nacosConfigService.publishConfig(configInfo);
            if (success) {
                log.info("\u914d\u7f6e{}\u53d1\u5e03\u6210\u529f", (Object)configInfo.getDataId());
                return PublishResult.success("\u914d\u7f6e\u53d1\u5e03\u6210\u529f");
            }
            log.error("\u914d\u7f6e{}\u53d1\u5e03\u5931\u8d25", (Object)configInfo.getDataId());
            return PublishResult.failed("\u914d\u7f6e\u53d1\u5e03\u5931\u8d25");
        }
        catch (Exception e) {
            log.error("\u914d\u7f6e{}\u53d1\u5e03\u5f02\u5e38", (Object)configInfo.getDataId(), (Object)e);
            return PublishResult.failed("\u914d\u7f6e\u53d1\u5e03\u5f02\u5e38\uff1a" + e.getMessage());
        }
    }

    public PublishResult autoPublish(ConfigInfo configInfo) {
        return this.publishConfig(configInfo, false);
    }

    public PublishResult manualPublish(ConfigInfo configInfo) {
        return this.publishConfig(configInfo, true);
    }

    public BatchPublishResult batchPublish(List<ConfigInfo> configInfoList, boolean force) {
        int successCount = 0;
        int failedCount = 0;
        ArrayList<String> errors = new ArrayList<String>();
        for (ConfigInfo configInfo : configInfoList) {
            PublishResult result = this.publishConfig(configInfo, force);
            if ("success".equals(result.getStatus())) {
                ++successCount;
                continue;
            }
            ++failedCount;
            errors.add(configInfo.getDataId() + ": " + result.getMessage());
        }
        return new BatchPublishResult(successCount, failedCount, errors);
    }

    @Generated
    public ConfigPublisher(NacosConfigService nacosConfigService) {
        this.nacosConfigService = nacosConfigService;
    }

    public static class PublishResult {
        private String status;
        private String message;

        public static PublishResult success(String message) {
            return new PublishResult("success", message);
        }

        public static PublishResult failed(String message) {
            return new PublishResult("failed", message);
        }

        public static PublishResult pending(String message) {
            return new PublishResult("pending", message);
        }

        @Generated
        public String getStatus() {
            return this.status;
        }

        @Generated
        public String getMessage() {
            return this.message;
        }

        @Generated
        public void setStatus(String status) {
            this.status = status;
        }

        @Generated
        public void setMessage(String message) {
            this.message = message;
        }

        @Generated
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof PublishResult)) {
                return false;
            }
            PublishResult other = (PublishResult)o;
            if (!other.canEqual(this)) {
                return false;
            }
            String this$status = this.getStatus();
            String other$status = other.getStatus();
            if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
                return false;
            }
            String this$message = this.getMessage();
            String other$message = other.getMessage();
            return !(this$message == null ? other$message != null : !this$message.equals(other$message));
        }

        @Generated
        protected boolean canEqual(Object other) {
            return other instanceof PublishResult;
        }

        @Generated
        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            String $status = this.getStatus();
            result = result * 59 + ($status == null ? 43 : $status.hashCode());
            String $message = this.getMessage();
            result = result * 59 + ($message == null ? 43 : $message.hashCode());
            return result;
        }

        @Generated
        public String toString() {
            return "ConfigPublisher.PublishResult(status=" + this.getStatus() + ", message=" + this.getMessage() + ")";
        }

        @Generated
        public PublishResult(String status, String message) {
            this.status = status;
            this.message = message;
        }
    }

    public static class BatchPublishResult {
        private int successCount;
        private int failedCount;
        private List<String> errors;

        @Generated
        public int getSuccessCount() {
            return this.successCount;
        }

        @Generated
        public int getFailedCount() {
            return this.failedCount;
        }

        @Generated
        public List<String> getErrors() {
            return this.errors;
        }

        @Generated
        public void setSuccessCount(int successCount) {
            this.successCount = successCount;
        }

        @Generated
        public void setFailedCount(int failedCount) {
            this.failedCount = failedCount;
        }

        @Generated
        public void setErrors(List<String> errors) {
            this.errors = errors;
        }

        @Generated
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof BatchPublishResult)) {
                return false;
            }
            BatchPublishResult other = (BatchPublishResult)o;
            if (!other.canEqual(this)) {
                return false;
            }
            if (this.getSuccessCount() != other.getSuccessCount()) {
                return false;
            }
            if (this.getFailedCount() != other.getFailedCount()) {
                return false;
            }
            List<String> this$errors = this.getErrors();
            List<String> other$errors = other.getErrors();
            return !(this$errors == null ? other$errors != null : !((Object)this$errors).equals(other$errors));
        }

        @Generated
        protected boolean canEqual(Object other) {
            return other instanceof BatchPublishResult;
        }

        @Generated
        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            result = result * 59 + this.getSuccessCount();
            result = result * 59 + this.getFailedCount();
            List<String> $errors = this.getErrors();
            result = result * 59 + ($errors == null ? 43 : ((Object)$errors).hashCode());
            return result;
        }

        @Generated
        public String toString() {
            return "ConfigPublisher.BatchPublishResult(successCount=" + this.getSuccessCount() + ", failedCount=" + this.getFailedCount() + ", errors=" + String.valueOf(this.getErrors()) + ")";
        }

        @Generated
        public BatchPublishResult(int successCount, int failedCount, List<String> errors) {
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.errors = errors;
        }
    }
}

