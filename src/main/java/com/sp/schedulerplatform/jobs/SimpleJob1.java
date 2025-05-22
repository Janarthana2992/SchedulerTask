package com.sp.schedulerplatform.jobs;

import com.sp.schedulerplatform.execution.JobExecutor;

public class SimpleJob1 implements JobExecutor {
    @Override
    public void runJob() throws InterruptedException {
        System.out.println("job1 is running...");
        Thread.sleep(5000);
        System.out.println(Thread.currentThread().getName());
        System.out.println("job1 is finished...");
    }
}
