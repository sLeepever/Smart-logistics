-- ============================================================
-- tracking_db 演示数据
-- 连接 tracking_db 后执行：psql -U postgres -d tracking_db -f demo-tracking_db.sql
-- ============================================================
-- 路线 5（driver_id=3，车辆粤A67890）当前在天河区附近
-- 路线 6（driver_id=4，车辆粤A78901）当前在荔湾区附近
-- ============================================================

INSERT INTO location_records (route_id, driver_id, lng, lat, speed, heading, recorded_at)
VALUES
-- 路线5 司机3 行驶轨迹（从大学城出发，向天河区方向）
(5, 3, 113.3960, 23.0452,  0.0,  45.0, NOW() - INTERVAL '4 hours'),
(5, 3, 113.3890, 23.0612, 45.5,  30.0, NOW() - INTERVAL '3 hours 50 minutes'),
(5, 3, 113.3750, 23.0820, 52.3,  15.0, NOW() - INTERVAL '3 hours 40 minutes'),
(5, 3, 113.3500, 23.1050, 48.8,  10.0, NOW() - INTERVAL '3 hours 30 minutes'),
(5, 3, 113.3200, 23.1200, 38.2, 350.0, NOW() - INTERVAL '3 hours 20 minutes'),
-- 到达越秀区，完成送货后转向白云区
(5, 3, 113.2750, 23.1350,  0.0,   0.0, NOW() - INTERVAL '3 hours 10 minutes'),
(5, 3, 113.2680, 23.1500, 42.1, 350.0, NOW() - INTERVAL '3 hours'),
(5, 3, 113.2600, 23.1680, 50.0, 355.0, NOW() - INTERVAL '2 hours 50 minutes'),
(5, 3, 113.2520, 23.1850, 45.5, 358.0, NOW() - INTERVAL '2 hours 40 minutes'),
-- 到达白云区完成送货，向海珠区出发
(5, 3, 113.2500, 23.1950,  0.0,   0.0, NOW() - INTERVAL '2 hours 30 minutes'),
(5, 3, 113.2600, 23.1700, 40.0, 180.0, NOW() - INTERVAL '2 hours 20 minutes'),
(5, 3, 113.2700, 23.1400, 48.5, 170.0, NOW() - INTERVAL '2 hours 10 minutes'),
(5, 3, 113.2700, 23.1200, 52.0, 160.0, NOW() - INTERVAL '2 hours'),
-- 当前位置（距海珠区目标较近）
(5, 3, 113.2700, 23.0950, 38.5, 180.0, NOW() - INTERVAL '30 minutes'),
(5, 3, 113.2710, 23.0900, 15.0, 175.0, NOW() - INTERVAL '15 minutes'),

-- 路线6 司机4 行驶轨迹（从大学城出发，向荔湾区方向）
(6, 4, 113.3960, 23.0452,  0.0,  315.0, NOW() - INTERVAL '3 hours 30 minutes'),
(6, 4, 113.3800, 23.0600, 44.2,  300.0, NOW() - INTERVAL '3 hours 20 minutes'),
(6, 4, 113.3500, 23.0800, 50.8,  280.0, NOW() - INTERVAL '3 hours 10 minutes'),
(6, 4, 113.3100, 23.0900, 47.5,  265.0, NOW() - INTERVAL '3 hours'),
(6, 4, 113.2700, 23.1050, 42.0,  270.0, NOW() - INTERVAL '2 hours 50 minutes'),
-- 到达荔湾区逢源路
(6, 4, 113.2300, 23.1200,  0.0,    0.0, NOW() - INTERVAL '2 hours 40 minutes'),
(6, 4, 113.2500, 23.1280, 38.5,   90.0, NOW() - INTERVAL '2 hours 30 minutes'),
(6, 4, 113.2800, 23.1350, 45.0,   85.0, NOW() - INTERVAL '2 hours 20 minutes'),
(6, 4, 113.3100, 23.1400, 50.5,   80.0, NOW() - INTERVAL '2 hours 10 minutes'),
-- 当前位置（天河区港丽商业广场附近，进行第二站送货）
(6, 4, 113.3350, 23.1450,  5.0,   75.0, NOW() - INTERVAL '1 hour'),
(6, 4, 113.3360, 23.1448,  0.0,    0.0, NOW() - INTERVAL '30 minutes');

SELECT setval('location_records_id_seq', (SELECT COALESCE(MAX(id), 1) FROM location_records));
SELECT setval('order_chat_conversations_id_seq', (SELECT COALESCE(MAX(id), 1) FROM order_chat_conversations));
SELECT setval('order_chat_messages_id_seq', (SELECT COALESCE(MAX(id), 1) FROM order_chat_messages));
