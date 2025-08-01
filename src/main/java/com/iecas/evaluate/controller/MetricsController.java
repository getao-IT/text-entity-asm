package com.iecas.evaluate.controller;

import com.iecas.evaluate.aop.annotation.Logger;
import com.iecas.evaluate.common.CommonResult;
import com.iecas.evaluate.pojo.dto.ParamsDTO;
import com.iecas.evaluate.pojo.entity.MetricsResult;
import com.iecas.evaluate.pojo.entity.SubMetricsResult;
import com.iecas.evaluate.utils.EntityMetricsUtils;
import org.springframework.web.bind.annotation.*;

/**
 * @Author: guo_x
 * @Date: 2025/5/29 15:46
 * @Description:
 */
@RestController
@RequestMapping("/metrics")
public class MetricsController {


    /**
     *  @author: getao
     *  @Date: 2025/6/11 9:35
     *  @Description: 获取文本评估多个维度评估结果
     */
    @PostMapping("/calculateTextMetrics")
    @Logger("获取文本实体的评估指标")
    public CommonResult calculateTextMetrics(@RequestBody ParamsDTO dto){
        MetricsResult result = EntityMetricsUtils.calculateMetrics(dto.getTrueFilePath(), dto.getPredFilePath());
        return new CommonResult().data(result).success();
    }


    /**
     *  @author: getao
     *  @Date: 2025/6/11 9:36
     *  @Description: 获取文本单一维度评估结果
     */
    @PostMapping("/calculateTextLightMetrics")
    @Logger("获取文本实体的评估指标, 轻量版仅将数据存入data中")
    public CommonResult calculateTextLightMetrics(@RequestBody ParamsDTO dto){
        SubMetricsResult result = EntityMetricsUtils.calculateLightMetrics(dto.getTrueFilePath(), dto.getPredFilePath());
        return new CommonResult().data(result).success();
    }


    /**
     *  @author: getao
     *  @Date: 2025/6/11 9:36
     *  @Description: 获取空天模型文本单一维度评估结果
     */
    @GetMapping("/calculateIecasTextMetrics")
    @Logger("获取空天模型文本单一维度评估结果")
    public CommonResult calculateIecasTextMetrics(String predPath, String gtPath){
        SubMetricsResult result = EntityMetricsUtils.calculateIecasTextMetrics(predPath, gtPath);
        return new CommonResult().data(result).success();
    }
}
