package com.autocommand;

import com.autocommand.scheduler.ModCommands;
import com.autocommand.scheduler.TaskScheduler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleMod implements ModInitializer {
    public static final String MOD_ID = "scheduler_mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Singleton instance of scheduler
    private static TaskScheduler scheduler;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Scheduler Mod for 1.21.1");

        // Initialize Logic / 初始化逻辑
        scheduler = new TaskScheduler();
        scheduler.register(); // Start ticking / 开始 tick 监听

        // Register Commands / 注册指令
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ModCommands.register(dispatcher, scheduler);
        });
    }
}