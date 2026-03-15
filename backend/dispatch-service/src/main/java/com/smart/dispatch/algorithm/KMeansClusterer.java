package com.smart.dispatch.algorithm;

import java.util.*;

/**
 * K-Means 地理聚类器
 * 将配送点按坐标聚类为 K 组，每组分配一辆车
 */
public class KMeansClusterer {

    private static final int MAX_ITER = 100;
    private static final Random RANDOM = new Random(42);

    /**
     * @param points points[i] = {lat, lng}
     * @param k      聚类数
     * @return Map<clusterIdx, List<pointIdx>>
     */
    public static Map<Integer, List<Integer>> cluster(List<double[]> points, int k) {
        int n = points.size();
        if (n <= k) {
            // 每个点单独一组
            Map<Integer, List<Integer>> result = new HashMap<>();
            for (int i = 0; i < n; i++) {
                result.put(i, List.of(i));
            }
            return result;
        }

        // K-means++ 初始化
        double[][] centroids = initCentroidsKMeansPP(points, k);
        int[] assignments = new int[n];

        for (int iter = 0; iter < MAX_ITER; iter++) {
            boolean changed = false;
            // 分配步骤
            for (int i = 0; i < n; i++) {
                int nearest = nearestCentroid(points.get(i), centroids, k);
                if (nearest != assignments[i]) {
                    assignments[i] = nearest;
                    changed = true;
                }
            }
            if (!changed) break;

            // 更新步骤
            double[][] sum = new double[k][2];
            int[] cnt = new int[k];
            for (int i = 0; i < n; i++) {
                sum[assignments[i]][0] += points.get(i)[0];
                sum[assignments[i]][1] += points.get(i)[1];
                cnt[assignments[i]]++;
            }
            for (int j = 0; j < k; j++) {
                if (cnt[j] > 0) {
                    centroids[j][0] = sum[j][0] / cnt[j];
                    centroids[j][1] = sum[j][1] / cnt[j];
                }
            }
        }

        // 归组
        Map<Integer, List<Integer>> result = new HashMap<>();
        for (int j = 0; j < k; j++) result.put(j, new ArrayList<>());
        for (int i = 0; i < n; i++) result.get(assignments[i]).add(i);
        // 移除空簇
        result.entrySet().removeIf(e -> e.getValue().isEmpty());
        return result;
    }

    private static double[][] initCentroidsKMeansPP(List<double[]> points, int k) {
        int n = points.size();
        double[][] centroids = new double[k][2];
        // 随机选第一个质心
        int first = RANDOM.nextInt(n);
        centroids[0] = points.get(first).clone();

        for (int c = 1; c < k; c++) {
            // 按距最近已有质心的距离平方为权重选下一个质心
            double[] dist2 = new double[n];
            double sum = 0;
            for (int i = 0; i < n; i++) {
                double minD = Double.MAX_VALUE;
                for (int j = 0; j < c; j++) {
                    double d = GeoUtils.distance(points.get(i)[0], points.get(i)[1],
                            centroids[j][0], centroids[j][1]);
                    minD = Math.min(minD, d);
                }
                dist2[i] = minD * minD;
                sum += dist2[i];
            }
            double r = RANDOM.nextDouble() * sum;
            double cumSum = 0;
            int chosen = n - 1;
            for (int i = 0; i < n; i++) {
                cumSum += dist2[i];
                if (cumSum >= r) {
                    chosen = i;
                    break;
                }
            }
            centroids[c] = points.get(chosen).clone();
        }
        return centroids;
    }

    private static int nearestCentroid(double[] point, double[][] centroids, int k) {
        int best = 0;
        double bestDist = Double.MAX_VALUE;
        for (int j = 0; j < k; j++) {
            double d = GeoUtils.distance(point[0], point[1], centroids[j][0], centroids[j][1]);
            if (d < bestDist) {
                bestDist = d;
                best = j;
            }
        }
        return best;
    }
}
