package com.sp.schedulerplatform.controller;


import com.sp.schedulerplatform.scheduler.JobScheduler;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/startScheduler")
public class ScheduleStartServlet extends HttpServlet {

    private Thread schedulerThread;
    private static JobScheduler scheduler;
    public void init(){
        scheduler=new JobScheduler();
        scheduler.run();
    }

    public void doGet (HttpServletRequest request, HttpServletResponse response ) throws IOException {
        if (schedulerThread==null || !schedulerThread.isAlive()) {
            scheduler = new JobScheduler();
            schedulerThread = new Thread(scheduler, "jobSchedulerThread");
            schedulerThread.start();
            response.getWriter().write("scheduler started");
        }
        else{
            response.getWriter().write("schedule already running");

        }

    }

    public void destroy(){
        if (scheduler!=null){
            scheduler.shutdown();
        }
        if (schedulerThread != null){
            schedulerThread.interrupt();
        }
    }

}
