#!/bin/bash
# ============================================================
# smartLogistics 数据库初始化脚本
# 使用方法：
#   bash init-db.sh [postgres_password]
#   例如：bash init-db.sh postgres
# ============================================================

PG_USER="postgres"
PG_HOST="localhost"
PG_PORT="5432"
PG_PASSWORD="${1:-postgres}"

export PGPASSWORD="$PG_PASSWORD"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "========================================"
echo " smartLogistics 数据库初始化"
echo " PostgreSQL: $PG_HOST:$PG_PORT"
echo " 用户: $PG_USER"
echo "========================================"

run_sql() {
    local db="$1"
    local file="$2"
    echo ">>> 执行 $file 到 $db ..."
    psql -U "$PG_USER" -h "$PG_HOST" -p "$PG_PORT" -d "$db" -f "$SCRIPT_DIR/$file"
    if [ $? -ne 0 ]; then
        echo "[ERROR] 执行失败：$file"
        exit 1
    fi
    echo "    完成"
}

# Step 1：创建数据库（连接默认库 postgres）
echo ""
echo "Step 1: 创建四个逻辑数据库..."
psql -U "$PG_USER" -h "$PG_HOST" -p "$PG_PORT" -d postgres -f "$SCRIPT_DIR/00-create-databases.sql"

# Step 2：创建表结构
echo ""
echo "Step 2: 创建表结构..."
run_sql "user_db"     "01-user_db.sql"
run_sql "order_db"    "02-order_db.sql"
run_sql "dispatch_db" "03-dispatch_db.sql"
run_sql "tracking_db" "04-tracking_db.sql"

# Step 3：导入演示数据
echo ""
echo "Step 3: 导入演示数据..."
run_sql "order_db"    "demo-order_db.sql"
run_sql "dispatch_db" "demo-dispatch_db.sql"
run_sql "tracking_db" "demo-tracking_db.sql"

echo ""
echo "========================================"
echo " 数据库初始化完成！"
echo " 注意：用户演示数据由 user-service 启动时"
echo "       DataInitializer 自动写入，密码 Demo@1234"
echo "========================================"

unset PGPASSWORD
