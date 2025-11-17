package com.basebackend.oauth2.user;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OAuth2.0用户详情
 * 集成Spring Security UserDetails接口
 */
@Data
public class OAuth2UserDetails implements UserDetails {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 性别：0-未知，1-男，2-女
     */
    private Integer gender;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 用户类型：0-系统用户，1-普通用户
     */
    private Integer userType;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 角色列表
     */
    private List<String> roles;

    /**
     * 权限列表
     */
    private List<String> permissions;

    /**
     * 构造函数
     */
    public OAuth2UserBuilder toBuilder() {
        return new OAuth2UserBuilder()
                .userId(this.userId)
                .username(this.username)
                .password(this.password)
                .nickname(this.nickname)
                .email(this.email)
                .phone(this.phone)
                .avatar(this.avatar)
                .gender(this.gender)
                .deptId(this.deptId)
                .deptName(this.deptName)
                .userType(this.userType)
                .status(this.status)
                .roles(this.roles)
                .permissions(this.permissions);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 返回权限列表
        return permissions != null
                ? permissions.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList())
                : List.of();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != null && status == 1;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status != null && status == 1;
    }

    /**
     * 获取角色权限列表
     */
    public List<GrantedAuthority> getRoleAuthorities() {
        return roles != null
                ? roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList())
                : List.of();
    }

    /**
     * 检查是否拥有指定角色
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * 检查是否拥有指定权限
     */
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    /**
     * OAuth2.0用户详情构建器
     */
    public static class OAuth2UserBuilder {
        private Long userId;
        private String username;
        private String password;
        private String nickname;
        private String email;
        private String phone;
        private String avatar;
        private Integer gender;
        private Long deptId;
        private String deptName;
        private Integer userType;
        private Integer status;
        private List<String> roles;
        private List<String> permissions;

        public OAuth2UserBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public OAuth2UserBuilder username(String username) {
            this.username = username;
            return this;
        }

        public OAuth2UserBuilder password(String password) {
            this.password = password;
            return this;
        }

        public OAuth2UserBuilder nickname(String nickname) {
            this.nickname = nickname;
            return this;
        }

        public OAuth2UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public OAuth2UserBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public OAuth2UserBuilder avatar(String avatar) {
            this.avatar = avatar;
            return this;
        }

        public OAuth2UserBuilder gender(Integer gender) {
            this.gender = gender;
            return this;
        }

        public OAuth2UserBuilder deptId(Long deptId) {
            this.deptId = deptId;
            return this;
        }

        public OAuth2UserBuilder deptName(String deptName) {
            this.deptName = deptName;
            return this;
        }

        public OAuth2UserBuilder userType(Integer userType) {
            this.userType = userType;
            return this;
        }

        public OAuth2UserBuilder status(Integer status) {
            this.status = status;
            return this;
        }

        public OAuth2UserBuilder roles(List<String> roles) {
            this.roles = roles;
            return this;
        }

        public OAuth2UserBuilder permissions(List<String> permissions) {
            this.permissions = permissions;
            return this;
        }

        public OAuth2UserDetails build() {
            OAuth2UserDetails userDetails = new OAuth2UserDetails();
            userDetails.setUserId(this.userId);
            userDetails.setUsername(this.username);
            userDetails.setPassword(this.password);
            userDetails.setNickname(this.nickname);
            userDetails.setEmail(this.email);
            userDetails.setPhone(this.phone);
            userDetails.setAvatar(this.avatar);
            userDetails.setGender(this.gender);
            userDetails.setDeptId(this.deptId);
            userDetails.setDeptName(this.deptName);
            userDetails.setUserType(this.userType);
            userDetails.setStatus(this.status);
            userDetails.setRoles(this.roles);
            userDetails.setPermissions(this.permissions);
            return userDetails;
        }
    }
}
