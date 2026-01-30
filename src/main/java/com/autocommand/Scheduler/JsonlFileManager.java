package com.autocommand.Scheduler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Manages the JSONL file operations: writing, rotating, and updating status.
 * 管理JSONL文件操作：写入、轮替（Rolling）和更新状态。
 */
public class JsonlFileManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("TaskMod-File");
    private static final Gson GSON = new GsonBuilder().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("task_logs");
    private static final int MAX_LINES = 100;
    private static final int MAX_FILES = 4;
    private static final String BASE_FILENAME = "tasks";

    /**
     * Initializes the directory.
     * 初始化目录。
     */
    public static void init() {
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create config directory", e);
        }
    }

    /**
     * Adds a new task to the latest log file. Handles rotation if necessary.
     * 将新任务添加到最新的日志文件中。如有必要，处理文件轮替。
     *
     * @param task The task object to save / 要保存的任务对象
     */
    public static synchronized void appendTask(TaskData task) {
        try {
            Path currentFile = getFile(0);
            
            // Check line count for rotation
            // 检查行数以进行轮替
            if (Files.exists(currentFile) && Files.lines(currentFile).count() >= MAX_LINES) {
                rotateFiles();
                currentFile = getFile(0); // Point to new empty file
            }

            String jsonLine = GSON.toJson(task);
            Files.writeString(currentFile, jsonLine + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
        } catch (IOException e) {
            LOGGER.error("Failed to append task", e);
        }
    }

    /**
     * Rotates files: 2->3 (delete old 3), 1->2, 0->1.
     * 轮替文件：2->3（删除旧3），1->2，0->1。
     */
    private static void rotateFiles() throws IOException {
        // Delete oldest allowed file if exists
        // 删除允许存在的最旧文件（如果存在）
        Path oldest = getFile(MAX_FILES - 1);
        if (Files.exists(oldest)) {
            Files.delete(oldest);
        }

        // Shift others down
        // 将其他文件向下移动
        for (int i = MAX_FILES - 2; i >= 0; i--) {
            Path source = getFile(i);
            Path target = getFile(i + 1);
            if (Files.exists(source)) {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /**
     * Reads all pending tasks from all valid log files.
     * 从所有有效的日志文件中读取所有待处理（PENDING）的任务。
     *
     * @return List of pending tasks / 待处理任务列表
     */
    public static synchronized List<TaskData> readPendingTasks() {
        List<TaskData> tasks = new ArrayList<>();
        // Read from newest to oldest
        // 从最新到最旧读取
        for (int i = 0; i < MAX_FILES; i++) {
            Path p = getFile(i);
            if (!Files.exists(p)) continue;

            try (BufferedReader reader = Files.newBufferedReader(p)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        TaskData t = GSON.fromJson(line, TaskData.class);
                        if ("PENDING".equals(t.status)) {
                            tasks.add(t);
                        }
                    } catch (Exception ignored) {}
                }
            } catch (IOException e) {
                LOGGER.error("Failed to read tasks from " + p.getFileName(), e);
            }
        }
        return tasks;
    }

    /**
     * Updates the status of a specific task in the files (e.g., PENDING -> EXECUTED).
     * 更新文件中特定任务的状态（例如：PENDING -> EXECUTED）。
     *
     * @param taskId The ID of the task / 任务ID
     * @param newStatus The new status / 新状态
     */
    public static synchronized void updateTaskStatus(String taskId, String newStatus) {
        for (int i = 0; i < MAX_FILES; i++) {
            Path p = getFile(i);
            if (!Files.exists(p)) continue;

            boolean modified = false;
            List<String> lines = new ArrayList<>();

            try {
                List<String> rawLines = Files.readAllLines(p);
                for (String line : rawLines) {
                    TaskData t = GSON.fromJson(line, TaskData.class);
                    if (t.id.equals(taskId)) {
                        t.status = newStatus;
                        lines.add(GSON.toJson(t));
                        modified = true;
                    } else {
                        lines.add(line);
                    }
                }

                if (modified) {
                    Files.write(p, lines, StandardOpenOption.TRUNCATE_EXISTING);
                    // If we found and updated it, we can stop searching usually,
                    // but assumes IDs are unique globally.
                    // 如果找到并更新了，通常可以停止搜索，假设ID是全局唯一的。
                    return;
                }
            } catch (IOException e) {
                LOGGER.error("Failed to update status in " + p.getFileName(), e);
            }
        }
    }

    /**
     * Helper to get file path by index.
     * 通过索引获取文件路径的助手方法。
     */
    private static Path getFile(int index) {
        return CONFIG_DIR.resolve(BASE_FILENAME + "_" + index + ".jsonl");
    }
}
