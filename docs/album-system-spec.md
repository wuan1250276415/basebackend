# 家庭相册共享系统 - 技术规格文档
> BearTeam 项目 | 基于 BaseBackend 平台

## 1. 项目概述

### 1.1 产品定位
面向家庭用户的私有化相册共享平台，支持照片/视频上传、智能相册管理、家庭成员共享、时间轴浏览、评论互动等功能。

### 1.2 核心功能
- 📸 照片/视频上传与管理（支持批量上传、拖拽上传）
- 📁 相册管理（创建、编辑、删除、封面设置）
- 👨‍👩‍👧‍👦 家庭组管理（创建家庭、邀请成员、权限控制）
- 🔗 相册共享（家庭内共享、外部链接分享）
- 📅 时间轴浏览（按日期自动分组）
- 💬 评论与点赞互动
- 🔍 照片搜索（按标签、日期、描述）
- 📥 批量下载与导出
- 🗑️ 回收站（软删除 + 自动清理）

### 1.3 技术栈
- **后端**: Java 25 + Spring Boot 4.0.3 + MyBatis-Plus 3.5.16
- **前端**: React 18 + TypeScript + Ant Design 5 + Zustand
- **存储**: MySQL + Redis + 本地文件系统（可扩展 OSS）
- **依赖平台模块**: basebackend-common, basebackend-database, basebackend-cache, basebackend-security, basebackend-jwt, basebackend-file-service, basebackend-search

---

## 2. 系统架构

### 2.1 模块结构
```
basebackend-album-api/          # 后端微服务
├── src/main/java/com/basebackend/album/
│   ├── AlbumApiApplication.java
│   ├── config/                 # 配置类
│   │   ├── AlbumAutoConfiguration.java
│   │   └── StorageConfig.java
│   ├── controller/             # REST 控制器
│   │   ├── AlbumController.java
│   │   ├── PhotoController.java
│   │   ├── FamilyGroupController.java
│   │   ├── ShareController.java
│   │   ├── CommentController.java
│   │   └── TimelineController.java
│   ├── entity/                 # 数据实体
│   │   ├── Album.java
│   │   ├── Photo.java
│   │   ├── FamilyGroup.java
│   │   ├── FamilyMember.java
│   │   ├── AlbumShare.java
│   │   ├── PhotoComment.java
│   │   ├── PhotoLike.java
│   │   └── ShareLink.java
│   ├── mapper/                 # MyBatis-Plus Mapper
│   ├── service/                # 业务逻辑
│   │   ├── AlbumService.java
│   │   ├── PhotoService.java
│   │   ├── FamilyGroupService.java
│   │   ├── ShareService.java
│   │   ├── CommentService.java
│   │   ├── TimelineService.java
│   │   └── impl/
│   ├── dto/                    # 数据传输对象
│   ├── vo/                     # 视图对象
│   └── enums/                  # 枚举
├── src/main/resources/
│   ├── application.yml
│   ├── db/migration/
│   │   └── V1__init_album_tables.sql
│   └── META-INF/
└── pom.xml

basebackend-album-ui/           # 前端应用
├── src/
│   ├── api/                    # API 接口
│   ├── components/             # 通用组件
│   │   ├── PhotoGrid/          # 照片网格（瀑布流）
│   │   ├── PhotoViewer/        # 照片查看器（全屏轮播）
│   │   ├── UploadZone/         # 拖拽上传区域
│   │   └── Timeline/           # 时间轴组件
│   ├── pages/
│   │   ├── Home/               # 首页（最近照片 + 快捷入口）
│   │   ├── Albums/             # 相册列表
│   │   ├── AlbumDetail/        # 相册详情
│   │   ├── Timeline/           # 时间轴页
│   │   ├── Family/             # 家庭管理
│   │   ├── SharedWithMe/       # 共享给我的
│   │   ├── Trash/              # 回收站
│   │   └── Login/              # 登录页
│   ├── stores/                 # Zustand 状态管理
│   ├── types/                  # TypeScript 类型
│   └── utils/                  # 工具函数
├── vite.config.ts
├── package.json
└── tsconfig.json
```

---

## 3. 数据模型

### 3.1 数据库表设计

```sql
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
```

---

## 4. REST API 设计

### 4.1 家庭组 `/api/album/families`
| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/album/families` | 创建家庭 |
| GET | `/api/album/families` | 我的家庭列表 |
| GET | `/api/album/families/{id}` | 家庭详情 |
| PUT | `/api/album/families/{id}` | 编辑家庭 |
| DELETE | `/api/album/families/{id}` | 解散家庭 |
| POST | `/api/album/families/{id}/invite` | 生成邀请码 |
| POST | `/api/album/families/join` | 加入家庭(邀请码) |
| GET | `/api/album/families/{id}/members` | 成员列表 |
| PUT | `/api/album/families/{id}/members/{userId}` | 修改成员角色 |
| DELETE | `/api/album/families/{id}/members/{userId}` | 移除成员 |
| POST | `/api/album/families/{id}/leave` | 退出家庭 |

### 4.2 相册 `/api/album/albums`
| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/album/albums` | 创建相册 |
| GET | `/api/album/albums` | 相册列表(个人+家庭) |
| GET | `/api/album/albums/{id}` | 相册详情 |
| PUT | `/api/album/albums/{id}` | 编辑相册 |
| DELETE | `/api/album/albums/{id}` | 删除相册 |
| PUT | `/api/album/albums/{id}/cover` | 设置封面 |
| GET | `/api/album/albums/family/{familyId}` | 家庭相册列表 |

### 4.3 照片 `/api/album/photos`
| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/album/photos/upload` | 上传照片(支持批量) |
| GET | `/api/album/photos` | 照片列表(分页) |
| GET | `/api/album/photos/{id}` | 照片详情 |
| PUT | `/api/album/photos/{id}` | 编辑照片信息 |
| DELETE | `/api/album/photos/{id}` | 删除照片(软删除) |
| DELETE | `/api/album/photos/batch` | 批量删除 |
| POST | `/api/album/photos/{id}/move` | 移动到其他相册 |
| GET | `/api/album/photos/timeline` | 时间轴(按日期分组) |
| GET | `/api/album/photos/search` | 搜索照片 |
| POST | `/api/album/photos/{id}/like` | 点赞 |
| DELETE | `/api/album/photos/{id}/like` | 取消点赞 |
| GET | `/api/album/photos/download/{id}` | 下载原图 |
| POST | `/api/album/photos/download/batch` | 批量下载(ZIP) |

### 4.4 评论 `/api/album/comments`
| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/album/comments` | 发表评论 |
| GET | `/api/album/comments/photo/{photoId}` | 照片评论列表 |
| DELETE | `/api/album/comments/{id}` | 删除评论 |

### 4.5 分享 `/api/album/shares`
| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/album/shares` | 创建分享链接 |
| GET | `/api/album/shares` | 我的分享列表 |
| DELETE | `/api/album/shares/{id}` | 取消分享 |
| GET | `/api/album/shares/view/{shareCode}` | 访问分享(公开) |

### 4.6 回收站 `/api/album/trash`
| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/album/trash` | 回收站列表 |
| POST | `/api/album/trash/{id}/restore` | 恢复照片 |
| DELETE | `/api/album/trash/{id}` | 彻底删除 |
| DELETE | `/api/album/trash/clear` | 清空回收站 |

---

## 5. 前端页面设计

### 5.1 页面清单
| 页面 | 路径 | 描述 |
|------|------|------|
| 首页 | `/` | 最近照片 + 快捷操作 + 家庭动态 |
| 相册列表 | `/albums` | 相册卡片网格，支持创建 |
| 相册详情 | `/albums/:id` | 照片网格 + 上传 + 管理 |
| 时间轴 | `/timeline` | 按日期分组的照片流 |
| 家庭管理 | `/family` | 家庭列表、成员管理、邀请 |
| 共享给我 | `/shared` | 家庭成员共享的相册 |
| 回收站 | `/trash` | 已删除照片，支持恢复 |
| 登录 | `/login` | 登录页 |

### 5.2 核心交互
- **照片网格**: 瀑布流布局，懒加载缩略图，hover 显示操作按钮
- **照片查看器**: 全屏轮播模式，支持左右切换、缩放、旋转、下载
- **拖拽上传**: 拖拽文件到页面任意位置触发上传
- **时间轴**: 按日期分组，滚动加载，左侧日期导航

### 5.3 UI 风格
- 简洁温馨的家庭风格
- 主色调：暖色系 #ff8c00 (橙色)
- 大圆角卡片、柔和阴影
- 照片优先展示，文字辅助

---

## 6. 开发计划（5 步）

### Step 1: 后端基础（后端熊 🐻‍❄️）
- 创建 `basebackend-album-api` 模块
- 数据实体 + Mapper + 基础 Service
- SQL 建表脚本
- 编译通过

### Step 2: 后端 API（后端熊 🐻‍❄️）
- 全部 Controller + 完整业务逻辑
- 文件上传（对接 basebackend-file-service）
- EXIF 信息提取
- 分享链接逻辑

### Step 3: 前端搭建（前端熊 🐼）
- 创建 `basebackend-album-ui` 模块
- 基础框架（路由、布局、状态管理）
- 登录页 + 首页 + 相册列表

### Step 4: 前端功能（前端熊 🐼）
- 照片上传 + 网格展示 + 查看器
- 时间轴 + 家庭管理 + 分享
- 评论互动 + 回收站

### Step 5: 测试 + 部署（测试熊 🐨 + 运维熊 🧸）
- 单元测试（核心 Service）
- Dockerfile
- 集成验证

---

## 7. 端口与配置
- **后端端口**: 8087
- **前端端口**: 5174（开发模式）
- **API 前缀**: `/api/album/`
- **表前缀**: `album_`
- **包名**: `com.basebackend.album`
