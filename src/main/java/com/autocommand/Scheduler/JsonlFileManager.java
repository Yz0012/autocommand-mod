package com.autocommand.scheduler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handles JSONL file operations: Writing, Reading, Rotation, and Deletion.
 * 处理 JSONL 文件操作：写入、读取、轮替和删除。
 */
public class JsonlFileManager {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("scheduler_tasks");
    private static final int MAX_LINES = 100;
    private static final int MAX_FILES = 4;

    public JsonlFileManager() {
        try {
            if (!Files.exists(DIR))
                Files.createDirectories(DIR);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add a new task to the latest file. Handles file rotation.
     * 添加新任务到最新文件。处理文件轮替（超过100行新建，超过4个文件删旧）。
     */
    public void addTask(Task task) {
        try {
            Path latestFile = getLatestFile();

            // Check line count / 检查行数
            if (latestFile != null && countLines(latestFile) >= MAX_LINES) {
                rotateFiles(); // Create new file, maybe delete old / 创建新文件，可能删除旧文件
                latestFile = getLatestFile();
            }

            if (latestFile == null) {
                latestFile = createNewFile();
            }

            // Append JSONL / 追加 JSONL
            try (BufferedWriter writer = Files.newBufferedWriter(latestFile, StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND)) {
                writer.write(GSON.toJson(task));
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads all tasks from all files.
     * 读取所有文件中的所有任务。
     */
    public List<Task> loadAllTasks() {
        List<Task> tasks = new ArrayList<>();
        List<Path> files = getAllFiles(); // Sorted by name/time / 按名称或时间排序

        for (Path file : files) {
            try (BufferedReader reader = Files.newBufferedReader(file)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        try {
                            tasks.add(GSON.fromJson(line, Task.class));
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return tasks;
    }

    /**
     * Rewrites the files with updated task list (used for status updates or
     * cancellation).
     * 使用更新后的任务列表重写文件（用于状态更新或取消任务）。
     * Note: This is expensive but necessary to maintain a clean JSONL history
     * without duplicates.
     * 注意：这开销较大，但为了保持 JSONL 历史记录清洁且无重复是必要的。
     */
    public void saveAllTasks(List<Task> tasks) {
        // Clear directory first strictly or manage overwrite.
        // For simplicity: Delete all jsonl in dir and rewrite in chunks of 100.
        // 为简单起见：删除目录下所有 jsonl 并按 100 条一块重写。
        try {
            List<Path> files = getAllFiles();
            for (Path p : files)
                Files.delete(p);

            int count = 0;
            Path currentFile = createNewFile();
            BufferedWriter writer = Files.newBufferedWriter(currentFile, StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);

            for (Task task : tasks) {
                if (count >= MAX_LINES) {
                    writer.close();
                    currentFile = createNewFile();
                    writer = Files.newBufferedWriter(currentFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    count = 0;
                }
                writer.write(GSON.toJson(task));
                writer.newLine();
                count++;
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Helper: Get all .jsonl files sorted / 获取所有排序后的 jsonl 文件
    private List<Path> getAllFiles() {
        try (Stream<Path> stream = Files.list(DIR)) {
            return stream.filter(p -> p.toString().endsWith(".jsonl"))
                    .sorted() // filenames usually have timestamps or indices / 文件名通常含时间戳或索引
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    private Path getLatestFile() {
        List<Path> files = getAllFiles();
        if (files.isEmpty())
            return null;
        return files.get(files.size() - 1);
    }

    private Path createNewFile() {
        // Naming: tasks_TIMESTAMP.jsonl
        return DIR.resolve("tasks_" + System.currentTimeMillis() + ".jsonl");
    }

    private long countLines(Path path) throws IOException {
        try (Stream<String> stream = Files.lines(path)) {
            return stream.count();
        }
    }

    private void rotateFiles() throws IOException {
        List<Path> files = getAllFiles();
        // If we have 4 or more, delete the oldest before creating a new one
        // 如果有4个或更多，在创建新文件前删除最旧的
        while (files.size() >= MAX_FILES) {
            Files.delete(files.get(0));
            files.remove(0);
        }
        createNewFile();
    }

    // Check if files were modified externally (Simple hash or timestamp check)
    // 检查文件是否在外部被修改（简单的时间戳检查）
    public boolean hasChanges() {
        // Implementation omitted for brevity, usually checks lastModifiedTime of
        // directory or files
        // 为简洁省略，通常检查目录或文件的 lastModifiedTime
        return false;
    }
}
