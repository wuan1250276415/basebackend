-- ==============================================
-- 家庭相册共享系统 - 初始化建表脚本
-- ==============================================

-- 家庭组
CREATE TABLE album_family_group (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '家庭名称',
    description VARCHAR(500) COMMENT '描述',
    avatar VARCHAR(500) COMMENT '家庭头像URL',
    owner_id BIGINT NOT NULL COMMENT '创建者用户ID',
    invite_code VARCHAR(32) UNIQUE COMMENT '邀请码',
    max_members INT DEFAULT 20 COMMENT '最大成员数',
    max_storage_gb INT DEFAULT 50 COMMENT '最大存储空间(GB)',
    used_storage_bytes BIGINT DEFAULT 0 COMMENT '已用存储(字节)',
    tenant_id BIGINT COMMENT '租户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_owner(owner_id),
    INDEX idx_invite(invite_code)
) COMMENT '家庭组';

-- 家庭成员
CREATE TABLE album_family_member (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    family_id BIGINT NOT NULL COMMENT '家庭ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    nickname VARCHAR(50) COMMENT '家庭内昵称',
    role TINYINT DEFAULT 0 COMMENT '角色: 0=成员 1=管理员 2=创建者',
    join_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    tenant_id BIGINT COMMENT '租户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_family_user(family_id, user_id),
    INDEX idx_user(user_id)
) COMMENT '家庭成员';

-- 相册
CREATE TABLE album_album (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(200) NOT NULL COMMENT '相册名称',
    description VARCHAR(1000) COMMENT '描述',
    cover_photo_id BIGINT COMMENT '封面照片ID',
    family_id BIGINT COMMENT '所属家庭ID（NULL=个人相册）',
    owner_id BIGINT NOT NULL COMMENT '创建者ID',
    type TINYINT DEFAULT 0 COMMENT '类型: 0=普通 1=时间轴自动 2=智能',
    visibility TINYINT DEFAULT 0 COMMENT '可见性: 0=私有 1=家庭 2=链接公开',
    photo_count INT DEFAULT 0 COMMENT '照片数量',
    sort_order INT DEFAULT 0 COMMENT '排序',
    tenant_id BIGINT COMMENT '租户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_family(family_id),
    INDEX idx_owner(owner_id)
) COMMENT '相册';

-- 照片/视频
CREATE TABLE album_photo (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    album_id BIGINT NOT NULL COMMENT '所属相册ID',
    owner_id BIGINT NOT NULL COMMENT '上传者ID',
    file_name VARCHAR(500) NOT NULL COMMENT '原始文件名',
    file_path VARCHAR(1000) NOT NULL COMMENT '存储路径',
    thumbnail_path VARCHAR(1000) COMMENT '缩略图路径',
    file_size BIGINT NOT NULL COMMENT '文件大小(字节)',
    mime_type VARCHAR(100) COMMENT 'MIME类型',
    width INT COMMENT '宽度(px)',
    height INT COMMENT '高度(px)',
    media_type TINYINT DEFAULT 0 COMMENT '媒体类型: 0=照片 1=视频',
    duration INT COMMENT '视频时长(秒)',
    taken_at DATETIME COMMENT '拍摄时间(EXIF)',
    location VARCHAR(200) COMMENT '拍摄地点',
    latitude DECIMAL(10,7) COMMENT '纬度',
    longitude DECIMAL(10,7) COMMENT '经度',
    description VARCHAR(1000) COMMENT '描述',
    tags VARCHAR(500) COMMENT '标签(逗号分隔)',
    exif_data JSON COMMENT 'EXIF信息',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    comment_count INT DEFAULT 0 COMMENT '评论数',
    tenant_id BIGINT COMMENT '租户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_album(album_id),
    INDEX idx_owner(owner_id),
    INDEX idx_taken(taken_at),
    INDEX idx_media_type(media_type)
) COMMENT '照片/视频';

-- 评论
CREATE TABLE album_comment (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    photo_id BIGINT NOT NULL COMMENT '照片ID',
    user_id BIGINT NOT NULL COMMENT '评论者ID',
    content VARCHAR(500) NOT NULL COMMENT '评论内容',
    parent_id BIGINT COMMENT '父评论ID(回复)',
    tenant_id BIGINT COMMENT '租户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_photo(photo_id),
    INDEX idx_user(user_id)
) COMMENT '照片评论';

-- 点赞
CREATE TABLE album_like (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    photo_id BIGINT NOT NULL COMMENT '照片ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_photo_user(photo_id, user_id)
) COMMENT '照片点赞';

-- 分享链接
CREATE TABLE album_share_link (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    album_id BIGINT NOT NULL COMMENT '相册ID',
    creator_id BIGINT NOT NULL COMMENT '创建者ID',
    share_code VARCHAR(32) UNIQUE NOT NULL COMMENT '分享码',
    password VARCHAR(100) COMMENT '访问密码(加密)',
    expire_time DATETIME COMMENT '过期时间(NULL=永不过期)',
    max_views INT COMMENT '最大查看次数',
    view_count INT DEFAULT 0 COMMENT '已查看次数',
    allow_download TINYINT DEFAULT 0 COMMENT '是否允许下载',
    status TINYINT DEFAULT 1 COMMENT '状态: 0=已失效 1=有效',
    tenant_id BIGINT COMMENT '租户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_album(album_id),
    INDEX idx_code(share_code)
) COMMENT '分享链接';
