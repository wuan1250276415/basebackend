package com.basebackend.backup.infrastructure.encryption;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncryptionResult {

    private File encryptedFile;
    private byte[] iv;
    private long originalSize;
    private String algorithm;
}
