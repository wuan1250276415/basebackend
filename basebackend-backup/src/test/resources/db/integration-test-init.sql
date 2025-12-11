-- 集成测试数据库初始化脚本
-- 这个脚本会在TestContainers MySQL启动时自动执行

-- 创建测试用户表
CREATE TABLE IF NOT EXISTS test_users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    age INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 创建测试订单表
CREATE TABLE IF NOT EXISTS test_orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    order_no VARCHAR(50) NOT NULL UNIQUE,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES test_users(id)
);

-- 创建测试产品表
CREATE TABLE IF NOT EXISTS test_products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    stock INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建测试日志表
CREATE TABLE IF NOT EXISTS test_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    level VARCHAR(10) NOT NULL,
    message TEXT NOT NULL,
    logger_name VARCHAR(100),
    thread_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_level (level),
    INDEX idx_created_at (created_at)
);

-- 插入基础测试数据
INSERT INTO test_users (name, email, age) VALUES
('张三', 'zhangsan@test.com', 25),
('李四', 'lisi@test.com', 30),
('王五', 'wangwu@test.com', 28),
('赵六', 'zhaoliu@test.com', 35),
('钱七', 'qianqi@test.com', 22);

INSERT INTO test_products (name, description, price, stock) VALUES
('iPhone 15', '苹果最新款手机', 6999.00, 100),
('MacBook Pro', '苹果笔记本电脑', 12999.00, 50),
('iPad Air', '苹果平板电脑', 4999.00, 80),
('小米13', '小米手机', 3999.00, 200),
('华为P60', '华为手机', 5999.00, 120);

INSERT INTO test_orders (user_id, order_no, amount, status) VALUES
(1, 'ORDER001', 6999.00, 'COMPLETED'),
(2, 'ORDER002', 12999.00, 'PENDING'),
(3, 'ORDER003', 4999.00, 'SHIPPED'),
(1, 'ORDER004', 3999.00, 'COMPLETED'),
(4, 'ORDER005', 5999.00, 'CANCELLED');

-- 创建存储过程（测试mysqldump --routines选项）
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS GetUserOrderCount(IN user_id INT, OUT order_count INT)
BEGIN
    SELECT COUNT(*) INTO order_count FROM test_orders WHERE user_id = user_id;
END //
DELIMITER ;

-- 创建触发器（测试mysqldump --triggers选项）
DELIMITER //
CREATE TRIGGER IF NOT EXISTS update_user_modified
    BEFORE UPDATE ON test_users
    FOR EACH ROW
BEGIN
    SET NEW.updated_at = CURRENT_TIMESTAMP;
END //
DELIMITER ;

-- 创建事件（测试mysqldump --events选项）
SET GLOBAL event_scheduler = ON;
DELIMITER //
CREATE EVENT IF NOT EXISTS cleanup_old_logs
    ON SCHEDULE EVERY 1 DAY
    STARTS CURRENT_DATE + INTERVAL 1 DAY
    DO
BEGIN
    DELETE FROM test_logs WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY);
END //
DELIMITER ;

-- 创建视图（测试复杂查询）
CREATE OR REPLACE VIEW user_order_summary AS
SELECT
    u.id,
    u.name,
    u.email,
    COUNT(o.id) as order_count,
    COALESCE(SUM(o.amount), 0) as total_amount,
    AVG(o.amount) as avg_amount
FROM test_users u
LEFT JOIN test_orders o ON u.id = o.user_id
GROUP BY u.id, u.name, u.email;

-- 创建索引（测试性能优化）
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON test_orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON test_orders(status);
CREATE INDEX IF NOT EXISTS idx_products_price ON test_products(price);
CREATE INDEX IF NOT EXISTS idx_users_email ON test_users(email);

-- 插入更多测试数据以模拟真实场景
INSERT INTO test_logs (level, message, logger_name, thread_name) VALUES
('INFO', 'Application started successfully', 'com.basebackend.BackupApplication', 'main'),
('DEBUG', 'Database connection established', 'com.basebackend.DatabaseService', 'db-pool-thread-1'),
('WARN', 'High memory usage detected', 'com.basebackend.MonitoringService', 'monitor-thread'),
('ERROR', 'Failed to connect to Redis', 'com.basebackend.CacheService', 'cache-thread'),
('INFO', 'Backup job completed successfully', 'com.basebackend.BackupService', 'backup-thread');

COMMIT;
