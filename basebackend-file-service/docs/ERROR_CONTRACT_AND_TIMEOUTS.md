# File Service Error Contract and Timeout Defaults

## Timeout defaults
Storage adapters default to the following timeout settings (unless overridden via configuration properties):

- OSS (`file.storage.oss`)
  - `connectionTimeout`: 5000 ms
  - `socketTimeout`: 5000 ms
- S3 (`file.storage.s3`)
  - `connectionTimeout`: 5000 ms
  - `socketTimeout`: 5000 ms
- ClamAV (`file.antivirus.clamav`)
  - `host`: localhost
  - `port`: 3310
  - `timeout`: 30000 ms

## Retry behavior
- File service does not implement explicit retry loops for storage operations.
- Retry behavior is delegated to the underlying SDK/client configuration (OSS/S3/MinIO).
- If retry policies are required, configure them at the SDK layer or wrap storage calls at the service layer.

## Error contract
- File service throws `BusinessException` for validation and storage errors.
- Exceptions are expected to be mapped to the common `Result` error response by global exception handling.

Common error categories used in file-service code:
- Parameter validation: `BusinessException.paramError(...)`
- Not found: `BusinessException.notFound(...)`
- Upload/IO failures: `BusinessException.fileUploadFailed(...)`
- Forbidden operations: `BusinessException.forbidden(...)`

## References
- `basebackend-file-service/src/main/java/com/basebackend/file/config/OssProperties.java`
- `basebackend-file-service/src/main/java/com/basebackend/file/config/S3Properties.java`
- `basebackend-file-service/src/main/java/com/basebackend/file/antivirus/ClamAVAntivirusService.java`
- `basebackend-common/basebackend-common-core/src/main/java/com/basebackend/common/exception/BusinessException.java`
