package com.sp.schedulerplatform.listener;

import com.sp.schedulerplatform.service.SchedulerThread;
import jakarta.servlet.ServletContextAttributeListener;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class AppListner implements ServletContextListener {
    private SchedulerThread schedulerThread;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
    schedulerThread= new SchedulerThread();
    schedulerThread.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContextListener.super.contextDestroyed(sce);
        if (schedulerThread!=null){
            schedulerThread.shutdown();
        }
    }
}
