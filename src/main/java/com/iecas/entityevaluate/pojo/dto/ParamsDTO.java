package com.iecas.entityevaluate.pojo.dto;

import lombok.Data;

/**
 * @Author: guo_x
 * @Date: 2025/5/29 15:49
 * @Description:
 */
@Data
public class ParamsDTO {

    /**
     * 真实文件路径
     */
    private String trueFilePath;

    /**
     * 预测文件路径
     */
    private String predFilePath;
}
