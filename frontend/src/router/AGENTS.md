# FRONTEND ROUTER KNOWLEDGE BASE

## OVERVIEW
`src/router` 定义页面入口、懒加载关系和最外层前端会话守卫，是前端导航边界。

## STRUCTURE
```text
router/
└── index.ts
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| 路由表 | `index.ts` | `/login`、主布局 children |
| 会话守卫 | `index.ts` | `meta.requiresAuth` + accessToken |
| 页面映射 | `index.ts` | 各 View 的 lazy import |

## CONVENTIONS
- 受保护页面挂在 `MainLayout.vue` 下，并通过 `meta.requiresAuth` 标记。
- 登录态判断只读 `sessionStorage.getItem('accessToken')`；更细粒度权限由后端保证。
- 页面组件统一使用 `@/views/...` 懒加载导入。

## ANTI-PATTERNS
- 不要把前端守卫误当成真实授权校验。
- 不要在页面组件里各自追加第二套路由拦截逻辑。
- 不要改角色跳转路径而忘记同步 `stores/auth.ts`。

## NOTES
- 这里负责导航体验，不负责服务端权限安全；安全边界仍在 gateway / backend。
