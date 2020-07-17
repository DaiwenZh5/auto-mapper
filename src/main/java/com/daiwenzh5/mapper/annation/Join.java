package com.daiwenzh5.mapper.annation;

import java.lang.annotation.*;

/**
 * @author daiwenzh5
 * @date 2020-07-18 00:19
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Join {

    /**
     * 别名
     */
    String as() default "";

    /**
     * 关联主键
     */
    Id joinId() default @Id;

    /**
     * 集合映射
     */
    boolean many() default false;

    @interface Id {

        /**
         * 注解属性名
         */
        String value() default "id";

    }
}
