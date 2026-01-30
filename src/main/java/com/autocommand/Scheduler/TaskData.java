package com.autocommand.Scheduler;

/**
 * Data model representing a scheduled task.
 * 代表一个计划任务的数据模型。
 */
public class TaskData {
    public String id;
    public long targetTime; // Epoch millis / 时间戳
    public String command;
    public boolean isLoop;
    public int loopIntervalSeconds;
    public String status; // "PENDING", "EXECUTED", "CANCELLED"

    /**
     * Constructor.
     * 构造函数。
     */
    public TaskData(String id, long targetTime, String command, boolean isLoop, int loopIntervalSeconds, String status) {
        this.id = id;
        this.targetTime = targetTime;
        this.command = command;
        this.isLoop = isLoop;
        this.loopIntervalSeconds = loopIntervalSeconds;
        this.status = status;
    }

    // Getters and Setters omitted for brevity, using public fields for direct GSON access.
    // 为了简洁省略Getter/Setter，使用Gson直接访问公共字段。
}