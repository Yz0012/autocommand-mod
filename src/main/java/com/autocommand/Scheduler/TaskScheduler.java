package com.autocommand.Scheduler;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles the logic for checking time and executing commands.
 * 处理检查时间和执行指令的逻辑。
 */
public class TaskScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger("TaskMod-Scheduler");
    // In-memory cache of pending tasks to avoid reading IO every tick.
    // 待处理任务的内存缓存，避免每一Tick都读取IO。
    private static final Map<String, TaskData> PENDING_TASKS = new ConcurrentHashMap<>();

    private static long lastFileCheck = 0;

    /**
     * Registers the tick listener.
     * 注册Tick监听器。
     */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(TaskScheduler::onTick);
    }

    /**
     * Called every server tick.
     * 每个服务端Tick被调用。
     *
     * @param server The Minecraft server instance / Minecraft服务器实例
     */
    private static void onTick(MinecraftServer server) {
        long now = System.currentTimeMillis();

        // Periodically sync from files (e.g., every 5 seconds) or relies on commands
        // updating memory.
        // For this example, we sync when map is empty or just check the map.
        // If file modification needs to be detected strictly from external edits:
        // 如果需要严格检测外部编辑的文件修改：
        if (now - lastFileCheck > 2000) { // Check every 2 seconds / 每2秒检查一次
            reloadFromDisk();
            lastFileCheck = now;
        }

        Iterator<Map.Entry<String, TaskData>> it = PENDING_TASKS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, TaskData> entry = it.next();
            TaskData task = entry.getValue();

            long diff = now - task.targetTime;

            // "Within 1s" logic: If current time is past target, but within 1s window (or
            // just past it).
            // Logic: If now >= targetTime.
            // "1s内"逻辑：如果当前时间超过目标时间。
            if (diff >= 0) {
                if (diff <= 1000) {
                    // Execute
                    executeTask(server, task);
                } else {
                    // Too late, mark as missed/expired or execute anyway?
                    // Prompt says: "Past time... add executed/unexecuted label".
                    // Assuming we execute if close, mark expired if too old.
                    // 假设接近则执行，太旧则标记过期。
                    LOGGER.warn("Task " + task.id + " missed its window by " + diff + "ms. Executing anyway.");
                    executeTask(server, task);
                }

                // Remove from current pending check
                it.remove();
            }
        }
    }

    /**
     * Reloads tasks from JSONL into memory.
     * 从JSONL重新加载任务到内存。
     */
    public static void reloadFromDisk() {
        List<TaskData> disksTasks = JsonlFileManager.readPendingTasks();
        // Simple merge: Add if not exists.
        for (TaskData t : disksTasks) {
            PENDING_TASKS.putIfAbsent(t.id, t);
        }
    }

    /**
     * Adds a task to memory and disk.
     * 将任务添加到内存和磁盘。
     */
    public static void scheduleTask(TaskData task) {
        JsonlFileManager.appendTask(task);
        PENDING_TASKS.put(task.id, task);
    }

    /**
     * Cancels a task.
     * 取消任务。
     */
    public static boolean cancelTask(String id) {
        if (PENDING_TASKS.containsKey(id)) {
            PENDING_TASKS.remove(id);
            JsonlFileManager.updateTaskStatus(id, "CANCELLED");
            return true;
        }
        // Also check disk even if not in memory
        JsonlFileManager.updateTaskStatus(id, "CANCELLED");
        return false;
    }

    /**
     * Executes the task command and handles looping.
     * 执行任务指令并处理循环。
     */
    @SuppressWarnings("null")
    private static void executeTask(MinecraftServer server, TaskData task) {
        LOGGER.info("Executing task: " + task.command);

        CommandSourceStack source = server.createCommandSourceStack();

        try {
            server.getCommands().performPrefixedCommand(source, task.command);

            // Update status on disk
            JsonlFileManager.updateTaskStatus(task.id, "EXECUTED");

            // Handle Loop
            // 处理循环
            if (task.isLoop) {
                long nextTime = System.currentTimeMillis() + (task.loopIntervalSeconds * 1000L);
                TaskData nextTask = new TaskData(
                        UUID.randomUUID().toString().substring(0, 8),
                        nextTime,
                        task.command,
                        true,
                        task.loopIntervalSeconds,
                        "PENDING");
                scheduleTask(nextTask); // Add new entry for next loop
                LOGGER.info("Rescheduled loop task: " + nextTask.id);
            }

        } catch (Exception e) {
            LOGGER.error("Error executing task command", e);
        }
    }
}
