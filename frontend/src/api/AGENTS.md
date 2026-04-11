# FRONTEND API KNOWLEDGE BASE

## OVERVIEW
`src/api` 是前端请求边界：`request.ts` 管公共 axios 行为，`auth/order/dispatch/tracking/vehicle` 按领域拆分。

## STRUCTURE
```text
api/
├── request.ts
├── auth.ts
├── order.ts
├── dispatch.ts
├── tracking.ts
└── vehicle.ts
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| 公共 axios 实例 | `request.ts` | `baseURL=/api`、超时、拦截器 |
| 登录/刷新 | `auth.ts` | 认证接口 |
| 订单接口 | `order.ts` | 订单 CRUD / 状态 |
| 调度接口 | `dispatch.ts` | 方案/路线/统计 |
| 追踪接口 | `tracking.ts` | 位置与轨迹 |
| 车辆接口 | `vehicle.ts` | 车辆 CRUD / 状态 |

## CONVENTIONS
- 所有业务请求复用 `request.ts`，不要各自创建 axios 实例。
- `request.ts` 负责注入 `Authorization: Bearer <token>`。
- 响应按 `{ code, message, data }` 包装处理；401 在这里统一触发登出。
- 一个领域一个文件，保持模块边界清楚。

## ANTI-PATTERNS
- 不要绕过 `request.ts` 直接写裸 axios 调用。
- 不要在各 API 文件重复 401、消息提示、token 注入逻辑。
- 不要把完整后端 origin 写死到模块里；保持相对路径 + Vite 代理。

## NOTES
- 当前 `request.ts` 依赖 `useAuthStore()` 做 401 登出；新增耦合时尽量别让这一层继续变重。
- 如果后续引入 token 刷新流程，优先仍放在这里统一处理，而不是散落到各领域模块。
