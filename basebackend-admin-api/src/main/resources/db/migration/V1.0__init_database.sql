-- ============================
-- Flyway Migration: V1.0
-- Description: 数据库初始化
-- Author: BaseBackend Team
-- Date: 2025-10-23
-- ============================

-- 注意：Flyway会自动连接到指定数据库，所以不需要CREATE DATABASE和USE语句
-- 这些语句会在外部数据库初始化脚本中执行

-- 创建flyway_schema_history表会由Flyway自动创建
-- 此脚本为基线版本，标记数据库迁移的起点
