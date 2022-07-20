package com.xxl.job.admin.core.thread;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobRegistry;
import com.xxl.job.core.biz.model.RegistryGroupParam;
import com.xxl.job.core.biz.model.RegistryJobParam;
import com.xxl.job.core.biz.model.RegistryParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.RegistryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * job registry instance
 *
 * @author xuxueli 2016-10-02 19:10:24
 */
public class JobRegistryHelper {
    private static Logger logger = LoggerFactory.getLogger(JobRegistryHelper.class);

    private static JobRegistryHelper instance = new JobRegistryHelper();

    public static JobRegistryHelper getInstance() {
        return instance;
    }

    private ThreadPoolExecutor registryOrRemoveThreadPool = null;
    private Thread registryMonitorThread;
    private volatile boolean toStop = false;

    public void start() {

        // for registry or remove
        registryOrRemoveThreadPool = new ThreadPoolExecutor(
                2,
                10,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(2000),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "xxl-job, admin JobRegistryMonitorHelper-registryOrRemoveThreadPool-" + r.hashCode());
                    }
                },
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        r.run();
                        logger.warn(">>>>>>>>>>> xxl-job, registry or remove too fast, match threadpool rejected handler(run now).");
                    }
                });

        // for monitor
        registryMonitorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!toStop) {
                    try {
                        // auto registry group
                        List<XxlJobGroup> groupList = XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().findByAddressType(0);
                        if (groupList != null && !groupList.isEmpty()) {

                            // remove dead address (admin/executor)
                            List<Integer> ids = XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao().findDead(RegistryConfig.DEAD_TIMEOUT, new Date());
                            if (ids != null && ids.size() > 0) {
                                XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao().removeDead(ids);
                            }

                            // fresh online address (admin/executor)
                            HashMap<String, List<String>> appAddressMap = new HashMap<String, List<String>>();
                            List<XxlJobRegistry> list = XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao().findAll(RegistryConfig.DEAD_TIMEOUT, new Date());
                            if (list != null) {
                                for (XxlJobRegistry item : list) {
                                    if (RegistryConfig.RegistType.EXECUTOR.name().equals(item.getRegistryGroup())) {
                                        String appname = item.getRegistryKey();
                                        List<String> registryList = appAddressMap.get(appname);
                                        if (registryList == null) {
                                            registryList = new ArrayList<String>();
                                        }

                                        if (!registryList.contains(item.getRegistryValue())) {
                                            registryList.add(item.getRegistryValue());
                                        }
                                        appAddressMap.put(appname, registryList);
                                    }
                                }
                            }

                            // fresh group address
                            for (XxlJobGroup group : groupList) {
                                List<String> registryList = appAddressMap.get(group.getAppname());
                                String addressListStr = null;
                                if (registryList != null && !registryList.isEmpty()) {
                                    Collections.sort(registryList);
                                    StringBuilder addressListSB = new StringBuilder();
                                    for (String item : registryList) {
                                        addressListSB.append(item).append(",");
                                    }
                                    addressListStr = addressListSB.toString();
                                    addressListStr = addressListStr.substring(0, addressListStr.length() - 1);
                                }
                                group.setAddressList(addressListStr);
                                group.setUpdateTime(new Date());

                                XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().update(group);
                            }
                        }
                    } catch (Exception e) {
                        if (!toStop) {
                            logger.error(">>>>>>>>>>> xxl-job, job registry monitor thread error:{}", e);
                        }
                    }
                    try {
                        TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
                    } catch (InterruptedException e) {
                        if (!toStop) {
                            logger.error(">>>>>>>>>>> xxl-job, job registry monitor thread error:{}", e);
                        }
                    }
                }
                logger.info(">>>>>>>>>>> xxl-job, job registry monitor thread stop");
            }
        });
        registryMonitorThread.setDaemon(true);
        registryMonitorThread.setName("xxl-job, admin JobRegistryMonitorHelper-registryMonitorThread");
        registryMonitorThread.start();
    }

    public void toStop() {
        toStop = true;

        // stop registryOrRemoveThreadPool
        registryOrRemoveThreadPool.shutdownNow();

        // stop monitir (interrupt and wait)
        registryMonitorThread.interrupt();
        try {
            registryMonitorThread.join();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }


    // ---------------------- helper ----------------------

    public ReturnT<String> registry(RegistryParam registryParam) {

        // valid
        if (!StringUtils.hasText(registryParam.getRegistryGroup())
                || !StringUtils.hasText(registryParam.getRegistryKey())
                || !StringUtils.hasText(registryParam.getRegistryValue())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "Illegal Argument.");
        }

        // async execute
        registryOrRemoveThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                int ret = XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao().registryUpdate(registryParam.getRegistryGroup(), registryParam.getRegistryKey(), registryParam.getRegistryValue(), new Date());
                if (ret < 1) {
                    XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao().registrySave(registryParam.getRegistryGroup(), registryParam.getRegistryKey(), registryParam.getRegistryValue(), new Date());

                    // fresh
                    freshGroupRegistryInfo(registryParam);
                }
            }
        });

        return ReturnT.SUCCESS;
    }

    public ReturnT<String> registryRemove(RegistryParam registryParam) {

        // valid
        if (!StringUtils.hasText(registryParam.getRegistryGroup())
                || !StringUtils.hasText(registryParam.getRegistryKey())
                || !StringUtils.hasText(registryParam.getRegistryValue())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "Illegal Argument.");
        }

        // async execute
        registryOrRemoveThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                int ret = XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao().registryDelete(registryParam.getRegistryGroup(), registryParam.getRegistryKey(), registryParam.getRegistryValue());
                if (ret > 0) {
                    // fresh
                    freshGroupRegistryInfo(registryParam);
                }
            }
        });

        return ReturnT.SUCCESS;
    }

    private void freshGroupRegistryInfo(RegistryParam registryParam) {
        // Under consideration, prevent affecting core tables
    }

    /**
     * wqe
     * @param registryParam
     * @return
     */
    public ReturnT<String> registryGroup(RegistryGroupParam registryParam) {

        if (!StringUtils.hasText(registryParam.getAddressType())
                || !StringUtils.hasText(registryParam.getAppname())
                || !StringUtils.hasText(registryParam.getTitle())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "Illegal Argument.");
        }

        XxlJobGroup group = XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().findByAppname(registryParam.getAppname());
        if (group == null) {

            XxlJobGroup xxlJobGroup = new XxlJobGroup();
            xxlJobGroup.setAppname(registryParam.getAppname());
            xxlJobGroup.setTitle(registryParam.getTitle());
            xxlJobGroup.setAddressType(Integer.parseInt(registryParam.getAddressType()));
            xxlJobGroup.setUpdateTime(new Date());
            int save = XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().save(xxlJobGroup);
            if (!(save > 0)) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, "自动注册执行器失败！");
            }
        }

        return ReturnT.SUCCESS;
    }

    public ReturnT<String> registryJob(List<RegistryJobParam> registryParam) {

        if (CollectionUtils.isEmpty(registryParam)){
			return ReturnT.SUCCESS;
		}
		//获取需要注册的任务的执行器
        XxlJobGroup group = XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().findByAppname(registryParam.get(0).getJobGroup());

        if (group == null) {
			//执行器为空，添加执行器
            XxlJobGroup xxlJobGroup = new XxlJobGroup();
            xxlJobGroup.setAppname(registryParam.get(0).getJobGroup());
            xxlJobGroup.setTitle(registryParam.get(0).getJobGroup());
            xxlJobGroup.setAddressType(0);
            int save = XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().save(xxlJobGroup);
            if (!(save > 0)) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, "自动注册执行器失败！");
            }
            xxlJobGroup.setId(save);
        }
		//获取执行器的所有任务
        Set<String> collect = XxlJobAdminConfig.getAdminConfig().getXxlJobInfoDao().getJobsByGroup(group.getId())
                .stream().map(XxlJobInfo::getExecutorHandler).collect(Collectors.toSet());

        for (RegistryJobParam param : registryParam) {
			//查看任务的名字是否在执行器存在，如果存在就不进行注册
            if (collect.contains(param.getExecutorHandler())){
				continue;
			}
			//在数据库中添加任务
            XxlJobInfo jobInfo = new XxlJobInfo();
            jobInfo.setJobGroup(group.getId());
            jobInfo.setJobDesc(param.getExecutorHandler());
            jobInfo.setAddTime(new Date());
            jobInfo.setUpdateTime(new Date());
            jobInfo.setAuthor("auto");
            jobInfo.setAlarmEmail("auto");
            jobInfo.setScheduleType("CRON");
            jobInfo.setScheduleConf(param.getScheduleConf());
            jobInfo.setMisfireStrategy("DO_NOTHING");
            jobInfo.setExecutorRouteStrategy(param.getExecutorRouteStrategy());//FIRST
            jobInfo.setExecutorHandler(param.getExecutorHandler()); //注解里的名字
            jobInfo.setExecutorParam("");//调用任务的参数
            jobInfo.setExecutorBlockStrategy(param.getExecutorBlockStrategy());
            jobInfo.setExecutorTimeout(0);//执行任务超时时间
            jobInfo.setExecutorFailRetryCount(0);//执行失败重试次数
            jobInfo.setGlueType(param.getGlueType());//运行模式使用默认bean模式
            jobInfo.setGlueSource("");
            jobInfo.setGlueRemark("自动生成");
            jobInfo.setGlueUpdatetime(new Date());
            jobInfo.setChildJobId("");//子任务ID
            jobInfo.setTriggerStatus(1);//触发状态启动

            XxlJobAdminConfig.getAdminConfig().getXxlJobInfoDao().save(jobInfo);
        }

        return ReturnT.SUCCESS;
    }


}
