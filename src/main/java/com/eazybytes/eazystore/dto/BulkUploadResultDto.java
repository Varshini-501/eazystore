package com.eazybytes.eazystore.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class BulkUploadResultDto {

    private int totalRows;
    private int successCount;
    private int failureCount;
    private List<String> errors;
}
