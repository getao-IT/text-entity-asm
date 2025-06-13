package com.iecas.evaluate.pojo.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * @Author: guo_x
 * @Date: 2025/5/29 14:59
 * @Description:
 */
@Data
public class MetricsResult {

    /**
     * 宏平均值
     */
    @JsonProperty("Micro Avg")
    private SubMetricsResult macro;

    /**
     * 微平均
     */
    @JsonProperty("Macro Avg")
    private SubMetricsResult micro;

    /**
     * 每一个类别的结果
     */
    private List<SubMetricsResult> preClassResult;
}
