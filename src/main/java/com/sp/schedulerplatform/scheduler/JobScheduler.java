package com.sp.schedulerplatform.scheduler;

import com.sp.schedulerplatform.execution.JobExecutor;
import com.sp.schedulerplatform.utils.DbPool;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.*;

public class JobScheduler implements Runnable {
    private static final JobScheduler instance = new JobScheduler();



    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            5, 20, 30L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()
    );

    private static final Map<Integer, Future<?>> runningJobs = new ConcurrentHashMap<>();
    private volatile boolean running = true;
    private  volatile int retries=0;
    private  volatile int i=0;

    public JobScheduler() {
        executor.allowCoreThreadTimeOut(true);
    }

    public static JobScheduler getInstance() {
        return instance;
    }

    @Override
    public void run() {
        while (running) {
            System.out.println(i++);
            try (Connection conn = DbPool.getConnection()) {
                String sql = "SELECT * FROM jobs WHERE scheduled_time <= now() ORDER BY scheduled_time ASC";
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

                        if ("once".equalsIgnoreCase(execMode) && isJobSuccessfullyExecuted(jobId)) {
                            continue;
                        }

                        if (shouldRun(jobId, concurrencyPolicy)) {
                            if ("replace".equalsIgnoreCase(concurrencyPolicy) && runningJobs.containsKey(jobId)) {
                                Future<?> oldFuture = runningJobs.get(jobId);
                                if (!oldFuture.isDone()) {
                                    oldFuture.cancel(true);
                                }
                            }

                            Future<?> future = executor.submit(createTask(jobId, name, execMode, execModeRaw,
                                    maxRetries, retryDelay, concurrencyPolicy, scheduledTime));
                            runningJobs.put(jobId, future);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                running = false;
                Thread.currentThread().interrupt();
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
            Thread.currentThread().interrupt();
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
    public boolean triggerJob(int jobId) {
        try (Connection conn = DbPool.getConnection()) {
            String sql = "SELECT * FROM jobs WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, jobId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        System.err.println("Job with ID " + jobId + " not found");
                        return false;
                    }

                    String name = rs.getString("name");
                    Timestamp scheduledTime = new Timestamp(System.currentTimeMillis()); // manual trigger time = now
                    String execModeRaw = rs.getString("execution_mode");
                    String execMode = execModeRaw.trim().toLowerCase();
                    int maxRetries = rs.getInt("retry_max");
                    long retryDelay = rs.getLong("retry_delay_ms");
                    String concurrencyPolicy = rs.getString("concurrency_policy");

                    // Check concurrency policy
                    if (!shouldRun(jobId, concurrencyPolicy)) {
                        System.out.println("Job " + jobId + " cannot be triggered due to concurrency policy.");
                        return false;
                    }

                    if ("replace".equalsIgnoreCase(concurrencyPolicy) && runningJobs.containsKey(jobId)) {
                        Future<?> oldFuture = runningJobs.get(jobId);
                        if (!oldFuture.isDone()) {
                            oldFuture.cancel(true);
                        }
                    }


                    Future<?> future = executor.submit(createTask(jobId, name, execMode, execModeRaw,
                            maxRetries, retryDelay, concurrencyPolicy, scheduledTime));
                    runningJobs.put(jobId, future);
                    System.out.println("Job " + jobId + " triggered manually.");
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to trigger job with ID " + jobId);
            e.printStackTrace();
            return false;
        }
    }
    public static boolean cancelJobBeforeExecution(int jobId, int userId) {
        Future<?> future = runningJobs.get(jobId);
        if (future != null && !future.isDone() && !future.isCancelled()) {
            boolean cancelled = future.cancel(true);
            if (cancelled) {
                logSkippedExecution(jobId, userId);
                runningJobs.remove(jobId);
                return true;
            }
        }
        return false;
    }

    private static void logSkippedExecution(int jobId, int userId) {
        String sql = "INSERT INTO job_executions (job_id, status, started_at, ended_at, job_duration, error_message) " +
                "VALUES (?, 'skipped', now(), now(), 0, 'Job manually cancelled before execution')";
        try (var conn = DbPool.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, jobId);
            stmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Failed to log skipped job execution for job " + jobId);
            e.printStackTrace();
        }
    }


    private Runnable createTask(int jobId, String jobClass, String execMode, String execModeRaw,
                                int maxRetries, long retryDelay, String concurrencyPolicy, Timestamp scheduledTime) {
        return () -> {
            LocalDateTime startTime = LocalDateTime.now();
            boolean success = false;
            String errorMessage = null;

            int executionId = insertExecutionLogStart(jobId);
            if (executionId == -1) {
                System.err.println("Failed to create execution log for job " + jobId);
                runningJobs.remove(jobId);
                return;
            }

            if (Duration.between(scheduledTime.toLocalDateTime(), startTime).toMinutes() > 1) {
                String timeStr = scheduledTime.toLocalDateTime()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                errorMessage = "Job was delayed. Originally scheduled for: " + timeStr;
            }


            try {
                while (retries <= maxRetries && !Thread.currentThread().isInterrupted()) {
                    try {
                        Class<?> clazz = Class.forName("com.sp.schedulerplatform.jobs." + jobClass);
                        JobExecutor jobObj = (JobExecutor) clazz.getDeclaredConstructor().newInstance();

                        updateJobExecutionStatus(executionId, "running");
                        jobObj.runJob();
                        updateJobExecutionStatus(executionId, "success");
                        success = true;
                        break;
                    } catch (Exception e) {
                        System.out.println("retry delay ; "+retryDelay);

                        updateJobExecutionStatus(executionId, "failed");
                        retries++;
                        System.out.println("retry:"+retries);
                        String exMsg = e.getMessage() != null ? e.getMessage() : e.toString();
                        errorMessage = (errorMessage == null) ? exMsg : errorMessage + " | " + exMsg;

                        if (retries <= maxRetries) {
                            Thread.sleep(retryDelay);
                        }
                    }
                }
            } catch (InterruptedException ie) {
                System.out.println("interrupted");
                updateJobExecutionStatus(executionId, "failed");
                errorMessage = (errorMessage == null) ? "job interrupted" : errorMessage + " | Job interrupted";
                Thread.currentThread().interrupt();
            }

            LocalDateTime endTime = LocalDateTime.now();
            updateExecutionStatus(executionId, startTime, endTime, errorMessage);

            if (success && isRecurring(execMode)) {
                Timestamp nextScheduledTime = calculateNextScheduledTime(execMode, Timestamp.valueOf(startTime), execModeRaw);
                updateNextScheduledTime(jobId, nextScheduledTime);
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
                        System.err.println("Invalid minute value: " + rawMode);
                    }
                }
        }
        return Timestamp.valueOf(next);
    }

    private void updateNextScheduledTime(int jobId, Timestamp newTime) {
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE jobs SET scheduled_time = ? WHERE id = ?")) {
            stmt.setTimestamp(1, newTime);
            stmt.setInt(2, jobId);
            stmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Failed to update scheduled_time for job: " + jobId);
            e.printStackTrace();
        }
    }

    private void updateJobExecutionStatus(int executionId, String status) {
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE job_executions SET status = ? WHERE id = ?")) {
            stmt.setString(1, status);
            stmt.setInt(2, executionId);
            stmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Failed to update job execution status for ID: " + executionId);
            e.printStackTrace();
        }
    }

    private int insertExecutionLogStart(int jobId) {
        String sql = "INSERT INTO job_executions (job_id, status, started_at) VALUES (?, ?, ?) RETURNING id";
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, jobId);
            stmt.setString(2, "running");
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (Exception e) {
            System.err.println("Failed to insert execution log for job " + jobId);
            e.printStackTrace();
        }
        return -1;
    }

    private void updateExecutionStatus(long executionId,
                                       LocalDateTime start, LocalDateTime end, String errorMessage) {
        String sql = "UPDATE job_executions SET ended_at = ?, error_message = ?, job_duration = ? WHERE id = ?";
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(end));
            if (errorMessage != null) {
                stmt.setString(2, errorMessage);
            } else {
                stmt.setNull(2, Types.VARCHAR);
            }
            stmt.setObject(3, Duration.between(start, end).toMillis());
            stmt.setLong(4, executionId);
            stmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Failed to update execution log for executionId: " + executionId);
            e.printStackTrace();
        }
    }

    private boolean isJobSuccessfullyExecuted(int jobId) {
        String sql = "SELECT status FROM job_executions WHERE job_id = ? ORDER BY id DESC LIMIT 1";
        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, jobId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return "success".equalsIgnoreCase(rs.getString("status"));
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to check last execution status for job: " + jobId);
            e.printStackTrace();
        }
        return false;
    }
}
