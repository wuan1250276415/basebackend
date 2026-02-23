package com.basebackend.common.export;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExportResult {

    private String fileName;
    private String contentType;
    private byte[] content;
}
