# SQL KNOWLEDGE BASE

## OVERVIEW
`backend/sql` 放本地数据库初始化脚本：建库、建表、demo 数据和一键初始化脚本。

## STRUCTURE
```text
sql/
├── 00-create-databases.sql
├── 01-04-*.sql          # 各服务 schema
├── demo-*.sql           # 演示数据
└── init-db.bat/.sh      # 初始化入口
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| 建库脚本 | `00-create-databases.sql` | 4 个逻辑库 |
| 各服务 DDL | `01-user_db.sql` ~ `04-tracking_db.sql` | 每库一份 schema |
| 演示数据 | `demo-*.sql` | 与 DDL 分离 |
| Windows 初始化 | `init-db.bat` | 交互式输入 PG 密码 |
| Shell 初始化 | `init-db.sh` | 非 Windows 环境 |

## CONVENTIONS
- 一库一服务：`user_db`、`order_db`、`dispatch_db`、`tracking_db`。
- DDL 与 demo 数据分开维护，不要混在同一个脚本里。
- 本地初始化脚本按 UTF-8/本地 PostgreSQL 假设编写。

## ANTI-PATTERNS
- 不要加入跨库 JOIN 方案来“省接口调用”。
- 不要把 demo 数据写回 DDL 文件。
- 不要把中文注释重新塞回 `init-db.bat`；Windows 编码问题已踩过坑。

## NOTES
- `init-db.bat` 执行完后仍需启动 `user-service` 补种用户数据。
- 这里的脚本默认面向本地演示环境，不要当成生产迁移体系来扩展。
