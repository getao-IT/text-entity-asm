package com.iecas.entityevaluate.pojo.entity;

import lombok.Data;

/**
 * @Author: guo_x
 * @Date: 2025/5/29 15:00
 * @Description:
 */
@Data
public class SubMetricsResult {

    /**
     * 精确率
     */
    private double precision;

    /**
     * 准确率
     */
    private double accuracy;

    /**
     * 召回率
     */
    private double recall;

    /**
     * f1
     */
    private double f1;

    /**
     * 实体类别名称
     */
    private String clazz;

    /**
     * 实际类别中包含的实体数量
     */
    private double TP;

    /**
     * 预测类别中包含的实体数量
     */
    private double FP;

    /**
     * 实际类别中没有的实体数量
     */
    private double FN;

    /**
     * 计算准确率
     * @param TN 预测正确的实体数量
     */
    public void calculateAccuracy(double TN){
        this.accuracy = (TP + TN) / (TN + TP + FP + FN);
    }
}
