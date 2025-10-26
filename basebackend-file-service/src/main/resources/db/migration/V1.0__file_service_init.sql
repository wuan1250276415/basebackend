-- 文件元数据表
CREATE TABLE IF NOT EXISTS file_metadata (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    file_id VARCHAR(64) NOT NULL UNIQUE COMMENT '文件唯一标识',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '存储路径',
    file_size BIGINT NOT NULL COMMENT '文件大小(字节)',
    content_type VARCHAR(100) COMMENT '文件MIME类型',
    file_extension VARCHAR(50) COMMENT '文件扩展名',
    md5 VARCHAR(64) COMMENT '文件MD5值',
    sha256 VARCHAR(128) COMMENT '文件SHA256值',
    storage_type VARCHAR(20) NOT NULL DEFAULT 'LOCAL' COMMENT '存储类型:LOCAL,MINIO,ALIYUN_OSS,AWS_S3',
    bucket_name VARCHAR(100) COMMENT '存储桶名称',
    folder_id BIGINT COMMENT '所属文件夹ID',
    folder_path VARCHAR(500) COMMENT '文件夹路径',
    is_folder TINYINT(1) DEFAULT 0 COMMENT '是否为文件夹',
    owner_id BIGINT NOT NULL COMMENT '所有者ID',
    owner_name VARCHAR(100) COMMENT '所有者名称',
    is_public TINYINT(1) DEFAULT 0 COMMENT '是否公开',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '是否删除(软删除)',
    deleted_at DATETIME COMMENT '删除时间',
    deleted_by BIGINT COMMENT '删除人ID',
    version INT DEFAULT 1 COMMENT '当前版本号',
    latest_version_id BIGINT COMMENT '最新版本ID',
    download_count INT DEFAULT 0 COMMENT '下载次数',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    thumbnail_path VARCHAR(500) COMMENT '缩略图路径',
    tags VARCHAR(500) COMMENT '标签(JSON数组)',
    description TEXT COMMENT '文件描述',
    metadata JSON COMMENT '扩展元数据',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_file_id (file_id),
    INDEX idx_owner_id (owner_id),
    INDEX idx_folder_id (folder_id),
    INDEX idx_deleted (is_deleted),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件元数据表';

-- 文件版本表
CREATE TABLE IF NOT EXISTS file_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    file_id VARCHAR(64) NOT NULL COMMENT '文件ID',
    version_number INT NOT NULL COMMENT '版本号',
    file_path VARCHAR(500) NOT NULL COMMENT '存储路径',
    file_size BIGINT NOT NULL COMMENT '文件大小',
    md5 VARCHAR(64) COMMENT '文件MD5值',
    change_description VARCHAR(500) COMMENT '变更说明',
    created_by BIGINT NOT NULL COMMENT '创建人ID',
    created_by_name VARCHAR(100) COMMENT '创建人名称',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    is_current TINYINT(1) DEFAULT 0 COMMENT '是否当前版本',
    INDEX idx_file_id (file_id),
    INDEX idx_version (file_id, version_number),
    UNIQUE KEY uk_file_version (file_id, version_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件版本表';

-- 文件权限表
CREATE TABLE IF NOT EXISTS file_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    file_id VARCHAR(64) NOT NULL COMMENT '文件ID',
    user_id BIGINT COMMENT '用户ID',
    role_id BIGINT COMMENT '角色ID',
    dept_id BIGINT COMMENT '部门ID',
    permission_type VARCHAR(20) NOT NULL COMMENT '权限类型:READ,WRITE,DELETE,SHARE',
    expire_time DATETIME COMMENT '过期时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_file_id (file_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id),
    INDEX idx_dept_id (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件权限表';

-- 文件分享表
CREATE TABLE IF NOT EXISTS file_share (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    share_code VARCHAR(64) NOT NULL UNIQUE COMMENT '分享码',
    file_id VARCHAR(64) NOT NULL COMMENT '文件ID',
    share_type VARCHAR(20) NOT NULL DEFAULT 'LINK' COMMENT '分享类型:LINK,PASSWORD',
    share_password VARCHAR(20) COMMENT '分享密码',
    share_by BIGINT NOT NULL COMMENT '分享人ID',
    share_by_name VARCHAR(100) COMMENT '分享人名称',
    expire_time DATETIME COMMENT '过期时间',
    download_limit INT DEFAULT 0 COMMENT '下载次数限制(0表示不限制)',
    download_count INT DEFAULT 0 COMMENT '已下载次数',
    view_count INT DEFAULT 0 COMMENT '查看次数',
    is_enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_share_code (share_code),
    INDEX idx_file_id (file_id),
    INDEX idx_share_by (share_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件分享表';

-- 文件操作日志表
CREATE TABLE IF NOT EXISTS file_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    file_id VARCHAR(64) NOT NULL COMMENT '文件ID',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型:UPLOAD,DOWNLOAD,DELETE,RENAME,MOVE,SHARE,RECOVER',
    operator_id BIGINT COMMENT '操作人ID',
    operator_name VARCHAR(100) COMMENT '操作人名称',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(500) COMMENT '用户代理',
    operation_detail TEXT COMMENT '操作详情',
    operation_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_file_id (file_id),
    INDEX idx_operator_id (operator_id),
    INDEX idx_operation_time (operation_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件操作日志表';

-- 回收站表
CREATE TABLE IF NOT EXISTS file_recycle_bin (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    file_id VARCHAR(64) NOT NULL COMMENT '文件ID',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '原始路径',
    file_size BIGINT NOT NULL COMMENT '文件大小',
    deleted_by BIGINT NOT NULL COMMENT '删除人ID',
    deleted_by_name VARCHAR(100) COMMENT '删除人名称',
    deleted_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '删除时间',
    expire_at DATETIME COMMENT '过期时间(自动清理时间)',
    original_metadata JSON COMMENT '原始元数据',
    INDEX idx_file_id (file_id),
    INDEX idx_deleted_by (deleted_by),
    INDEX idx_deleted_at (deleted_at),
    INDEX idx_expire_at (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回收站表';

-- 文件标签表
CREATE TABLE IF NOT EXISTS file_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tag_name VARCHAR(50) NOT NULL COMMENT '标签名称',
    tag_color VARCHAR(20) COMMENT '标签颜色',
    created_by BIGINT COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_tag_name (tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件标签表';

-- 文件标签关联表
CREATE TABLE IF NOT EXISTS file_tag_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    file_id VARCHAR(64) NOT NULL COMMENT '文件ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_file_id (file_id),
    INDEX idx_tag_id (tag_id),
    UNIQUE KEY uk_file_tag (file_id, tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件标签关联表';
