package com.basebackend.common.export;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ImportResult<T> {

    private int totalRows;
    private int successRows;
    private int failedRows;
    private List<String> errors;
    private List<T> data;
}
