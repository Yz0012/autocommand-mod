package com.autocommand.scheduler;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands; // Mojang Mapping
import net.minecraft.network.chat.Component; // Mojang Mapping

/**
 * Command Registration.
 * 指令注册。
 */
public class ModCommands {

    @SuppressWarnings("null")
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, TaskScheduler scheduler) {
        dispatcher.register(Commands.literal("schedule")

                // Subcommand: Add Once / 子指令：添加单次
                .then(Commands.literal("add")
                        .then(Commands.argument("time_seconds", IntegerArgumentType.integer(1))
                                .then(Commands.argument("command", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            int time = IntegerArgumentType.getInteger(context, "time_seconds");
                                            String cmd = StringArgumentType.getString(context, "command");

                                            scheduler.scheduleTask(cmd, time, false, 0);

                                            context.getSource().sendSystemMessage(
                                                    Component.literal("Task added! Execution in " + time + "s."));
                                            return 1;
                                        }))))

                // Subcommand: Add Loop / 子指令：添加循环
                .then(Commands.literal("loop")
                        .then(Commands.argument("interval_seconds", IntegerArgumentType.integer(1))
                                .then(Commands.argument("command", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            int interval = IntegerArgumentType.getInteger(context, "interval_seconds");
                                            String cmd = StringArgumentType.getString(context, "command");

                                            // Loop starts after 'interval' seconds initially
                                            scheduler.scheduleTask(cmd, interval, true, interval);

                                            context.getSource().sendSystemMessage(
                                                    Component.literal("Loop task added! Interval: " + interval + "s."));
                                            return 1;
                                        }))))

                // Subcommand: Stop / 子指令：停止
                .then(Commands.literal("stop")
                        .then(Commands.argument("command_match", StringArgumentType.string())
                                .executes(context -> {
                                    String match = StringArgumentType.getString(context, "command_match");
                                    int removed = scheduler.stopTasks(match);
                                    context.getSource().sendSystemMessage(Component
                                            .literal("Removed " + removed + " future tasks matching: " + match));
                                    return 1;
                                }))));
    }
}
