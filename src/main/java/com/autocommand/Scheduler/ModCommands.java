package com.autocommand.Scheduler;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/**
 * Registers the game commands.
 * 注册游戏指令。
 */
public class ModCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register(ModCommands::registerCommands);
    }

    /**
     * Definitions of the commands.
     * 指令定义。
     */
    @SuppressWarnings("null")
    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {

        // /task add <seconds> <command>
        dispatcher.register(Commands.literal("task")
            .then(Commands.literal("add")
                .then(Commands.argument("seconds", IntegerArgumentType.integer(1))
                    .then(Commands.argument("command", StringArgumentType.greedyString())
                        .executes(context -> {
                            int seconds = IntegerArgumentType.getInteger(context, "seconds");
                            String commandStr = StringArgumentType.getString(context, "command");

                            long targetTime = System.currentTimeMillis() + (seconds * 1000L);
                            String id = UUID.randomUUID().toString().substring(0, 8);

                            TaskData task = new TaskData(id, targetTime, commandStr, false, 0, "PENDING");
                            TaskScheduler.scheduleTask(task);

                            context.getSource().sendSuccess(() -> Component.literal("Task scheduled: " + id + " in " + seconds + "s"), false);
                            return 1;
                        })
                    )
                )
            )
            // /task loop <interval> <command>
            .then(Commands.literal("loop")
                .then(Commands.argument("interval", IntegerArgumentType.integer(1))
                    .then(Commands.argument("command", StringArgumentType.greedyString())
                        .executes(context -> {
                            int interval = IntegerArgumentType.getInteger(context, "interval");
                            String commandStr = StringArgumentType.getString(context, "command");

                            long targetTime = System.currentTimeMillis() + (interval * 1000L);
                            String id = UUID.randomUUID().toString().substring(0, 8);

                            TaskData task = new TaskData(id, targetTime, commandStr, true, interval, "PENDING");
                            TaskScheduler.scheduleTask(task);

                            context.getSource().sendSuccess(() -> Component.literal("Loop task scheduled: " + id + " every " + interval + "s"), false);
                            return 1;
                        })
                    )
                )
            )
            // /task stop <id>
            .then(Commands.literal("stop")
                .then(Commands.argument("id", StringArgumentType.string())
                    .executes(context -> {
                        String id = StringArgumentType.getString(context, "id");
                        boolean found = TaskScheduler.cancelTask(id);

                        if (found) {
                            context.getSource().sendSuccess(() -> Component.literal("Task cancelled: " + id), false);
                        } else {
                            context.getSource().sendFailure(Component.literal("Task not found or already executed: " + id));
                        }
                        return 1;
                    })
                )
            )
        );
    }
}
