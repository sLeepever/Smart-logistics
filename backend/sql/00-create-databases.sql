-- ============================================================
-- smartLogistics 数据库创建脚本
-- 以 postgres 超级用户身份执行（连接默认库 postgres）
-- 执行方式：psql -U postgres -f 00-create-databases.sql
-- ============================================================

-- 创建四个逻辑数据库
CREATE DATABASE user_db     ENCODING 'UTF8' LC_COLLATE 'zh_CN.UTF-8' LC_CTYPE 'zh_CN.UTF-8' TEMPLATE template0;
CREATE DATABASE order_db    ENCODING 'UTF8' LC_COLLATE 'zh_CN.UTF-8' LC_CTYPE 'zh_CN.UTF-8' TEMPLATE template0;
CREATE DATABASE dispatch_db ENCODING 'UTF8' LC_COLLATE 'zh_CN.UTF-8' LC_CTYPE 'zh_CN.UTF-8' TEMPLATE template0;
CREATE DATABASE tracking_db ENCODING 'UTF8' LC_COLLATE 'zh_CN.UTF-8' LC_CTYPE 'zh_CN.UTF-8' TEMPLATE template0;

-- 授权（按需修改应用程序用户名，本地开发直接用 postgres 即可）
-- GRANT ALL PRIVILEGES ON DATABASE user_db     TO smart_app;
-- GRANT ALL PRIVILEGES ON DATABASE order_db    TO smart_app;
-- GRANT ALL PRIVILEGES ON DATABASE dispatch_db TO smart_app;
-- GRANT ALL PRIVILEGES ON DATABASE tracking_db TO smart_app;
