package com.smart.dispatch.algorithm;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 遗传算法路线优化器（TSP 变种）
 * 解决单辆车配送路线最短问题
 */
public class GeneticOptimizer {

    private final int popSize;
    private final int maxIter;
    private final double mutationRate;
    private static final int TOURNAMENT_SIZE = 3;
    private static final Random RANDOM = new Random();

    public GeneticOptimizer(int popSize, int maxIter, double mutationRate) {
        this.popSize = popSize;
        this.maxIter = maxIter;
        this.mutationRate = mutationRate;
    }

    /**
     * 优化配送序列
     *
     * @param points   配送点坐标，points[i] = {lat, lng}
     * @param depotLat 仓库纬度
     * @param depotLng 仓库经度
     * @return 优化后的配送顺序索引数组
     */
    public int[] optimize(double[][] points, double depotLat, double depotLng) {
        int n = points.length;
        if (n <= 1) {
            int[] seq = new int[n];
            for (int i = 0; i < n; i++) seq[i] = i;
            return seq;
        }
        if (n == 2) return new int[]{0, 1};

        // 初始种群
        List<int[]> population = initPopulation(n);
        int[] best = findBest(population, points, depotLat, depotLng);
        double bestDist = GeoUtils.routeDistance(best, points, depotLat, depotLng);

        for (int iter = 0; iter < maxIter; iter++) {
            List<int[]> newPop = new ArrayList<>();

            // 精英保留：前 10%
            int eliteCount = Math.max(1, popSize / 10);
            population.sort(Comparator.comparingDouble(c ->
                    GeoUtils.routeDistance(c, points, depotLat, depotLng)));
            newPop.addAll(population.subList(0, eliteCount).stream()
                    .map(int[]::clone).collect(Collectors.toList()));

            // 交叉 + 变异填充剩余
            while (newPop.size() < popSize) {
                int[] p1 = tournamentSelect(population, points, depotLat, depotLng);
                int[] p2 = tournamentSelect(population, points, depotLat, depotLng);
                int[] child = oxCrossover(p1, p2);
                if (RANDOM.nextDouble() < mutationRate) swapMutation(child);
                newPop.add(child);
            }
            population = newPop;

            // 更新全局最优
            int[] iterBest = findBest(population, points, depotLat, depotLng);
            double iterDist = GeoUtils.routeDistance(iterBest, points, depotLat, depotLng);
            if (iterDist < bestDist) {
                best = iterBest.clone();
                bestDist = iterDist;
            }
        }
        return best;
    }

    private List<int[]> initPopulation(int n) {
        List<int[]> pop = new ArrayList<>(popSize);
        int[] base = IntStream.range(0, n).toArray();
        // 第一个为贪心最近邻解
        pop.add(base.clone());
        for (int i = 1; i < popSize; i++) {
            int[] perm = base.clone();
            shuffle(perm);
            pop.add(perm);
        }
        return pop;
    }

    private void shuffle(int[] arr) {
        for (int i = arr.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            int tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
    }

    private int[] findBest(List<int[]> pop, double[][] points, double depotLat, double depotLng) {
        return pop.stream().min(Comparator.comparingDouble(c ->
                GeoUtils.routeDistance(c, points, depotLat, depotLng))).orElseThrow();
    }

    private int[] tournamentSelect(List<int[]> pop, double[][] points,
                                   double depotLat, double depotLng) {
        int[] best = null;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            int[] c = pop.get(RANDOM.nextInt(pop.size()));
            double d = GeoUtils.routeDistance(c, points, depotLat, depotLng);
            if (d < bestDist) {
                bestDist = d;
                best = c;
            }
        }
        return best;
    }

    /**
     * 顺序交叉算子（OX - Order Crossover）
     */
    private int[] oxCrossover(int[] p1, int[] p2) {
        int n = p1.length;
        int a = RANDOM.nextInt(n);
        int b = RANDOM.nextInt(n);
        if (a > b) {
            int tmp = a;
            a = b;
            b = tmp;
        }
        int[] child = new int[n];
        Arrays.fill(child, -1);
        // 复制 p1[a..b] 段到 child
        for (int i = a; i <= b; i++) child[i] = p1[i];
        // 按 p2 顺序填充剩余位置
        int pos = (b + 1) % n;
        for (int i = 0; i < n; i++) {
            int val = p2[(b + 1 + i) % n];
            boolean exists = false;
            for (int k = a; k <= b; k++) {
                if (child[k] == val) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                child[pos] = val;
                pos = (pos + 1) % n;
            }
        }
        return child;
    }

    /** 随机交换两个位置 */
    private void swapMutation(int[] chromosome) {
        int i = RANDOM.nextInt(chromosome.length);
        int j = RANDOM.nextInt(chromosome.length);
        int tmp = chromosome[i];
        chromosome[i] = chromosome[j];
        chromosome[j] = tmp;
    }
}
