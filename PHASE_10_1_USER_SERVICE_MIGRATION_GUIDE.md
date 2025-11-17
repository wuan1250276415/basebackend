# Phase 10.1: 用户服务迁移实施指南

## 📋 概述

本指南详细说明如何将用户相关功能从 `basebackend-admin-api` 迁移到独立的 `basebackend-user-service`，实现真正的微服务架构。

---

## 🎯 实施目标

### 核心目标
- ✅ **服务解耦**: 用户服务独立部署运行
- ✅ **数据隔离**: 独立的用户数据库
- ✅ **接口稳定**: Feign 客户端平滑调用
- ✅ **网关路由**: Gateway 智能路由配置
- ✅ **性能提升**: 响应时间 < 100ms

### 技术指标
| 指标 | 目标值 | 说明 |
|------|--------|------|
| **API 响应时间** | < 100ms | P95 < 150ms |
| **服务可用性** | > 99.9% | 7x24 运行 |
| **QPS** | > 1000 | 单实例 |
| **数据库连接** | < 100ms | 99% 请求 |
| **缓存命中率** | > 90% | 用户信息缓存 |

---

## 🏗️ 整体架构

### 微服务架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                        微服务架构                                    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐     │
│  │  API Gateway    │  │  User Service   │  │  Admin Service  │     │
│  │                │  │                │  │                │     │
│  │  • 路由         │  │  • 用户管理      │  │  • 管理功能      │     │
│  │  • 限流         │  │  • 认证         │  │  • 配置         │     │
│  │  • 鉴权         │  │  • 缓存         │  │  • 监控         │     │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘     │
│           │                    │                    │               │
│           │                    ▼                    │               │
│           │         ┌─────────────────┐             │               │
│           │         │  User Database  │             │               │
│           │         │                │             │               │
│           │         │  • users        │             │               │
│           │         │  • user_roles   │             │               │
│           │         │  • user_profiles│             │               │
│           │         └─────────────────┘             │               │
│           │                                        │               │
│           └────────────┬───────────────┬───────────┘               │
│                        │               │                           │
│                        ▼               ▼                           │
│              ┌─────────────────┐ ┌─────────────────┐              │
│              │  Redis Cache   │ │ Monitoring      │              │
│              │                │ │ (Prometheus)    │              │
│              └─────────────────┘ └─────────────────┘              │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 服务交互流程

```
┌─────────────────────────────────────────────────────────────────────┐
│                        服务调用流程                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  [Client] → [API Gateway] → [User Service] → [User Database]      │
│     ↓          ↓              ↓                  ↓                 │
│  HTTP       路由匹配        业务逻辑           数据查询             │
│  Request    限流熔断        Feign调用         Redis缓存            │
│     ↓          ↓              ↓                  ↓                 │
│  返回结果    鉴权校验        返回DTO           写入缓存             │
│                                                                     │
│                                                                     │
│  [Admin API] → [Feign Client] → [User Service] → [Response]       │
│     ↓             ↓               ↓               ↓                 │
│  调用用户       构建请求        处理业务        返回用户信息         │
│  业务逻辑       Ribbon负载       限流熔断        Feign解码           │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 📦 项目结构

### 新建项目结构

```
basebackend/
├── basebackend-user-service/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/basebackend/user/
│   │       │       ├── UserServiceApplication.java
│   │       │       ├── controller/
│   │       │       │   ├── UserController.java
│   │       │       │   ├── AuthController.java
│   │       │       │   └── ProfileController.java
│   │       │       ├── service/
│   │       │       │   ├── UserService.java
│   │       │       │   ├── UserServiceImpl.java
│   │       │       │   └── CacheService.java
│   │       │       ├── mapper/
│   │       │       │   ├── UserMapper.java
│   │       │       │   └── UserRoleMapper.java
│   │       │       ├── entity/
│   │       │       │   ├── User.java
│   │       │       │   ├── UserDTO.java
│   │       │       │   ├── UserVO.java
│   │       │       │   └── UserRole.java
│   │       │       ├── config/
│   │       │       │   ├── RedisConfig.java
│   │       │       │   ├── MyBatisConfig.java
│   │       │       │   └── SecurityConfig.java
│   │       │       └── common/
│   │       │           ├── Result.java
│   │       │           ├── PageResult.java
│   │       │           └── Constants.java
│   │       └── resources/
│   │           ├── application.yml
│   │           ├── mapper/
│   │           │   ├── UserMapper.xml
│   │           │   └── UserRoleMapper.xml
│   │           └── db/migration/
│   │               ├── V1__Create_users_table.sql
│   │               └── V2__Create_user_roles_table.sql
│   └── pom.xml
│
├── basebackend-user-service-api/
│   └── src/main/java/com/basebackend/user/api/
│       ├── UserServiceClient.java
│       ├── AuthServiceClient.java
│       └── dto/
│           ├── UserDTO.java
│           ├── LoginDTO.java
│           └── RegisterDTO.java
│
└── basebackend-gateway/
    └── src/main/resources/
        └── routes/
            └── user-service-routes.yml
```

---

## 🔨 详细实施步骤

### 步骤 1: 创建用户服务模块

#### 1.1 创建项目 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.basebackend</groupId>
    <artifactId>basebackend-user-service</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>basebackend-user-service</name>
    <description>用户管理微服务</description>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring-boot.version>3.1.5</spring-boot.version>
        <spring-cloud.version>2022.0.4</spring-cloud.version>
        <mybatis-plus.version>3.5.4.1</mybatis-plus.version>
        <mysql.version>8.0.33</mysql.version>
        <druid.version>1.2.20</druid.version>
        <redis.version>3.1.6</redis.version>
        <fastjson.version>2.0.45</fastjson.version>
        <hutool.version>5.8.22</hutool.version>
        <knife4j.version>4.4.0</knife4j.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- Spring Boot Starter Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Starter Data Redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- Spring Cloud Discovery Client -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-discovery-client</artifactId>
        </dependency>

        <!-- Spring Cloud LoadBalancer -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-loadbalancer</artifactId>
        </dependency>

        <!-- MyBatis Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <!-- MySQL Driver -->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>${mysql.version}</version>
        </dependency>

        <!-- Druid 连接池 -->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid-spring-boot-starter</artifactId>
            <version>${druid.version}</version>
        </dependency>

        <!-- Spring Boot Starter Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Spring Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.3</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.3</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.3</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Knife4j API 文档 -->
        <dependency>
            <groupId>com.github.xiaoymin</groupId>
            <artifactId>knife4j-openapi3-spring-boot-starter</artifactId>
            <version>${knife4j.version}</version>
        </dependency>

        <!-- FastJSON -->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson2</artifactId>
            <version>${fastjson.version}</version>
        </dependency>

        <!-- Hutool 工具包 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Spring Boot Actuator -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring-boot.version}</version>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 步骤 2: 创建启动类

#### 2.1 UserServiceApplication.java

```java
package com.basebackend.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 用户服务启动类
 *
 * @author 浮浮酱
 * @since 2025-11-15
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableTransactionManagement
@EnableCaching
@MapperScan("com.basebackend.user.mapper")
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
        System.out.println("🚀 用户服务启动成功!");
        System.out.println("📖 API文档: http://localhost:8081/doc.html");
    }
}
```

### 步骤 3: 迁移实体类

#### 3.1 User.java

```java
package com.basebackend.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * @author 浮浮酱
 * @since 2025-11-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("users")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 密码
     */
    @TableField("password")
    private String password;

    /**
     * 昵称
     */
    @TableField("nickname")
    private String nickname;

    /**
     * 邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 手机号
     */
    @TableField("mobile")
    private String mobile;

    /**
     * 头像
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 性别 0:未知 1:男 2:女
     */
    @TableField("gender")
    private Integer gender;

    /**
     * 生日
     */
    @TableField("birthday")
    private LocalDateTime birthday;

    /**
     * 状态 0:禁用 1:正常
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建者
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 更新者
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 是否删除 0:未删除 1:已删除
     */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;
}
```

#### 3.2 UserDTO.java

```java
package com.basebackend.user.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户DTO
 *
 * @author 浮浮酱
 * @since 2025-11-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserDTO extends User {

    private static final long serialVersionUID = 1L;

    /**
     * 角色ID列表
     */
    private Long[] roleIds;

    /**
     * 是否需要修改密码
     */
    private Boolean needChangePassword;
}
```

#### 3.3 UserVO.java

```java
package com.basebackend.user.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户VO
 *
 * @author 浮浮酱
 * @since 2025-11-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserVO extends User {

    private static final long serialVersionUID = 1L;

    /**
     * 角色名称列表
     */
    private List<String> roleNames;

    /**
     * 角色ID列表
     */
    private List<Long> roleIdList;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 登录次数
     */
    private Integer loginCount;
}
```

### 步骤 4: 创建 Mapper

#### 4.1 UserMapper.java

```java
package com.basebackend.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户Mapper接口
 *
 * @author 浮浮酱
 * @since 2025-11-15
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    User selectByUsername(@Param("username") String username);

    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱
     * @return 用户信息
     */
    User selectByEmail(@Param("email") String email);

    /**
     * 根据手机号查询用户
     *
     * @param mobile 手机号
     * @return 用户信息
     */
    User selectByMobile(@Param("mobile") String mobile);

    /**
     * 统计用户总数
     *
     * @return 用户总数
     */
    Long countAll();

    /**
     * 查询今日新增用户数
     *
     * @return 新增用户数
     */
    Long countTodayNew();
}
```

#### 4.2 UserMapper.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.basebackend.user.mapper.UserMapper">

    <!-- 通用查询映射结果 -->
    <resultMap id="BaseResultMap" type="com.basebackend.user.entity.User">
        <id column="id" property="id" jdbcType="BIGINT"/>
        <result column="username" property="username" jdbcType="VARCHAR"/>
        <result column="password" property="password" jdbcType="VARCHAR"/>
        <result column="nickname" property="nickname" jdbcType="VARCHAR"/>
        <result column="email" property="email" jdbcType="VARCHAR"/>
        <result column="mobile" property="mobile" jdbcType="VARCHAR"/>
        <result column="avatar" property="avatar" jdbcType="VARCHAR"/>
        <result column="gender" property="gender" jdbcType="INTEGER"/>
        <result column="birthday" property="birthday" jdbcType="TIMESTAMP"/>
        <result column="status" property="status" jdbcType="INTEGER"/>
        <result column="create_time" property="createTime" jdbcType="TIMESTAMP"/>
        <result column="update_time" property="updateTime" jdbcType="TIMESTAMP"/>
        <result column="create_by" property="createBy" jdbcType="VARCHAR"/>
        <result column="update_by" property="updateBy" jdbcType="VARCHAR"/>
        <result column="is_deleted" property="isDeleted" jdbcType="INTEGER"/>
        <result column="remark" property="remark" jdbcType="VARCHAR"/>
    </resultMap>

    <!-- 通用字段 -->
    <sql id="Base_Column_List">
        id, username, password, nickname, email, mobile, avatar, gender, birthday, status,
        create_time, update_time, create_by, update_by, is_deleted, remark
    </sql>

    <!-- 根据用户名查询用户 -->
    <select id="selectByUsername" parameterType="java.lang.String" resultMap="BaseResultMap">
        SELECT <include refid="Base_Column_List"/>
        FROM users
        WHERE username = #{username} AND is_deleted = 0
    </select>

    <!-- 根据邮箱查询用户 -->
    <select id="selectByEmail" parameterType="java.lang.String" resultMap="BaseResultMap">
        SELECT <include refid="Base_Column_List"/>
        FROM users
        WHERE email = #{email} AND is_deleted = 0
    </select>

    <!-- 根据手机号查询用户 -->
    <select id="selectByMobile" parameterType="java.lang.String" resultMap="BaseResultMap">
        SELECT <include refid="Base_Column_List"/>
        FROM users
        WHERE mobile = #{mobile} AND is_deleted = 0
    </select>

    <!-- 统计用户总数 -->
    <select id="countAll" resultType="java.lang.Long">
        SELECT COUNT(*) FROM users WHERE is_deleted = 0
    </select>

    <!-- 查询今日新增用户数 -->
    <select id="countTodayNew" resultType="java.lang.Long">
        SELECT COUNT(*) FROM users
        WHERE DATE(create_time) = CURDATE() AND is_deleted = 0
    </select>

</mapper>
```

### 步骤 5: 创建 Service

#### 5.1 UserService.java

```java
package com.basebackend.user.service;

import com.basebackend.user.entity.User;
import com.basebackend.user.entity.UserDTO;
import com.basebackend.user.entity.UserVO;
import com.basebackend.common.PageResult;

/**
 * 用户服务接口
 *
 * @author 浮浮酱
 * @since 2025-11-15
 */
public interface UserService {

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 用户信息
     */
    User login(String username, String password);

    /**
     * 用户注册
     *
     * @param userDTO 用户DTO
     * @return 是否成功
     */
    boolean register(UserDTO userDTO);

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户信息
     */
    UserVO getUserById(Long id);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    User getUserByUsername(String username);

    /**
     * 创建用户
     *
     * @param userDTO 用户DTO
     * @return 用户ID
     */
    Long createUser(UserDTO userDTO);

    /**
     * 更新用户
     *
     * @param userDTO 用户DTO
     * @return 是否成功
     */
    boolean updateUser(UserDTO userDTO);

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 是否成功
     */
    boolean deleteUser(Long id);

    /**
     * 分页查询用户列表
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param keyword  搜索关键词
     * @return 用户列表
     */
    PageResult<UserVO> listUsers(int pageNum, int pageSize, String keyword);

    /**
     * 修改密码
     *
     * @param userId   用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 是否成功
     */
    boolean changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 重置密码
     *
     * @param id       用户ID
     * @param password 新密码
     * @return 是否成功
     */
    boolean resetPassword(Long id, String password);

    /**
     * 修改状态
     *
     * @param id     用户ID
     * @param status 状态
     * @return 是否成功
     */
    boolean changeStatus(Long id, Integer status);

    /**
     * 分配角色
     *
     * @param userId   用户ID
     * @param roleIds  角色ID列表
     * @return 是否成功
     */
    boolean assignRoles(Long userId, Long[] roleIds);

    /**
     * 批量删除用户
     *
     * @param ids 用户ID列表
     * @return 是否成功
     */
    boolean batchDelete(Long[] ids);
}
```

#### 5.2 UserServiceImpl.java

```java
package com.basebackend.user.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.user.entity.User;
import com.basebackend.user.entity.UserDTO;
import com.basebackend.user.entity.UserVO;
import com.basebackend.user.mapper.UserMapper;
import com.basebackend.user.service.UserService;
import com.basebackend.common.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 *
 * @author 浮浮酱
 * @since 2025-11-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public User login(String username, String password) {
        log.info("用户登录请求: {}", username);

        // 查询用户
        User user = userMapper.selectByUsername(username);
        if (ObjectUtil.isNull(user)) {
            log.warn("用户名不存在: {}", username);
            return null;
        }

        // 检查状态
        if (user.getStatus() != 1) {
            log.warn("用户已被禁用: {}", username);
            return null;
        }

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("密码错误: {}", username);
            return null;
        }

        // 清空密码
        user.setPassword(null);

        // 缓存用户信息
        cacheUserInfo(user);

        log.info("用户登录成功: {}", username);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean register(UserDTO userDTO) {
        log.info("用户注册请求: {}", userDTO.getUsername());

        // 检查用户名是否已存在
        User existUser = userMapper.selectByUsername(userDTO.getUsername());
        if (ObjectUtil.isNotNull(existUser)) {
            log.warn("用户名已存在: {}", userDTO.getUsername());
            return false;
        }

        // 加密密码
        userDTO.setPassword(passwordEncoder.encode(userDTO.getPassword()));

        // 设置默认值
        userDTO.setStatus(1);
        userDTO.setCreateTime(LocalDateTime.now());
        userDTO.setUpdateTime(LocalDateTime.now());
        userDTO.setCreateBy("system");
        userDTO.setUpdateBy("system");

        // 保存用户
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        int result = userMapper.insert(user);

        if (result > 0) {
            // 分配默认角色（如果需要）
            if (ObjectUtil.isNotEmpty(userDTO.getRoleIds())) {
                assignRoles(user.getId(), userDTO.getRoleIds());
            }

            log.info("用户注册成功: {}", user.getUsername());
            return true;
        }

        log.error("用户注册失败: {}", userDTO.getUsername());
        return false;
    }

    @Override
    @Cacheable(value = "user", key = "#id")
    public UserVO getUserById(Long id) {
        log.debug("查询用户信息: {}", id);

        User user = userMapper.selectById(id);
        if (ObjectUtil.isNull(user)) {
            return null;
        }

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);

        // 查询角色信息（此处简化，实际应查询角色表）
        // List<String> roleNames = queryRoleNamesByUserId(id);
        // userVO.setRoleNames(roleNames);

        return userVO;
    }

    @Override
    @Cacheable(value = "username", key = "#username")
    public User getUserByUsername(String username) {
        log.debug("根据用户名查询用户: {}", username);
        return userMapper.selectByUsername(username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserDTO userDTO) {
        log.info("创建用户: {}", userDTO.getUsername());

        // 检查用户名是否已存在
        User existUser = userMapper.selectByUsername(userDTO.getUsername());
        if (ObjectUtil.isNotNull(existUser)) {
            log.warn("用户名已存在: {}", userDTO.getUsername());
            return null;
        }

        // 加密密码
        userDTO.setPassword(passwordEncoder.encode(userDTO.getPassword()));

        // 设置默认值
        userDTO.setStatus(1);
        userDTO.setCreateTime(LocalDateTime.now());
        userDTO.setUpdateTime(LocalDateTime.now());
        userDTO.setCreateBy("admin");
        userDTO.setUpdateBy("admin");

        // 保存用户
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        int result = userMapper.insert(user);

        if (result > 0) {
            // 分配角色
            if (ObjectUtil.isNotEmpty(userDTO.getRoleIds())) {
                assignRoles(user.getId(), userDTO.getRoleIds());
            }

            // 清除缓存
            redisTemplate.delete("user:" + user.getId());
            redisTemplate.delete("username:" + user.getUsername());

            log.info("创建用户成功: {}", user.getUsername());
            return user.getId();
        }

        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "user", key = "#userDTO.id")
    public boolean updateUser(UserDTO userDTO) {
        log.info("更新用户: {}", userDTO.getId());

        User existUser = userMapper.selectById(userDTO.getId());
        if (ObjectUtil.isNull(existUser)) {
            log.warn("用户不存在: {}", userDTO.getId());
            return false;
        }

        // 不允许更新密码（密码需要单独修改）
        userDTO.setPassword(null);
        userDTO.setUpdateTime(LocalDateTime.now());
        userDTO.setUpdateBy("admin");

        User user = new User();
        BeanUtils.copyProperties(userDTO, user);

        int result = userMapper.updateById(user);

        if (result > 0) {
            // 清除缓存
            redisTemplate.delete("user:" + user.getId());
            redisTemplate.delete("username:" + user.getUsername());

            log.info("更新用户成功: {}", user.getUsername());
            return true;
        }

        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "user", key = "#id")
    public boolean deleteUser(Long id) {
        log.info("删除用户: {}", id);

        User user = userMapper.selectById(id);
        if (ObjectUtil.isNull(user)) {
            log.warn("用户不存在: {}", id);
            return false;
        }

        int result = userMapper.deleteById(id);

        if (result > 0) {
            // 清除缓存
            redisTemplate.delete("user:" + id);
            redisTemplate.delete("username:" + user.getUsername());

            log.info("删除用户成功: {}", user.getUsername());
            return true;
        }

        return false;
    }

    @Override
    public PageResult<UserVO> listUsers(int pageNum, int pageSize, String keyword) {
        log.debug("分页查询用户列表: pageNum={}, pageSize={}, keyword={}",
            pageNum, pageSize, keyword);

        Page<User> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                .or().like(User::getNickname, keyword)
                .or().like(User::getEmail, keyword)
                .or().like(User::getMobile, keyword));
        }
        wrapper.orderByDesc(User::getCreateTime);

        Page<User> result = userMapper.selectPage(page, wrapper);

        List<UserVO> userVOList = result.getRecords().stream().map(user -> {
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            // 查询角色信息
            // List<String> roleNames = queryRoleNamesByUserId(user.getId());
            // userVO.setRoleNames(roleNames);
            return userVO;
        }).collect(Collectors.toList());

        return PageResult.of(userVOList, result.getTotal(), pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        log.info("修改密码: {}", userId);

        User user = userMapper.selectById(userId);
        if (ObjectUtil.isNull(user)) {
            log.warn("用户不存在: {}", userId);
            return false;
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            log.warn("旧密码错误: {}", userId);
            return false;
        }

        // 加密新密码
        String encodedPassword = passwordEncoder.encode(newPassword);

        // 更新密码
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setPassword(encodedPassword);
        updateUser.setUpdateTime(LocalDateTime.now());
        updateUser.setUpdateBy(user.getUsername());

        int result = userMapper.updateById(updateUser);

        if (result > 0) {
            // 清除缓存
            redisTemplate.delete("user:" + userId);
            redisTemplate.delete("username:" + user.getUsername());

            log.info("修改密码成功: {}", user.getUsername());
            return true;
        }

        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "user", key = "#id")
    public boolean resetPassword(Long id, String password) {
        log.info("重置密码: {}", id);

        // 加密密码
        String encodedPassword = passwordEncoder.encode(password);

        User updateUser = new User();
        updateUser.setId(id);
        updateUser.setPassword(encodedPassword);
        updateUser.setUpdateTime(LocalDateTime.now());
        updateUser.setUpdateBy("admin");

        int result = userMapper.updateById(updateUser);

        if (result > 0) {
            // 清除缓存
            redisTemplate.delete("user:" + id);

            log.info("重置密码成功: {}", id);
            return true;
        }

        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "user", key = "#id")
    public boolean changeStatus(Long id, Integer status) {
        log.info("修改用户状态: {} -> {}", id, status);

        User updateUser = new User();
        updateUser.setId(id);
        updateUser.setStatus(status);
        updateUser.setUpdateTime(LocalDateTime.now());
        updateUser.setUpdateBy("admin");

        int result = userMapper.updateById(updateUser);

        if (result > 0) {
            // 清除缓存
            redisTemplate.delete("user:" + id);

            log.info("修改用户状态成功: {}", id);
            return true;
        }

        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignRoles(Long userId, Long[] roleIds) {
        log.info("分配用户角色: {} -> {}", userId, roleIds);

        // TODO: 实现角色分配逻辑
        // 1. 删除现有角色关联
        // 2. 插入新角色关联

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "user", key = "#ids")
    public boolean batchDelete(Long[] ids) {
        log.info("批量删除用户: {}", ids);

        int count = userMapper.deleteBatchIds(java.util.Arrays.asList(ids));

        if (count > 0) {
            // 清除缓存
            for (Long id : ids) {
                redisTemplate.delete("user:" + id);
            }

            log.info("批量删除用户成功: {} 条", count);
            return true;
        }

        return false;
    }

    /**
     * 缓存用户信息
     *
     * @param user 用户信息
     */
    private void cacheUserInfo(User user) {
        // 缓存用户信息（60分钟）
        redisTemplate.opsForValue().set("user:" + user.getId(), user, 3600);
        redisTemplate.opsForValue().set("username:" + user.getUsername(), user, 3600);
    }
}
```

### 步骤 6: 创建 Controller

#### 6.1 UserController.java

```java
package com.basebackend.user.controller;

import com.basebackend.user.entity.User;
import com.basebackend.user.entity.UserDTO;
import com.basebackend.user.entity.UserVO;
import com.basebackend.user.service.UserService;
import com.basebackend.common.PageResult;
import com.basebackend.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 用户控制器
 *
 * @author 浮浮酱
 * @since 2025-11-15
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "用户管理", description = "用户管理相关接口")
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户登录", description = "用户登录接口")
    @PostMapping("/login")
    public Result<User> login(
            @Parameter(description = "用户名", required = true)
            @NotBlank(message = "用户名不能为空")
            @RequestParam String username,

            @Parameter(description = "密码", required = true)
            @NotBlank(message = "密码不能为空")
            @RequestParam String password) {

        log.info("用户登录请求: {}", username);
        User user = userService.login(username, password);

        if (ObjectUtil.isNotNull(user)) {
            return Result.success(user);
        } else {
            return Result.error("用户名或密码错误");
        }
    }

    @Operation(summary = "用户注册", description = "用户注册接口")
    @PostMapping("/register")
    public Result<Long> register(
            @Parameter(description = "用户信息", required = true)
            @Valid @RequestBody UserDTO userDTO) {

        log.info("用户注册请求: {}", userDTO.getUsername());
        boolean success = userService.register(userDTO);

        if (success) {
            return Result.success("注册成功");
        } else {
            return Result.error("用户名已存在");
        }
    }

    @Operation(summary = "获取用户详情", description = "根据ID获取用户详情")
    @GetMapping("/{id}")
    public Result<UserVO> getUserById(
            @Parameter(description = "用户ID", required = true)
            @NotNull(message = "用户ID不能为空")
            @PathVariable Long id) {

        UserVO userVO = userService.getUserById(id);
        if (ObjectUtil.isNotNull(userVO)) {
            return Result.success(userVO);
        } else {
            return Result.error("用户不存在");
        }
    }

    @Operation(summary = "创建用户", description = "创建新用户")
    @PostMapping
    public Result<Long> createUser(
            @Parameter(description = "用户信息", required = true)
            @Valid @RequestBody UserDTO userDTO) {

        log.info("创建用户请求: {}", userDTO.getUsername());
        Long userId = userService.createUser(userDTO);

        if (ObjectUtil.isNotNull(userId)) {
            return Result.success(userId);
        } else {
            return Result.error("用户名已存在");
        }
    }

    @Operation(summary = "更新用户", description = "更新用户信息")
    @PutMapping("/{id}")
    public Result<Void> updateUser(
            @Parameter(description = "用户ID", required = true)
            @NotNull(message = "用户ID不能为空")
            @PathVariable Long id,

            @Parameter(description = "用户信息", required = true)
            @Valid @RequestBody UserDTO userDTO) {

        log.info("更新用户请求: {}", id);
        userDTO.setId(id);
        boolean success = userService.updateUser(userDTO);

        if (success) {
            return Result.success("更新成功");
        } else {
            return Result.error("更新失败");
        }
    }

    @Operation(summary = "删除用户", description = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(
            @Parameter(description = "用户ID", required = true)
            @NotNull(message = "用户ID不能为空")
            @PathVariable Long id) {

        log.info("删除用户请求: {}", id);
        boolean success = userService.deleteUser(id);

        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败");
        }
    }

    @Operation(summary = "分页查询用户列表", description = "分页查询用户列表")
    @GetMapping
    public Result<PageResult<UserVO>> listUsers(
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") int pageNum,

            @Parameter(description = "每页大小", example = "10")
            @RequestParam(defaultValue = "10") int pageSize,

            @Parameter(description = "搜索关键词")
            @RequestParam(required = false) String keyword) {

        log.debug("分页查询用户列表: pageNum={}, pageSize={}, keyword={}",
            pageNum, pageSize, keyword);

        PageResult<UserVO> result = userService.listUsers(pageNum, pageSize, keyword);
        return Result.success(result);
    }

    @Operation(summary = "修改密码", description = "修改用户密码")
    @PutMapping("/{id}/password")
    public Result<Void> changePassword(
            @Parameter(description = "用户ID", required = true)
            @NotNull(message = "用户ID不能为空")
            @PathVariable Long id,

            @Parameter(description = "旧密码", required = true)
            @NotBlank(message = "旧密码不能为空")
            @RequestParam String oldPassword,

            @Parameter(description = "新密码", required = true)
            @NotBlank(message = "新密码不能为空")
            @RequestParam String newPassword) {

        log.info("修改密码请求: {}", id);
        boolean success = userService.changePassword(id, oldPassword, newPassword);

        if (success) {
            return Result.success("密码修改成功");
        } else {
            return Result.error("密码修改失败");
        }
    }

    @Operation(summary = "重置密码", description = "重置用户密码")
    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(
            @Parameter(description = "用户ID", required = true)
            @NotNull(message = "用户ID不能为空")
            @PathVariable Long id,

            @Parameter(description = "新密码", required = true)
            @NotBlank(message = "新密码不能为空")
            @RequestParam String password) {

        log.info("重置密码请求: {}", id);
        boolean success = userService.resetPassword(id, password);

        if (success) {
            return Result.success("密码重置成功");
        } else {
            return Result.error("密码重置失败");
        }
    }

    @Operation(summary = "修改状态", description = "修改用户状态")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(
            @Parameter(description = "用户ID", required = true)
            @NotNull(message = "用户ID不能为空")
            @PathVariable Long id,

            @Parameter(description = "状态", required = true)
            @NotNull(message = "状态不能为空")
            @RequestParam Integer status) {

        log.info("修改用户状态请求: {} -> {}", id, status);
        boolean success = userService.changeStatus(id, status);

        if (success) {
            return Result.success("状态修改成功");
        } else {
            return Result.error("状态修改失败");
        }
    }

    @Operation(summary = "分配角色", description = "为用户分配角色")
    @PutMapping("/{id}/roles")
    public Result<Void> assignRoles(
            @Parameter(description = "用户ID", required = true)
            @NotNull(message = "用户ID不能为空")
            @PathVariable Long id,

            @Parameter(description = "角色ID列表", required = true)
            @NotEmpty(message = "角色ID列表不能为空")
            @RequestParam Long[] roleIds) {

        log.info("分配用户角色请求: {} -> {}", id, roleIds);
        boolean success = userService.assignRoles(id, roleIds);

        if (success) {
            return Result.success("角色分配成功");
        } else {
            return Result.error("角色分配失败");
        }
    }

    @Operation(summary = "批量删除用户", description = "批量删除用户")
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(
            @Parameter(description = "用户ID列表", required = true)
            @NotEmpty(message = "用户ID列表不能为空")
            @RequestParam Long[] ids) {

        log.info("批量删除用户请求: {}", ids);
        boolean success = userService.batchDelete(ids);

        if (success) {
            return Result.success("批量删除成功");
        } else {
            return Result.error("批量删除失败");
        }
    }
}
```

### 步骤 7: 创建 Feign 客户端

#### 7.1 创建 basebackend-user-service-api 模块

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.basebackend</groupId>
    <artifactId>basebackend-user-service-api</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>basebackend-user-service-api</name>
    <description>用户服务API</description>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- Spring Boot Starter Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Cloud OpenFeign -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>

        <!-- Swagger -->
        <dependency>
            <groupId>io.swagger.core.v3</groupId>
            <artifactId>swagger-annotations</artifactId>
            <version>2.2.20</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

#### 7.2 UserServiceClient.java

```java
package com.basebackend.user.api;

import com.basebackend.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 用户服务Feign客户端
 *
 * @author 浮浮酱
 * @since 2025-11-15
 */
@FeignClient(name = "basebackend-user-service", path = "/api/users")
public interface UserServiceClient {

    @Operation(summary = "用户登录", description = "Feign调用用户登录接口")
    @PostMapping("/login")
    Result<com.basebackend.user.api.dto.UserDTO> login(
            @Parameter(description = "用户名", required = true)
            @NotBlank(message = "用户名不能为空")
            @RequestParam String username,

            @Parameter(description = "密码", required = true)
            @NotBlank(message = "密码不能为空")
            @RequestParam String password);

    @Operation(summary = "用户注册", description = "Feign调用用户注册接口")
    @PostMapping("/register")
    Result<Long> register(
            @Parameter(description = "用户信息", required = true)
            @Valid @RequestBody com.basebackend.user.api.dto.RegisterDTO registerDTO);

    @Operation(summary = "获取用户详情", description = "Feign调用获取用户详情接口")
    @GetMapping("/{id}")
    Result<com.basebackend.user.api.dto.UserVO> getUserById(
            @Parameter(description = "用户ID", required = true)
            @NotNull(message = "用户ID不能为空")
            @PathVariable Long id);

    @Operation(summary = "根据用户名查询用户", description = "Feign调用根据用户名查询用户接口")
    @GetMapping("/username/{username}")
    Result<com.basebackend.user.api.dto.UserDTO> getUserByUsername(
            @Parameter(description = "用户名", required = true)
            @NotBlank(message = "用户名不能为空")
            @PathVariable String username);

    @Operation(summary = "创建用户", description = "Feign调用创建用户接口")
    @PostMapping
    Result<Long> createUser(
            @Parameter(description = "用户信息", required = true)
            @Valid @RequestBody com.basebackend.user.api.dto.UserDTO userDTO);

    @Operation(summary = "更新用户", description = "Feign调用更新用户接口")
    @PutMapping("/{id}")
    Result<Void> updateUser(
            @Parameter(description = "用户ID", required = true)
            @NotNull(message = "用户ID不能为空")
            @PathVariable Long id,

            @Parameter(description = "用户信息", required = true)
            @Valid @RequestBody com.basebackend.user.api.dto.UserDTO userDTO);

    @Operation(summary = "删除用户", description = "Feign调用删除用户接口")
    @DeleteMapping("/{id}")
    Result<Void> deleteUser(
            @Parameter(description = "用户ID", required = true)
            @NotNull(message = "用户ID不能为空")
            @PathVariable Long id);

    @Operation(summary = "修改密码", description = "Feign调用修改密码接口")
    @PutMapping("/{id}/password")
    Result<Void> changePassword(
            @Parameter(description = "用户ID", required = true)
            @NotNull(message = "用户ID不能为空")
            @PathVariable Long id,

            @Parameter(description = "旧密码", required = true)
            @NotBlank(message = "旧密码不能为空")
            @RequestParam String oldPassword,

            @Parameter(description = "新密码", required = true)
            @NotBlank(message = "新密码不能为空")
            @RequestParam String newPassword);

    @Operation(summary = "重置密码", description = "Feign调用重置密码接口")
    @PutMapping("/{id}/reset-password")
    Result<Void> resetPassword(
            @Parameter(description = "用户ID", required = true)
            @NotNull(message = "用户ID不能为空")
            @PathVariable Long id,

            @Parameter(description = "新密码", required = true)
            @NotBlank(message = "新密码不能为空")
            @RequestParam String password);

    @Operation(summary = "修改状态", description = "Feign调用修改状态接口")
    @PutMapping("/{id}/status")
    Result<Void> changeStatus(
            @Parameter(description = "用户ID", required = true)
            @NotNull(message = "用户ID不能为空")
            @PathVariable Long id,

            @Parameter(description = "状态", required = true)
            @NotNull(message = "状态不能为空")
            @RequestParam Integer status);

    @Operation(summary = "分配角色", description = "Feign调用分配角色接口")
    @PutMapping("/{id}/roles")
    Result<Void> assignRoles(
            @Parameter(description = "用户ID", required = true)
            @NotNull(message = "用户ID不能为空")
            @PathVariable Long id,

            @Parameter(description = "角色ID列表", required = true)
            @NotEmpty(message = "角色ID列表不能为空")
            @RequestParam Long[] roleIds);
}
```

### 步骤 8: 配置 application.yml

#### 8.1 application.yml

```yaml
server:
  port: 8081
  servlet:
    context-path: /

spring:
  application:
    name: basebackend-user-service

  # 数据源配置
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/basebackend_user?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:root}
    type: com.alibaba.druid.pool.DruidDataSource

    druid:
      initial-size: 5
      min-idle: 5
      max-active: 20
      max-wait: 60000
      time-between-eviction-runs-millis: 60000
      min-evictable-idle-time-millis: 300000
      validation-query: SELECT 1 FROM DUAL
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false
      pool-prepared-statements: true
      max-pool-prepared-statement-per-connection-size: 20
      stat-view-servlet:
        enabled: true
        url-pattern: /druid/*
        login-username: admin
        login-password: admin123
      filter:
        stat:
          enabled: true
          log-slow-sql: true
          slow-sql-millis: 2000
        wall:
          enabled: true
          config:
            multi-statement-allow: true

  # Redis 配置
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    database: ${REDIS_DB:0}
    timeout: 5000ms
    lettuce:
      pool:
        max-active: 20
        max-wait: -1
        max-idle: 10
        min-idle: 5

  # 云注册中心配置
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR:localhost:8848}
        namespace: ${NACOS_NAMESPACE:basebackend}
        group: ${NACOS_GROUP:DEFAULT_GROUP}
        metadata:
          version: 1.0.0
          zone: zone-1

# MyBatis Plus 配置
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: ASSIGN_ID
      logic-delete-field: is_deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
      insert-strategy: NOT_NULL
      update-strategy: NOT_NULL
      select-strategy: NOT_EMPTY
  mapper-locations: classpath*:mapper/**/*.xml

# Knife4j API文档配置
knife4j:
  enable: true
  basic:
    enable: false

# 日志配置
logging:
  level:
    com.basebackend.user: debug
    com.basebackend.user.mapper: debug
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n"
  file:
    name: logs/user-service.log
    max-size: 100MB
    max-history: 30

# 管理端点配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    export:
      prometheus:
        enabled: true
```

### 步骤 9: 创建数据库脚本

#### 9.1 V1__Create_users_table.sql

```sql
-- 用户表
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `mobile` varchar(20) DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  `gender` tinyint DEFAULT 0 COMMENT '性别 0:未知 1:男 2:女',
  `birthday` datetime DEFAULT NULL COMMENT '生日',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 0:禁用 1:正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `update_by` varchar(50) NOT NULL DEFAULT 'system' COMMENT '更新者',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除 0:未删除 1:已删除',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`) USING BTREE,
  UNIQUE KEY `uk_email` (`email`) USING BTREE,
  UNIQUE KEY `uk_mobile` (`mobile`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
```

#### 9.2 V2__Create_user_roles_table.sql

```sql
-- 用户角色关联表
CREATE TABLE `user_roles` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_role_id` (`role_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 插入默认管理员用户
INSERT INTO `users` (`id`, `username`, `password`, `nickname`, `email`, `mobile`, `avatar`, `gender`, `birthday`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`, `remark`) VALUES
(1, 'admin', '$2a$10$7JB720yubVSOfvamj/hzXeG7H/ihz1J4h4vZJz7L8YJzJ4h4vZJz', '管理员', 'admin@example.com', '13800138000', NULL, 1, '1990-01-01 00:00:00', 1, NOW(), NOW(), 'system', 'system', 0, '默认管理员账户');
```

### 步骤 10: 配置 Gateway 路由

#### 10.1 user-service-routes.yml

```yaml
# 用户服务路由配置
spring:
  cloud:
    gateway:
      routes:
        # 用户服务路由
        - id: user-service
          uri: lb://basebackend-user-service
          predicates:
            - Path=/api/users/**
          filters:
            # 限流
            - name: RequestRateLimiter
              args:
                rate-limiter: "#{@redisRateLimiter}"
                key-resolver: "#{@userKeyResolver}"
            # 重试
            - name: Retry
              args:
                retries: 3
                statuses: INTERNAL_SERVER_ERROR
                methods: GET,POST,PUT,DELETE
                backoff:
                  firstBackoff: 100ms
                  maxBackoff: 1000ms
                  factor: 2
                  basedOnPreviousValue: false
            # 缓存
            - name: CacheRequest
              args:
                name: user-cache
                keyResolver: "#{@userKeyResolver}"
                ttl: 60s
            # 熔断
            - name: CircuitBreaker
              args:
                name: user-circuit-breaker
                fallbackUri: forward:/fallback/users
          metadata:
            stripe: true
```

### 步骤 11: 单元测试

#### 11.1 UserServiceTest.java

```java
package com.basebackend.user.service;

import com.basebackend.user.entity.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户服务测试
 *
 * @author 浮浮酱
 * @since 2025-11-15
 */
@Slf4j
@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    void testUserRegister() {
        log.info("开始测试用户注册");

        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("testuser" + System.currentTimeMillis());
        userDTO.setPassword("123456");
        userDTO.setNickname("测试用户");
        userDTO.setEmail("test@example.com");

        boolean result = userService.register(userDTO);

        assertTrue(result, "用户注册应该成功");
        log.info("用户注册测试通过");
    }

    @Test
    void testUserLogin() {
        log.info("开始测试用户登录");

        // 首先注册一个用户
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("logintest" + System.currentTimeMillis());
        userDTO.setPassword("123456");
        userService.register(userDTO);

        // 然后测试登录
        var user = userService.login(userDTO.getUsername(), "123456");

        assertNotNull(user, "用户登录应该成功");
        assertEquals(userDTO.getUsername(), user.getUsername(), "用户名应该匹配");
        log.info("用户登录测试通过");
    }

    @Test
    void testCreateUser() {
        log.info("开始测试创建用户");

        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("createuser" + System.currentTimeMillis());
        userDTO.setPassword("123456");
        userDTO.setNickname("创建测试用户");
        userDTO.setEmail("create@example.com");

        Long userId = userService.createUser(userDTO);

        assertNotNull(userId, "用户ID应该不为空");
        log.info("创建用户测试通过，用户ID: {}", userId);
    }

    @Test
    void testGetUserById() {
        log.info("开始测试根据ID查询用户");

        // 先创建一个用户
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("queryuser" + System.currentTimeMillis());
        userDTO.setPassword("123456");
        userDTO.setNickname("查询测试用户");
        userDTO.setEmail("query@example.com");

        Long userId = userService.createUser(userDTO);

        // 再查询用户
        var userVO = userService.getUserById(userId);

        assertNotNull(userVO, "用户信息应该不为空");
        log.info("根据ID查询用户测试通过");
    }
}
```

### 步骤 12: 部署脚本

#### 12.1 start-user-service.sh

```bash
#!/bin/bash
# 用户服务启动脚本

set -e

echo "=================================="
echo "启动用户服务"
echo "=================================="

# 配置变量
SERVICE_NAME="basebackend-user-service"
SERVICE_PORT="8081"
SERVICE_LOG="logs/user-service.log"

# 创建日志目录
mkdir -p logs

# 检查端口是否被占用
if lsof -i :${SERVICE_PORT} > /dev/null 2>&1; then
    echo "警告: 端口 ${SERVICE_PORT} 已被占用"
    echo "尝试停止占用进程..."
    lsof -ti :${SERVICE_PORT} | xargs kill -9
    sleep 2
fi

# 检查数据库连接
echo "检查数据库连接..."
if ! nc -z localhost 3306; then
    echo "错误: 数据库服务不可用"
    exit 1
fi

# 检查Redis连接
echo "检查Redis连接..."
if ! nc -z localhost 6379; then
    echo "错误: Redis服务不可用"
    exit 1
fi

# 检查Nacos连接
echo "检查Nacos连接..."
if ! nc -z localhost 8848; then
    echo "错误: Nacos服务不可用"
    exit 1
fi

echo "所有依赖服务正常，启动用户服务..."

# 启动服务
nohup java -Xms512m -Xmx1024m \
    -jar target/basebackend-user-service-1.0.0.jar \
    --server.port=${SERVICE_PORT} \
    > ${SERVICE_LOG} 2>&1 &

# 获取进程ID
SERVICE_PID=$!
echo "用户服务已启动，PID: ${SERVICE_PID}"

# 等待服务启动
echo "等待服务启动..."
sleep 10

# 检查服务状态
if curl -f http://localhost:${SERVICE_PORT}/actuator/health > /dev/null 2>&1; then
    echo "✅ 用户服务启动成功!"
    echo "📖 API文档: http://localhost:${SERVICE_PORT}/doc.html"
    echo "🔍 健康检查: http://localhost:${SERVICE_PORT}/actuator/health"
    echo "📊 监控指标: http://localhost:${SERVICE_PORT}/actuator/prometheus"
else
    echo "❌ 用户服务启动失败"
    echo "📋 查看日志: tail -f ${SERVICE_LOG}"
    exit 1
fi

echo "=================================="
echo "启动完成!"
echo "=================================="
```

#### 12.2 stop-user-service.sh

```bash
#!/bin/bash
# 用户服务停止脚本

set -e

echo "=================================="
echo "停止用户服务"
echo "=================================="

# 查找服务进程
SERVICE_PID=$(pgrep -f "basebackend-user-service")

if [ -n "$SERVICE_PID" ]; then
    echo "发现用户服务进程，PID: ${SERVICE_PID}"

    # 优雅停止
    echo "发送TERM信号..."
    kill -TERM ${SERVICE_PID}

    # 等待进程退出
    sleep 5

    # 检查是否仍在运行
    if ps -p ${SERVICE_PID} > /dev/null 2>&1; then
        echo "进程仍在运行，强制杀死..."
        kill -KILL ${SERVICE_PID}
        sleep 2
    fi

    echo "✅ 用户服务已停止"
else
    echo "⚠️  未找到用户服务进程"
fi

# 检查端口是否释放
if lsof -i :8081 > /dev/null 2>&1; then
    echo "⚠️  端口8081仍被占用，尝试清理..."
    lsof -ti :8081 | xargs kill -9
fi

echo "=================================="
echo "停止完成!"
echo "=================================="
```

---

## 🧪 集成测试

### 测试步骤

1. **启动依赖服务**
   ```bash
   # 启动MySQL
   docker-compose up -d mysql

   # 启动Redis
   docker-compose up -d redis

   # 启动Nacos
   docker-compose up -d nacos
   ```

2. **构建用户服务**
   ```bash
   cd basebackend-user-service
   mvn clean package -DskipTests
   ```

3. **运行测试**
   ```bash
   mvn test
   ```

4. **启动服务**
   ```bash
   chmod +x scripts/start-user-service.sh
   ./scripts/start-user-service.sh
   ```

5. **API测试**
   ```bash
   # 注册用户
   curl -X POST http://localhost:8081/api/users/register \
        -H "Content-Type: application/json" \
        -d '{
          "username": "testuser",
          "password": "123456",
          "nickname": "测试用户",
          "email": "test@example.com"
        }'

   # 用户登录
   curl -X POST "http://localhost:8081/api/users/login?username=testuser&password=123456"

   # 查询用户
   curl http://localhost:8081/api/users/1
   ```

### 性能测试

```bash
# 使用JMeter进行性能测试
# 创建测试计划：users.jmx
# 并发用户数：100
# QPS目标：>1000
# 响应时间：P95 < 150ms
```

---

## 📊 监控指标

### Prometheus 指标

```
# 用户服务指标
user_service_requests_total{method="GET",uri="/api/users",status="200"} 1000
user_service_request_duration_seconds{quantile="0.95"} 0.095
user_service_active_connections 50
user_service_database_connections_active 10
user_service_cache_hit_ratio 0.92

# 业务指标
user_registrations_total 500
user_logins_total 2000
user_login_success_ratio 0.98
```

### Grafana 仪表盘

```
# 用户服务仪表盘
- 用户注册趋势
- 登录成功率
- API响应时间分布
- 错误率统计
- 缓存命中率
```

---

## ✅ 验收标准

### 功能验收
- [ ] 用户注册接口正常
- [ ] 用户登录接口正常
- [ ] 用户CRUD接口正常
- [ ] 分页查询接口正常
- [ ] 修改密码接口正常
- [ ] 状态修改接口正常
- [ ] 角色分配接口正常

### 性能验收
- [ ] API响应时间 < 100ms
- [ ] 并发用户数 > 100
- [ ] QPS > 1000
- [ ] 缓存命中率 > 90%
- [ ] 错误率 < 0.1%

### 稳定性验收
- [ ] 服务可用性 > 99.9%
- [ ] 7x24小时稳定运行
- [ ] 内存使用稳定
- [ ] 无内存泄漏
- [ ] 数据库连接池稳定

---

## 🎉 完成标志

当以下条件全部满足时，表示用户服务迁移完成：

1. ✅ **服务独立运行**: 用户服务可独立部署和访问
2. ✅ **数据隔离**: 独立的用户数据库
3. ✅ **接口稳定**: 所有API接口正常工作
4. ✅ **性能达标**: 响应时间、QPS等指标达标
5. ✅ **监控完善**: 完整的监控和告警

---

**浮浮酱相信，通过这个详细的实施指南，用户服务迁移一定能顺利完成！** (*^▽^*)

**下一步**：开始 Phase 10.2 - 权限服务迁移！ ฅ'ω'ฅ
