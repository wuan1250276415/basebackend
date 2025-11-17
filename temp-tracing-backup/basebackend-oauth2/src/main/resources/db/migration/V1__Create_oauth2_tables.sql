-- =====================================================================
-- OAuth2.0数据库初始化脚本
-- 创建时间: 2025-11-15
-- 描述: 创建OAuth2.0和OpenID Connect相关数据表
-- =====================================================================

-- OAuth2客户端表
CREATE TABLE IF NOT EXISTS oauth2_registered_client (
    id varchar(100) NOT NULL,
    client_id varchar(100) NOT NULL,
    client_id_issued_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
    client_secret varchar(200) DEFAULT NULL,
    client_secret_expires_at timestamp DEFAULT NULL,
    client_name varchar(200) DEFAULT NULL,
    client_authentication_methods varchar(1000) NOT NULL,
    authorization_grant_types varchar(1000) NOT NULL,
    redirect_uris varchar(1000) DEFAULT NULL,
    post_logout_redirect_uris varchar(1000) DEFAULT NULL,
    scopes varchar(1000) NOT NULL,
    client_settings varchar(2000) NOT NULL,
    token_settings varchar(2000) NOT NULL,
    PRIMARY KEY (id)
);

-- OAuth2授权表
CREATE TABLE IF NOT EXISTS oauth2_authorization (
    id varchar(100) NOT NULL,
    registered_client_id varchar(100) NOT NULL,
    principal_name varchar(200) NOT NULL,
    authorization_grant_type varchar(100) NOT NULL,
    authorized_scopes varchar(1000) DEFAULT NULL,
    attributes blob DEFAULT NULL,
    state varchar(500) DEFAULT NULL,
    authorization_code_value blob DEFAULT NULL,
    authorization_code_issued_at timestamp DEFAULT NULL,
    authorization_code_expires_at timestamp DEFAULT NULL,
    authorization_code_metadata blob DEFAULT NULL,
    access_token_value blob DEFAULT NULL,
    access_token_issued_at timestamp DEFAULT NULL,
    access_token_expires_at timestamp DEFAULT NULL,
    access_token_metadata blob DEFAULT NULL,
    access_token_type varchar(100) DEFAULT NULL,
    access_token_scopes varchar(1000) DEFAULT NULL,
    oidc_id_token_value blob DEFAULT NULL,
    oidc_id_token_issued_at timestamp DEFAULT NULL,
    oidc_id_token_expires_at timestamp DEFAULT NULL,
    oidc_id_token_metadata blob DEFAULT NULL,
    refresh_token_value blob DEFAULT NULL,
    refresh_token_issued_at timestamp DEFAULT NULL,
    refresh_token_expires_at timestamp DEFAULT NULL,
    refresh_token_metadata blob DEFAULT NULL,
    PRIMARY KEY (id)
);

-- OAuth2授权同意表
CREATE TABLE IF NOT EXISTS oauth2_authorization_consent (
    registered_client_id varchar(100) NOT NULL,
    principal_name varchar(200) NOT NULL,
    authorities varchar(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);

-- JWK表（用于存储密钥）
CREATE TABLE IF NOT EXISTS oauth2_jwk (
    id varchar(100) NOT NULL,
    created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
    jwk_set_id varchar(200) NOT NULL,
    jwk_set_name varchar(200) DEFAULT NULL,
    status varchar(20) DEFAULT 'active' NOT NULL,
    jwk blob NOT NULL,
    PRIMARY KEY (id)
);

-- 创建索引
CREATE INDEX idx_oauth2_registered_client_client_id ON oauth2_registered_client(client_id);
CREATE INDEX idx_oauth2_authorization_principal ON oauth2_authorization(principal_name);
CREATE INDEX idx_oauth2_authorization_client ON oauth2_authorization(registered_client_id);
CREATE INDEX idx_oauth2_authorization_consent_principal ON oauth2_authorization_consent(principal_name);
CREATE INDEX idx_oauth2_jwk_set ON oauth2_jwk(jwk_set_id);

-- 插入默认JWK记录
INSERT INTO oauth2_jwk (id, jwk_set_id, jwk_set_name, status, jwk)
VALUES (
    'default-jwk',
    'default-jwk-set',
    'Default JWK Set',
    'active',
    '{"kty":"RSA","e":"AQAB","n":"","kid":"default-key-id","alg":"RS256","use":"sig"}'
);
