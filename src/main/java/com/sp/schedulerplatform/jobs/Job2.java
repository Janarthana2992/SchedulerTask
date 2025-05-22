package com.sp.schedulerplatform.jobs;

import com.sp.schedulerplatform.execution.JobExecutor;

public class Job2 implements JobExecutor {
    @Override
    public void runJob() throws InterruptedException {
        System.out.println("job2 is running...");
        Thread.sleep(1000);
        System.out.println(Thread.currentThread().getName());
        System.out.println("job2 is finished...");
    }
}
