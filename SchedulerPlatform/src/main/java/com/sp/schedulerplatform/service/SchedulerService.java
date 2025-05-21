//package com.sp.schedulerplatform.service;
//
//import com.sp.schedulerplatform.utils.DbPool;
//
//import java.sql.*;
//import java.time.Instant;
//import java.util.Map;
//import java.util.concurrent.*;
//import java.util.logging.Level;
//import java.util.logging.Logger;

//public class SchedulerService {

//    private static final Logger LOGGER = Logger.getLogger(SchedulerService.class.getName());
//    private static final int POLL_INTERVAL_SEC = 10;
//    private static final SchedulerService INSTANCE = new SchedulerService();
//
//    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
//    private final ExecutorService executor = Executors.newFixedThreadPool(10);
//    private final Map<Integer, Future<?>> runningJobs = new ConcurrentHashMap<>();
//
//    private SchedulerService() {}
//
//    public static SchedulerService getInstance() {
//        return INSTANCE;
//    }
//
//    public void start() {
//        LOGGER.info("Scheduler started.");
//        scheduler.scheduleAtFixedRate(this::pollAndDispatch, 0, POLL_INTERVAL_SEC, TimeUnit.SECONDS);
//    }
//
//    private void pollAndDispatch() {
//        try (Connection conn = DbPool.getConnection()) {
//            String sql = "SELECT * FROM jobs WHERE scheduled_time <= now() ORDER BY scheduled_time ASC";
//            try (PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
//                while (rs.next()) {
//                    int jobId = rs.getInt("id");
//                    String name = rs.getString("name");
//                    Timestamp scheduledTime = rs.getTimestamp("scheduled_time");
//                    String executionMode = rs.getString("execution_mode");
//                    int retryMax = rs.getInt("retry_max");
//                    long retryDelay = rs.getLong("retry_delay_ms");
//                    String concurrencyPolicy = rs.getString("concurrency_policy");
//
//                    if (!shouldExecute(executionMode, scheduledTime)) continue;
//
//                    if (runningJobs.containsKey(jobId)) {
//                        switch (concurrencyPolicy) {
//                            case "FORBID":
//                                continue;
//                            case "REPLACE":
//                                Future<?> existing = runningJobs.get(jobId);
//                                if (existing != null) existing.cancel(true);
//                                break;
//                            case "ALLOW":
//                            default:
//                                // No restriction
//                        }
//                    }
//
//                    Runnable jobTask = () -> executeJob(jobId, name, scheduledTime, executionMode, retryMax, retryDelay);
//                    Future<?> future = executor.submit(jobTask);
//                    runningJobs.put(jobId, future);
//                }
//            }
//        } catch (Exception e) {
//            LOGGER.log(Level.SEVERE, "Error during job polling", e);
//        }
//    }
//
//    private boolean shouldExecute(String mode, Timestamp scheduledTime) {
//        Instant now = Instant.now();
//        return scheduledTime.toInstant().isBefore(now) || scheduledTime.toInstant().equals(now);
//    }
//
//    private void executeJob(int jobId, String className, Timestamp scheduledTime, String executionMode, int retryMax, long retryDelayMs) {
//        int attempt = 0;
//        boolean success = false;
//        String error = null;
//        Timestamp startedAt = Timestamp.from(Instant.now());
//        Timestamp endedAt;
//
//        insertExecutionLog(jobId, "running", startedAt, null, null);
//
//        try {
//            while (attempt <= retryMax && !success) {
//                try {
//                    Class<?> jobClass = Class.forName("com.sp.schedulerplatform.jobs." + className);
//                    Runnable job = (Runnable) jobClass.getDeclaredConstructor().newInstance();
//                    job.run();
//                    success = true;
//                } catch (Exception e) {
//                    error = e.getMessage();
//                    LOGGER.log(Level.WARNING, "Job execution failed on attempt " + (attempt + 1) + " for jobId=" + jobId, e);
//                    attempt++;
//                    if (attempt <= retryMax) Thread.sleep(retryDelayMs);
//                }
////            }
////        } catch (InterruptedException ie) {
////            Thread.currentThread().interrupt();
////            error = "Interrupted during execution";
////            LOGGER.log(Level.SEVERE, "Job execution interrupted for jobId=" + jobId, ie);
////        } catch (Exception ex) {
////            error = "Fatal: " + ex.getMessage();
////            LOGGER.log(Level.SEVERE, "Fatal error executing jobId=" + jobId, ex);
////        } finally {
////            endedAt = Timestamp.from(Instant.now());
////            String status = success ? "success" : "failed";
////            insertExecutionLog(jobId, status, startedAt, endedAt, error);
////            updateNextSchedule(jobId, scheduledTime, executionMode);
////            runningJobs.remove(jobId);
////        }
////    }
////
////    private void insertExecutionLog(int jobId, String status, Timestamp start, Timestamp end, String error) {
////        String sql = "INSERT INTO job_executions(job_id, status, started_at, ended_at, error_message) VALUES (?, ?, ?, ?, ?)";
////        try (Connection conn = DbPool.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
////            stmt.setInt(1, jobId);
////            stmt.setString(2, status);
////            stmt.setTimestamp(3, start);
////            if (end != null) stmt.setTimestamp(4, end); else stmt.setNull(4, Types.TIMESTAMP);
////            if (error != null) stmt.setString(5, error); else stmt.setNull(5, Types.VARCHAR);
////            stmt.executeUpdate();
////        } catch (SQLException e) {
////            LOGGER.log(Level.SEVERE, "Failed to insert execution log for jobId=" + jobId, e);
////        }
////    }
////
//    private void updateNextSchedule(int jobId, Timestamp prevSchedule, String mode) {
//        Timestamp nextSchedule = null;
//
//        switch (mode) {
//            case "XMIN":
//                nextSchedule = Timestamp.from(prevSchedule.toInstant().plusSeconds(60));
//                break;
//            case "HOURLY":
//                nextSchedule = Timestamp.from(prevSchedule.toInstant().plusSeconds(3600));
//                break;
//            case "DAILY":
//                nextSchedule = Timestamp.from(prevSchedule.toInstant().plusSeconds(86400));
//                break;
//            case "WEEKLY":
//                nextSchedule = Timestamp.from(prevSchedule.toInstant().plusSeconds(604800));
//                break;
//            case "MONTHLY":
//                nextSchedule = Timestamp.from(prevSchedule.toInstant().plusSeconds(2592000)); // Approx 30 days
//                break;
//            case "ONCE":
//            default:
//                return; // No update for one-time jobs
//        }
//
//        String sql = "UPDATE jobs SET scheduled_time = ? WHERE id = ?";
//        try (Connection conn = DbPool.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
//            stmt.setTimestamp(1, nextSchedule);
//            stmt.setInt(2, jobId);
//            stmt.executeUpdate();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
//}
