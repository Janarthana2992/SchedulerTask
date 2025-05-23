package com.sp.schedulerplatform.jobs;

import com.sp.schedulerplatform.execution.JobExecutor;

public class Job4 implements JobExecutor {
    @Override
    public void runJob() throws InterruptedException {
        System.out.println("job4 is running...");
        Thread.sleep(7000);
        System.out.println(Thread.currentThread().getName());
        System.out.println("job4 is finished...");
    }
}
