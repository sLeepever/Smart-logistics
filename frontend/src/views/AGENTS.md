# FRONTEND VIEWS KNOWLEDGE BASE

## OVERVIEW
`src/views` 放路由页面和主布局。这里承接用户可见的业务流程：登录、仪表盘、订单、车辆、调度、追踪、统计、司机任务。

## STRUCTURE
```text
views/
├── MainLayout.vue
├── LoginView.vue
├── Dashboard/Orders/Vehicle/Dispatch/Tracking/Stats
└── DriverTaskView.vue
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| 主布局/菜单 | `MainLayout.vue` | 登录后壳层 |
| 登录页 | `LoginView.vue` | 触发 auth store |
| 仪表盘 | `DashboardView.vue` | KPI/概览 |
| 订单/车辆 | `OrdersView.vue`, `VehicleView.vue` | 运营管理 |
| 调度/追踪 | `DispatchView.vue`, `TrackingView.vue` | 核心业务场景 |
| 统计/司机任务 | `StatsView.vue`, `DriverTaskView.vue` | 角色差异化界面 |

## CONVENTIONS
- 路由页面文件名使用 PascalCase。
- 这些页面通过 `router/index.ts` 懒加载，不要在别处复制第二套路由装配。
- 页面可以直接调用领域 API 模块，但公共请求行为仍应留在 `src/api/`。
- `driver/tasks` 是现有唯一显式的角色化子路径。

## ANTI-PATTERNS
- 不要在每个页面重复写全局鉴权/跳转守卫。
- 不要在页面里硬编码后端地址或 token 处理。
- 不要把共享布局/菜单规则散落到多个 View 文件里。

## NOTES
- `AboutView.vue`、`HomeView.vue` 更像模板遗留；动它们前先确认是否仍在真实路由链路中。
- 角色相关可见性优先放在布局和路由层统一控制，不要在每个页面各自发明一套。
