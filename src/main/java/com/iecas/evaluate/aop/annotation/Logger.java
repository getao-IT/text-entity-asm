/**
 * @Time: 2024/8/28 15:11
 * @Author: guoxun
 * @File: Logger
 * @Description:
 */

package com.iecas.evaluate.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Logger {

    String value() default "";
}
