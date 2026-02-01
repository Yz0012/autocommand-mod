package com.autocommand.scheduler;

import com.google.gson.annotations.SerializedName;
import java.util.UUID;

/**
 * Task Data Model
 * 任务数据模型
 */
public class Task {
    // Unique ID for the task / 任务唯一ID
    public String id;

    // The command to execute / 要执行的指令
    public String command;

    // Scheduled execution time (UTC Epoch Millis) / 计划执行时间 (UTC毫秒时间戳)
    @SerializedName("scheduled_time")
    public long scheduledTime;

    // Task type: ONCE or LOOP / 任务类型：单次或循环
    public String type; // "ONCE", "LOOP"

    // Loop interval in seconds (only if type is LOOP) / 循环间隔秒数
    public long interval;

    // Task status / 任务状态
    // PENDING (Waiting), EXECUTED (Done), CANCELLED (Stopped)
    public String status;

    public Task(String command, long scheduledTime, String type, long interval) {
        this.id = UUID.randomUUID().toString();
        this.command = command;
        this.scheduledTime = scheduledTime;
        this.type = type;
        this.interval = interval;
        this.status = "PENDING";
    }
}
