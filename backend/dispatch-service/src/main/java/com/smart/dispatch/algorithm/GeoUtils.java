package com.smart.dispatch.algorithm;

/**
 * 地理距离工具类 - Haversine 公式计算球面距离
 */
public class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /** 计算两点间球面距离（单位：km） */
    public static double distance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
    }

    /**
     * 计算路线总距离：仓库 → 各配送点（按 chromosome 顺序）→ 仓库
     *
     * @param chromosome 配送顺序（各点在 points 数组中的索引）
     * @param points     配送点坐标，points[i] = {lat, lng}
     * @param depotLat   仓库纬度
     * @param depotLng   仓库经度
     */
    public static double routeDistance(int[] chromosome, double[][] points,
                                       double depotLat, double depotLng) {
        int n = chromosome.length;
        if (n == 0) return 0;
        double total = distance(depotLat, depotLng,
                points[chromosome[0]][0], points[chromosome[0]][1]);
        for (int i = 0; i < n - 1; i++) {
            total += distance(points[chromosome[i]][0], points[chromosome[i]][1],
                    points[chromosome[i + 1]][0], points[chromosome[i + 1]][1]);
        }
        total += distance(points[chromosome[n - 1]][0], points[chromosome[n - 1]][1],
                depotLat, depotLng);
        return total;
    }

    /** 顺序路线总距离（不优化，用于对比 before 数据） */
    public static double sequentialDistance(double[][] points, double depotLat, double depotLng) {
        int[] seq = new int[points.length];
        for (int i = 0; i < seq.length; i++) seq[i] = i;
        return routeDistance(seq, points, depotLat, depotLng);
    }
}
