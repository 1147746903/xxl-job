package com.xxl.job.core.biz.model;

import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;

import java.io.Serializable;

/**
 * wqe注册任务的对象
 * Created by xuxueli on 2017-05-10 20:22:42
 */
public class RegistryJobParam implements Serializable {
    private static final long serialVersionUID = 42L;

    private String jobGroup;
    private String jobName;
    private String executorHandler;
    private String scheduleConf;
    private String executorRouteStrategy = "FIRST";
    private String executorBlockStrategy = ExecutorBlockStrategyEnum.SERIAL_EXECUTION.name();
    private String glueType = "BEAN";

    public RegistryJobParam(){}

    public RegistryJobParam(String jobGroup, String jobName, String executorHandler,String scheduleConf) {
        this.jobGroup = jobGroup;
        this.jobName = jobName;
        this.executorHandler = executorHandler;
        this.scheduleConf=scheduleConf;
    }

    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getExecutorHandler() {
        return executorHandler;
    }

    public void setExecutorHandler(String executorHandler) {
        this.executorHandler = executorHandler;
    }

    public String getExecutorRouteStrategy() {
        return executorRouteStrategy;
    }

    public void setExecutorRouteStrategy(String executorRouteStrategy) {
        this.executorRouteStrategy = executorRouteStrategy;
    }

    public String getExecutorBlockStrategy() {
        return executorBlockStrategy;
    }

    public void setExecutorBlockStrategy(String executorBlockStrategy) {
        this.executorBlockStrategy = executorBlockStrategy;
    }

    public String getGlueType() {
        return glueType;
    }

    public void setGlueType(String glueType) {
        this.glueType = glueType;
    }

    public String getScheduleConf() {
        return scheduleConf;
    }

    public void setScheduleConf(String scheduleConf) {
        this.scheduleConf = scheduleConf;
    }

    @Override
    public String toString() {
        return "RegistryJobParam{" +
                "jobGroup='" + jobGroup + '\'' +
                ", jobName='" + jobName + '\'' +
                ", executorHandler='" + executorHandler + '\'' +
                ", scheduleConf='" + scheduleConf + '\'' +
                ", executorRouteStrategy='" + executorRouteStrategy + '\'' +
                ", executorBlockStrategy='" + executorBlockStrategy + '\'' +
                ", glueType='" + glueType + '\'' +
                '}';
    }
}
