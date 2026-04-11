# FRONTEND SRC KNOWLEDGE BASE

## OVERVIEW
`src` 是真实业务代码边界。主线很清晰：`main.ts` 启动应用，`router/` 决定页面流转，`stores/` 管登录态，`api/` 包装请求，`views/` 组织屏幕。

## STRUCTURE
```text
src/
├── api/         # 按领域拆分的 HTTP 客户端
├── router/      # 路由表与守卫
├── stores/      # Pinia store
├── views/       # 路由页面与主布局
├── components/  # 共享组件；当前多为模板遗留
└── assets/      # CSS / logo
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| 应用装配 | `main.ts` | 安装 Pinia、router、Element Plus |
| 请求约定 | `api/request.ts` | 所有接口调用的统一边界 |
| 登录态 | `stores/auth.ts` | sessionStorage 键名与角色跳转 |
| 页面流转 | `router/index.ts` | lazy import + guard |
| 业务界面 | `views/` | Dashboard/Orders/Dispatch/Tracking 等 |

## CONVENTIONS
- `main.ts` 只负责装配，不承载业务逻辑。
- 页面组件通过领域 API 模块取数；不要在 `views/` 里直接 new axios 实例。
- `components/` 当前主要是模板遗留与图标，不是核心业务入口。
- `tsconfig.app.json` 明确排除了 `src/**/__tests__/*`，但仓库目前没有前端测试目录。

## ANTI-PATTERNS
- 不要在页面里复制全局请求/鉴权逻辑。
- 不要把会话存储改回 `localStorage` 而不评估多标签登录问题。
- 不要把业务流程藏进模板遗留组件里。

## NOTES
- 新功能优先沿用现有分层：`views -> api -> backend`，状态共享再考虑加 store。
