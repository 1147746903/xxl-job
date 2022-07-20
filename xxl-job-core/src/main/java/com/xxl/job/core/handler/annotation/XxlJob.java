package com.xxl.job.core.handler.annotation;

import java.lang.annotation.*;

/**
 * annotation for method jobhandler
 *
 * @author xuxueli 2019-12-11 20:50:13
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface XxlJob {

    /**
     * jobhandler name
     */
    String value();

    /**
     * init handler, invoked when JobThread init
     */
    String init() default "";

    /**
     * destroy handler, invoked when JobThread destroy
     */
    String destroy() default "";

    /**
     * wqe
     * destroy handler, invoked when JobThread destroy
     */
    boolean autoRegister() default false;

    /**
     * wqe
     * CRON表达式 默认5秒执行一次
     */
    String scheduleConf() default "*/5 * * * * ?";

    /**
     * wqe
     * 任务描述
     */
    String description() default "";

}
