# FRONTEND STORES KNOWLEDGE BASE

## OVERVIEW
`src/stores` 当前真正承担业务的是 `auth.ts`；`counter.ts` 更像模板残留，不是核心业务状态入口。

## STRUCTURE
```text
stores/
├── auth.ts
└── counter.ts
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| 登录态/用户信息 | `auth.ts` | accessToken、refreshToken、role 等 |
| 模板示例 | `counter.ts` | 可删/可忽略的模板 store |

## CONVENTIONS
- 使用 setup-style `defineStore()`。
- `auth.ts` 负责维护这些 `sessionStorage` 键：`accessToken`、`refreshToken`、`userId`、`username`、`realName`、`role`。
- 登录成功后的跳转统一在 `roleRouteMap` 内管理。

## ANTI-PATTERNS
- 不要把登录态改成 `localStorage` 而忽略多标签角色互相覆盖问题。
- 不要在多个页面重复写相同的会话持久化键。
- 不要把模板 store 当成真实业务基线继续复制。

## NOTES
- 角色跳转入口在这里；若改路由结构，必须同步检查本文件和 `router/index.ts`。
- 新增全局状态前，先判断是否真的需要 store；很多页面数据更适合留在 `views/` 本地状态。
