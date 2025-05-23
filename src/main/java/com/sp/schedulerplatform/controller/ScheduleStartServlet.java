package com.sp.schedulerplatform.controller;

import com.sp.schedulerplatform.scheduler.JobScheduler;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/startScheduler")
public class ScheduleStartServlet extends HttpServlet {

    private static volatile boolean isRunning = false;
    private static JobScheduler scheduler;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        if (isRunning) {
            response.getWriter().write("Scheduler already running");
            return;
        }

        new Thread(() -> {
            isRunning = true;
            scheduler = new JobScheduler();
            scheduler.run();
            isRunning = false;
        }).start();

        response.getWriter().write("Scheduler started");
    }

    @Override
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}