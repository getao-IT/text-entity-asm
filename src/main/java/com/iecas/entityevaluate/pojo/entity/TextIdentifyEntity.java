package com.iecas.entityevaluate.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



/**
 *  @author: getao
 *  @Date: 2025/6/11 10:52
 *  @Description: 问题实体识别结果实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TextIdentifyEntity {

    private int start;

    private int end;

    private String type;

    private String word;
}
