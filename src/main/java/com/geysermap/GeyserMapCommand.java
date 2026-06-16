package com.geysermap;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class GeyserMapCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("geysermap")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("generate")
                    .executes(GeyserMapCommand::generate))
                .then(Commands.literal("items")
                    .executes(GeyserMapCommand::generateItems))
                .then(Commands.literal("blocks")
                    .executes(GeyserMapCommand::generateBlocks))
        );
    }

    private static int generate(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal("§aGeyserMap: Generating items + blocks..."), false);
        try {
            int items = MappingGenerator.generateItems(ctx.getSource().getServer());
            int blocks = MappingGenerator.generateBlocks(ctx.getSource().getServer());
            MappingGenerator.generateBedrockPack(ctx.getSource().getServer());
            ctx.getSource().sendSuccess(() ->
                Component.literal("§aGeyserMap: Done! " + items + " items, " + blocks + " blocks mapped.\n" +
                    "§eFiles saved to: ./custom_mappings/geysermap_mappings.json\n" +
                    "§eResource pack: ./packs/geysermap_pack.zip"), false);
        } catch (Exception e) {
            GeyserMapMod.LOGGER.error("GeyserMap error: ", e);
            ctx.getSource().sendFailure(Component.literal("§cGeyserMap error: " + e.getMessage()));
        }
        return 1;
    }

    private static int generateItems(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal("§aGeyserMap: Generating items only..."), false);
        try {
            int count = MappingGenerator.generateItems(ctx.getSource().getServer());
            ctx.getSource().sendSuccess(() ->
                Component.literal("§aGeyserMap: Done! " + count + " items mapped.\n" +
                    "§eFile saved to: ./custom_mappings/geysermap_items.json"), false);
        } catch (Exception e) {
            GeyserMapMod.LOGGER.error("GeyserMap error: ", e);
            ctx.getSource().sendFailure(Component.literal("§cGeyserMap error: " + e.getMessage()));
        }
        return 1;
    }

    private static int generateBlocks(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal("§aGeyserMap: Generating blocks only..."), false);
        try {
            int count = MappingGenerator.generateBlocks(ctx.getSource().getServer());
            ctx.getSource().sendSuccess(() ->
                Component.literal("§aGeyserMap: Done! " + count + " blocks mapped.\n" +
                    "§eFile saved to: ./custom_mappings/geysermap_blocks.json"), false);
        } catch (Exception e) {
            GeyserMapMod.LOGGER.error("GeyserMap error: ", e);
            ctx.getSource().sendFailure(Component.literal("§cGeyserMap error: " + e.getMessage()));
        }
        return 1;
    }
}
