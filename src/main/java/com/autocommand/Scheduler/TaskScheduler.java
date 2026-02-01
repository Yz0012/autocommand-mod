package com.autocommand.scheduler;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Main Logic for checking and executing tasks.
 * 检查和执行任务的主逻辑。
 */
public class TaskScheduler {
    private final JsonlFileManager fileManager;
    private List<Task> memoryCache;
    @SuppressWarnings("unused")
    private long lastCheckTime = 0;

    public TaskScheduler() {
        this.fileManager = new JsonlFileManager();
        this.memoryCache = new ArrayList<>();
        // Load initially / 初始化加载
        refreshCache();
    }

    public void register() {
        // Execute at the end of server tick
        // 在服务器 tick 结束时执行
        ServerTickEvents.END_SERVER_TICK.register(this::onTick);
    }

    private void refreshCache() {
        this.memoryCache = fileManager.loadAllTasks();
    }

    /**
     * Called every tick.
     * 每个 tick 调用。
     */
    private void onTick(MinecraftServer server) {
        // 1. Monitor file changes (Simulated logic, or rely on internal state)
        // 1. 监听文件更改 (模拟逻辑，或者依赖内部状态)
        // For this example, we assume we drive the data, but if external change is
        // detected:
        // 在此示例中，假设我们驱动数据，但如果检测到外部更改：
        // if (fileManager.hasChanges()) refreshCache();

        long now = Instant.now().toEpochMilli();
        boolean needsSave = false;
        CommandSourceStack source = server.createCommandSourceStack();

        // 2. Find nearest tasks
        // 2. 查找最近的任务
        // Filter pending tasks / 过滤待处理任务
        List<Task> pendingTasks = new ArrayList<>();
        for (Task t : memoryCache) {
            if ("PENDING".equals(t.status)) {
                pendingTasks.add(t);
            }
        }

        // Sort by time / 按时间排序
        pendingTasks.sort(Comparator.comparingLong(t -> t.scheduledTime));

        for (Task task : pendingTasks) {
            long diff = task.scheduledTime - now;

            // Logic: If task time is within 1s (1000ms) or already passed
            // 逻辑：如果任务时间在 1秒 (1000ms) 内或已经过去
            if (diff <= 1000) {
                // Execute / 执行
                executeTask(server, source, task);

                // Mark status / 标记状态
                task.status = "EXECUTED";

                // If it's a loop task, add a new future task
                // 如果是循环任务，添加一个新的未来任务
                if ("LOOP".equals(task.type)) {
                    Task nextTask = new Task(task.command, now + (task.interval * 1000), "LOOP", task.interval);
                    memoryCache.add(nextTask); // Add to cache immediately / 立即添加到缓存
                }

                needsSave = true;
            } else {
                // If the nearest task is far in future, we can stop checking others (since
                // sorted)
                // 如果最近的任务还在很远的未来，可以停止检查其他任务（因为已排序）
                break;
            }
        }

        // 3. Save if changes happened
        // 3. 如果发生更改则保存
        if (needsSave) {
            fileManager.saveAllTasks(memoryCache);
        }
    }

    @SuppressWarnings("null")
    private void executeTask(MinecraftServer server, CommandSourceStack source, Task task) {
        try {
            System.out.println("Executing Task: " + task.command);
            server.getCommands().performPrefixedCommand(source, task.command);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Public API to add task from command
    // 从指令添加任务的公共 API
    public void scheduleTask(String command, int delaySeconds, boolean loop, int loopInterval) {
        long targetTime = Instant.now().toEpochMilli() + (delaySeconds * 1000L);
        Task task = new Task(command, targetTime, loop ? "LOOP" : "ONCE", loopInterval);

        // Add to cache and write directly
        // 添加到缓存并直接写入
        memoryCache.add(task);
        fileManager.addTask(task); // Appends to file immediately / 立即追加到文件
    }

    // Public API to stop tasks containing a specific command string
    // 停止包含特定指令字符串的任务的公共 API
    public int stopTasks(String commandMatch) {
        int count = 0;
        boolean needsSave = false;
        long now = Instant.now().toEpochMilli();

        // Iterate and remove future tasks
        // 遍历并移除未来任务
        // Using removeIf or Iterator is safer
        // 使用 removeIf 或 Iterator 更安全
        for (int i = 0; i < memoryCache.size(); i++) {
            Task t = memoryCache.get(i);
            // Check if future and matches command / 检查是否为未来任务且匹配指令
            if ("PENDING".equals(t.status) && t.scheduledTime > now && t.command.contains(commandMatch)) {
                // Ideally mark cancelled or remove. Prompt says "remove future tasks".
                // 理想情况下标记为取消或移除。提示要求 "去除任务"。
                memoryCache.remove(i);
                i--;
                count++;
                needsSave = true;
            }
        }

        if (needsSave) {
            fileManager.saveAllTasks(memoryCache);
        }
        return count;
    }
}
