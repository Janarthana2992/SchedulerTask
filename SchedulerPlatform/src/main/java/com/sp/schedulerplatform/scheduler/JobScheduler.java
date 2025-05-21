package com.sp.schedulerplatform.scheduler;

import com.sp.schedulerplatform.execution.JobExecutor;
import com.sp.schedulerplatform.utils.DbPool;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;

public class JobScheduler implements Runnable {

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            5, 20, 30L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()
    );

    private final Map<Integer, Future<?>> runningJobs = new ConcurrentHashMap<>();
    private volatile boolean running = true;

    public JobScheduler() {
        executor.allowCoreThreadTimeOut(true);
    }

    @Override
    public void run() {
        while (running) {
            try (Connection conn = DbPool.getConnection()) {
                String sql = "Select * from jobs where scheduled_time <= now() order by scheduled_time ASC";
                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {

                    while (rs.next()) {
                        int jobId = rs.getInt("id");
                        String name = rs.getString("name");
                        Timestamp scheduledTime = rs.getTimestamp("scheduled_time");
                        String execModeRaw = rs.getString("execution_mode");
                        String execMode = execModeRaw.trim().toLowerCase();
                        int maxRetries = rs.getInt("retry_max");
                        long retryDelay = rs.getLong("retry_delay_ms");
                        String concurrencyPolicy = rs.getString("concurrency_policy");
                        String jobStatus = rs.getString("job_status");

                        if ("once".equalsIgnoreCase(execMode) && "completed".equalsIgnoreCase(jobStatus)) {
                            continue;
                        }

                        // update job_status to in_progress for recurring jobs if scheduledTime passed and not running
                        if (!"once".equalsIgnoreCase(execMode) &&
                                scheduledTime.toLocalDateTime().isBefore(LocalDateTime.now()) &&
                                !"in_progress".equalsIgnoreCase(jobStatus)) {
                            updateJobStatus(jobId, "in_progress");
                        }


                        if (shouldRun(jobId, concurrencyPolicy)) {

                            if ("replace".equalsIgnoreCase(concurrencyPolicy) && runningJobs.containsKey(jobId)) {
                                Future<?> oldFuture = runningJobs.get(jobId);
                                if (!oldFuture.isDone()) {
                                    oldFuture.cancel(true);
                                }
                            }

                            if (isRecurring(execMode)) {
                                Timestamp nextScheduledTime = calculateNextScheduledTime(execMode, scheduledTime, execModeRaw);
                                updateNextScheduledTime(jobId, nextScheduledTime);
                            }

                            Future<?> future = executor.submit(createTask(jobId, name, execMode, execModeRaw,
                                    maxRetries, retryDelay, concurrencyPolicy));
                            runningJobs.put(jobId, future);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(50000);
            } catch (InterruptedException e) {
                running = false;
            }
        }

        shutdownExecutor();
    }

    public void shutdown() {
        running = false;
    }

    private void shutdownExecutor() {
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                System.err.println("Executor did not terminate in time");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean shouldRun(int jobId, String policy) {
        switch (policy.toLowerCase()) {
            case "allow":
                return true;
            case "forbid":
                return !runningJobs.containsKey(jobId) || runningJobs.get(jobId).isDone();
            case "replace":
                return true;
            default:
                return false;
        }
    }

    private boolean isRecurring(String execMode) {
        return !execMode.equalsIgnoreCase("once");
    }

    private Runnable createTask(int jobId, String jobClass, String execMode, String execModeRaw,
                                int maxRetries, long retryDelay, String concurrencyPolicy) {
        return () -> {
            LocalDateTime startTime = LocalDateTime.now();
            String status = "failed";
            String errorMessage = null;

            // insert initial execution log and get executionId
            long executionId = insertExecutionLogStart(jobId);
            if (executionId == -1) {
                System.err.println("Failed to create execution log for job " + jobId);
                runningJobs.remove(jobId);
                return;
            }

            // update job_status to running
            updateJobStatus(jobId, "running");

            int retries = 0;
            try {
                while (retries <= maxRetries && !Thread.currentThread().isInterrupted()) {
                    try {
                        Class<?> clazz = Class.forName("com.sp.schedulerplatform.jobs." + jobClass);
                        JobExecutor jobObj = (JobExecutor) clazz.getDeclaredConstructor().newInstance();
                        jobObj.runJob();
                        status = "success";

                        break;
                    } catch (Exception e) {
                        retries++;
                        errorMessage = e.getMessage();
                        if (retries <= maxRetries) {
                            Thread.sleep(retryDelay);
                        }
                    }
                }

            } catch (InterruptedException ie) {
                status = "terminated";
                errorMessage = "job interrupted --replace policy";
                Thread.currentThread().interrupt();
            }
            LocalDateTime endTime = LocalDateTime.now();



            // update execution log
            updateExecutionStatus(executionId, status, startTime, endTime, errorMessage);

            // update jobs.job_status
            if ("once".equalsIgnoreCase(execMode) && "success".equalsIgnoreCase(status)) {
                updateJobStatus(jobId, "completed");
            } else if (!"once".equalsIgnoreCase(execMode)) {
                updateJobStatus(jobId, "in_progress");
            }

            runningJobs.remove(jobId);
        };
    }

    private Timestamp calculateNextScheduledTime(String mode, Timestamp current, String rawMode) {
        LocalDateTime next = current.toLocalDateTime();
        switch (mode.toLowerCase()) {
            case "daily":
                next = next.plusDays(1);
                break;
            case "weekly":
                next = next.plusWeeks(1);
                break;
            case "monthly":
                next = next.plusMonths(1);
                break;
            default:
                if (mode.endsWith("min")) {
                    try {
                        int minutes = Integer.parseInt(rawMode.toLowerCase().replace("min", "").trim());
                        next = next.plusMinutes(minutes);
                    } catch (NumberFormatException e) {
                        System.err.println("invalid min value: " + rawMode);
                    }
                }
        }
        System.out.println("timestamp "+ Timestamp.valueOf(next));
        return Timestamp.valueOf(next);
    }

    private void updateNextScheduledTime(int jobId, Timestamp newTime) {
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement("update jobs set scheduled_time = ? where id = ?")) {
            stmt.setTimestamp(1, newTime);
            stmt.setInt(2, jobId);
            stmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Failed to update scheduled_time for job: " + jobId);
            e.printStackTrace();
        }
    }

    private void updateJobStatus(int jobId, String status) {
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement("update jobs set job_status = ? where id = ?")) {
            stmt.setString(1, status);
            stmt.setInt(2, jobId);
            stmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Failed to update job_status for job: " + jobId);
            e.printStackTrace();
        }
    }

    //insert a new job_executions

    private long insertExecutionLogStart(int jobId) {
        String sql = "insert into  job_executions (job_id, status, started_at) values (?, ?, ?) RETURNING id";
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, jobId);
            stmt.setString(2, "running");
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong("id");
            }
        } catch (Exception e) {
            System.err.println("Failed to insert start log for job " + jobId);
            e.printStackTrace();
        }
        return -1;
    }

    // Update the job_executions
    private void updateExecutionStatus(long executionId, String status,
                                       LocalDateTime start, LocalDateTime end, String errorMessage) {
        String sql = "update job_executions set status = ?, started_at = ?, ended_at = ?, error_message = ?, job_duration = ? Where id = ?";
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setTimestamp(2, Timestamp.valueOf(start));
            stmt.setTimestamp(3, end != null ? Timestamp.valueOf(end) : null);
            if (errorMessage != null) {
                stmt.setString(4, errorMessage);
            } else {
                stmt.setNull(4, Types.VARCHAR);
            }
            Duration duration = Duration.between(start, end);
            stmt.setLong(5, duration.toMillis());
            stmt.setLong(6, executionId);
            stmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("failed to update execution log for executionId: " + executionId);
            e.printStackTrace();
        }
    }
}
