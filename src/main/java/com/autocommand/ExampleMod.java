package com.autocommand;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.autocommand.Scheduler.JsonlFileManager;
import com.autocommand.Scheduler.ModCommands;
import com.autocommand.Scheduler.TaskScheduler;

/**
 * Main entry point for the mod.
 * 模组的主入口点。
 */
public class ExampleMod implements ModInitializer {
    public static final String MOD_ID = "taskmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing TaskMod...");

        // Initialize File System
        // 初始化文件系统
        JsonlFileManager.init();

        // Register Commands
        // 注册指令
        ModCommands.register();

        // Register Tick Scheduler
        // 注册Tick调度器
        TaskScheduler.register();
    }
}