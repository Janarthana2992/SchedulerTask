package com.sp.schedulerplatform.listener;

import com.sp.schedulerplatform.scheduler.JobScheduler;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;


public class AppListener implements ServletContextListener {
    private Thread schedulerThread;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        JobScheduler scheduler = new JobScheduler();
        schedulerThread = new Thread(scheduler);
        schedulerThread.start();
        System.out.println("Job Scheduler started.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (schedulerThread != null) {
            schedulerThread.interrupt();
        }
        System.out.println("Job Scheduler stopped.");
    }

}
