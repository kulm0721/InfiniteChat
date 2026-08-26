package com.shanyangcode.redpacketservice.util;

import java.util.*;

public class RedPacketAlgorithm {

    /**
     * 最小红包金额（单位：分）
     */
    private static final long MIN_AMOUNT = 1L;

    /**
     * 分配普通红包（均分）
     *
     * @param totalAmount 红包总金额（单位：分）
     * @param totalCount  红包总个数
     * @return 金额列表（单位：分）
     */
    public static List<Long> allocateNormalRedPacket(long totalAmount, int totalCount) {
        if (totalAmount <= 0 || totalCount <= 0) {
            throw new IllegalArgumentException("红包金额和数量必须大于0");
        }

        if (totalAmount < totalCount * MIN_AMOUNT) {
            throw new IllegalArgumentException("红包总金额不足，至少需要" + (totalCount * MIN_AMOUNT) + "分");
        }

        List<Long> amounts = new ArrayList<>(totalCount);

        // 计算平均金额
        long avgAmount = totalAmount / totalCount;
        // 计算余数
        long remainder = totalAmount % totalCount;

        // 先分配平均金额
        for (int i = 0; i < totalCount; i++) {
            amounts.add(avgAmount);
        }

        // 将余数分配给前面的红包
        for (int i = 0; i < remainder; i++) {
            amounts.set(i, amounts.get(i) + 1);
        }

        return amounts;
    }

    /**
     * 分配拼手气红包（随机金额）
     * 使用"预处理线段切割法"
     *
     * @param totalAmount 红包总金额（单位：分）
     * @param totalCount  红包总个数
     * @return 金额列表（单位：分）
     */
    public static List<Long> allocateRandomRedPacket(long totalAmount, int totalCount) {
        if (totalAmount <= 0 || totalCount <= 0) {
            throw new IllegalArgumentException("红包金额和数量必须大于0");
        }

        if (totalAmount < totalCount * MIN_AMOUNT) {
            throw new IllegalArgumentException("红包总金额不足，至少需要" + (totalCount * MIN_AMOUNT) + "分");
        }

        List<Long> amounts = new ArrayList<>(totalCount);

        // 特殊情况：只有一个红包
        if (totalCount == 1) {
            amounts.add(totalAmount);
            return amounts;
        }

        // 1. 预留每个红包的最小金额
        long reservedAmount = totalCount * MIN_AMOUNT;
        long remainAmount = totalAmount - reservedAmount;

        // 特殊情况：remainAmount < totalCount 会导致出现死循环，退化调用 allocateNormalRedPacket 均分
        if (remainAmount < totalCount) {
            return allocateNormalRedPacket(totalAmount, totalCount);
        }

        // 2. 生成 n-1 个随机切割点
        Set<Long> cutPoints = new TreeSet<>();
        cutPoints.add(0L);
        cutPoints.add(remainAmount);

        Random random = new Random();
        while (cutPoints.size() < totalCount + 1) {
            long point = (long) (random.nextDouble() * remainAmount);
            cutPoints.add(point);
        }

        // 3. 将切割点转换为列表并排序（TreeSet 已经排序）
        List<Long> sortedPoints = new ArrayList<>(cutPoints);

        // 4. 计算每段的长度，并加上最小金额
        for (int i = 0; i < totalCount; i++) {
            long segment = sortedPoints.get(i + 1) - sortedPoints.get(i);
            amounts.add(segment + MIN_AMOUNT);
        }


        // 5. 打乱顺序
        Collections.shuffle(amounts);

        return amounts;
    }
}
