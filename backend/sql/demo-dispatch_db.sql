-- ============================================================
-- dispatch_db 演示数据
-- 连接 dispatch_db 后执行：psql -U postgres -d dispatch_db -f demo-dispatch_db.sql
-- ============================================================
-- driver_id 3 = 李司机（driver001），driver_id 4 = 王司机（driver002）
-- ============================================================

-- 车辆演示数据（10辆）
INSERT INTO vehicles (id, plate_no, vehicle_type, max_weight, max_volume, driver_id, status)
VALUES
-- 空闲车辆（5辆）
(1,  '粤A12345', '厢式货车', 2000.00, 12.00, 3, 'idle'),
(2,  '粤A23456', '厢式货车', 2000.00, 12.00, 4, 'idle'),
(3,  '粤A34567', '厢式货车', 2000.00, 12.00, NULL, 'idle'),
(4,  '粤A45678', '冷藏车',   1500.00,  8.00, NULL, 'idle'),
(5,  '粤A56789', '冷藏车',   1500.00,  8.00, NULL, 'idle'),
-- 在途车辆（3辆）
(6,  '粤A67890', '厢式货车', 2000.00, 12.00, 3, 'on_route'),
(7,  '粤A78901', '冷藏车',   1500.00,  8.00, 4, 'on_route'),
(8,  '粤A89012', '重型卡车', 5000.00, 30.00, NULL, 'on_route'),
-- 维修中（2辆）
(9,  '粤A90123', '重型卡车', 5000.00, 30.00, NULL, 'maintenance'),
(10, '粤A01234', '冷藏车',   1500.00,  8.00, NULL, 'maintenance');

SELECT setval('vehicles_id_seq', (SELECT MAX(id) FROM vehicles));

-- --------------------------------------------------------
-- 已完成调度方案（含算法对比数据，用于论文图表）
-- --------------------------------------------------------
INSERT INTO dispatch_plans (id, plan_no, status, total_orders, total_routes,
    before_total_distance, after_total_distance, before_vehicle_count, after_vehicle_count,
    algorithm_params, created_by, confirmed_at, completed_at)
VALUES
(1, 'PLN20260312001', 'completed', 10, 2,
    342.50, 285.30, 3, 2,
    '{"popSize":100,"maxIter":200,"mutationRate":0.05,"clusterK":2}'::JSONB,
    2, NOW() - INTERVAL '3 days 4 hours', NOW() - INTERVAL '3 days'),
(2, 'PLN20260313001', 'completed', 8,  2,
    278.80, 231.60, 2, 2,
    '{"popSize":100,"maxIter":200,"mutationRate":0.05,"clusterK":2}'::JSONB,
    2, NOW() - INTERVAL '2 days 3 hours', NOW() - INTERVAL '2 days');

-- 执行中调度方案（司机正在配送）
INSERT INTO dispatch_plans (id, plan_no, status, total_orders, total_routes,
    before_total_distance, after_total_distance, before_vehicle_count, after_vehicle_count,
    algorithm_params, created_by, confirmed_at)
VALUES
(3, 'PLN20260313002', 'executing', 8, 2,
    256.40, 198.75, 2, 2,
    '{"popSize":100,"maxIter":200,"mutationRate":0.05,"clusterK":2}'::JSONB,
    2, NOW() - INTERVAL '6 hours');

SELECT setval('dispatch_plans_id_seq', (SELECT MAX(id) FROM dispatch_plans));

-- --------------------------------------------------------
-- 已完成方案的路线（plan_id=1）
-- --------------------------------------------------------
INSERT INTO routes (id, plan_id, vehicle_id, driver_id, status,
    estimated_distance, estimated_duration, actual_distance, started_at, completed_at)
VALUES
(1, 1, 1, 3, 'completed', 148.20, 210, 151.50, NOW() - INTERVAL '3 days 3 hours 30 minutes', NOW() - INTERVAL '3 days 30 minutes'),
(2, 1, 2, 4, 'completed', 137.10, 195, 133.80, NOW() - INTERVAL '3 days 3 hours 20 minutes', NOW() - INTERVAL '3 days 20 minutes');

-- 已完成方案的路线（plan_id=2）
INSERT INTO routes (id, plan_id, vehicle_id, driver_id, status,
    estimated_distance, estimated_duration, actual_distance, started_at, completed_at)
VALUES
(3, 2, 1, 3, 'completed', 118.40, 170, 120.20, NOW() - INTERVAL '2 days 2 hours 30 minutes', NOW() - INTERVAL '2 days 20 minutes'),
(4, 2, 2, 4, 'completed', 113.20, 165, 111.40, NOW() - INTERVAL '2 days 2 hours 20 minutes', NOW() - INTERVAL '2 days 15 minutes');

-- 执行中方案的路线（plan_id=3，对应 vehicle 6 和 7，状态 on_route）
INSERT INTO routes (id, plan_id, vehicle_id, driver_id, status,
    estimated_distance, estimated_duration, started_at)
VALUES
(5, 3, 6, 3, 'in_progress', 128.50, 185, NOW() - INTERVAL '4 hours'),
(6, 3, 7, 4, 'in_progress', 113.25, 162, NOW() - INTERVAL '3 hours 30 minutes');

SELECT setval('routes_id_seq', (SELECT MAX(id) FROM routes));

-- --------------------------------------------------------
-- 路线途经点（plan_id=3 执行中路线，冗余存储地址坐标）
-- 路线5：订单 16,17,18,19 的配送点
-- --------------------------------------------------------
INSERT INTO route_stops (route_id, order_id, stop_seq, stop_type, address, lng, lat)
VALUES
-- 路线5 先统一取货
(5, 16, 1, 'pickup',   '广东工业大学大学城校区仓库', 113.3960, 23.0452),
(5, 17, 2, 'pickup',   '广东工业大学大学城校区仓库', 113.3960, 23.0452),
(5, 18, 3, 'pickup',   '广东工业大学大学城校区仓库', 113.3960, 23.0452),
(5, 19, 4, 'pickup',   '广东工业大学大学城校区仓库', 113.3960, 23.0452),
-- 路线5 按优化顺序送货
(5, 18, 5, 'delivery', '广州市越秀区黄花路58号',                         113.2750, 23.1350),
(5, 19, 6, 'delivery', '广州市白云区均禾街松洲东路158号',                  113.2500, 23.1950),
(5, 17, 7, 'delivery', '广州市海珠区龙凤街南华西路32号',                   113.2700, 23.0900),
(5, 16, 8, 'delivery', '广州市天河区高塘石路9号高德置地广场',              113.3400, 23.1400),
-- 路线6 先统一取货
(6, 20, 1, 'pickup',   '广东工业大学大学城校区仓库', 113.3960, 23.0452),
(6, 21, 2, 'pickup',   '广东工业大学大学城校区仓库', 113.3960, 23.0452),
(6, 22, 3, 'pickup',   '广东工业大学大学城校区仓库', 113.3960, 23.0452),
(6, 23, 4, 'pickup',   '广东工业大学大学城校区仓库', 113.3960, 23.0452),
-- 路线6 按优化顺序送货
(6, 23, 5, 'delivery', '广州市荔湾区逢源路289号',                          113.2300, 23.1200),
(6, 22, 6, 'delivery', '广州市天河区天河北路363号港丽商业广场',            113.3350, 23.1450),
(6, 20, 7, 'delivery', '广州市黄埔区南岗镇广州大道南3278号',               113.4200, 23.1100),
(6, 21, 8, 'delivery', '广州市番禺区大石街迎宾路128号',                    113.3500, 22.9600);
