package com.iecas.evaluate.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: guo_x
 * @Date: 2025/5/29 14:17
 * @Description: 实体信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EntityInfo {

    /**
     * 文本内容
     */
    private String entity;

    /**
     * 文本所对应的实体类别
     */
    private String entityClazz;

    /**
     * 文本内容对应的起始位置索引
     */
    private long start;

    /**
     * 文本内容对应的结束位置索引
     */
    private long end;
}
