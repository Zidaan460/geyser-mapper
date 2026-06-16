package com.geysermap;

import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class MappingGenerator {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Vanilla namespaces to skip
    private static final Set<String> VANILLA_NAMESPACES = Set.of("minecraft");

    public static int generateItems(MinecraftServer server) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("format_version", 2);

        JsonObject itemsObj = new JsonObject();
        int count = 0;

        for (Map.Entry<ResourceLocation, Item> entry : BuiltInRegistries.ITEM.entrySet()) {
            ResourceLocation rl = entry.getKey();

            // Skip vanilla items
            if (VANILLA_NAMESPACES.contains(rl.getNamespace())) continue;
            // Skip air
            if (entry.getValue() == Items.AIR) continue;

            String modId = rl.getNamespace();
            String itemPath = rl.getPath();
            String fullId = modId + ":" + itemPath;

            // Map to closest vanilla base item
            String vanillaBase = guessVanillaBase(entry.getValue());

            JsonArray definitions = new JsonArray();
            JsonObject def = new JsonObject();
            def.addProperty("name", fullId.replace(":", "_"));
            def.addProperty("display_name", toDisplayName(itemPath));
            def.addProperty("icon", modId + "_" + itemPath);

            // Components
            JsonObject components = new JsonObject();
            components.addProperty("item_properties", "{}");
            def.add("components", components);

            definitions.add(def);

            if (!itemsObj.has(vanillaBase)) {
                itemsObj.add(vanillaBase, definitions);
            } else {
                itemsObj.get(vanillaBase).getAsJsonArray().add(def);
            }

            count++;
        }

        root.add("items", itemsObj);

        // Save to custom_mappings folder
        Path outputDir = server.getServerDirectory().resolve("custom_mappings");
        Files.createDirectories(outputDir);
        Path outputFile = outputDir.resolve("geysermap_items.json");
        Files.writeString(outputFile, GSON.toJson(root));

        GeyserMapMod.LOGGER.info("GeyserMap: Generated {} mod items", count);
        return count;
    }

    public static int generateBlocks(MinecraftServer server) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("format_version", 1);

        JsonObject blocksObj = new JsonObject();
        int count = 0;

        for (Map.Entry<ResourceLocation, Block> entry : BuiltInRegistries.BLOCK.entrySet()) {
            ResourceLocation rl = entry.getKey();

            // Skip vanilla blocks
            if (VANILLA_NAMESPACES.contains(rl.getNamespace())) continue;
            if (entry.getValue() == Blocks.AIR) continue;

            String modId = rl.getNamespace();
            String blockPath = rl.getPath();
            String fullId = modId + ":" + blockPath;

            Block block = entry.getValue();
            List<BlockState> states = block.getStateDefinition().getPossibleStates();

            JsonObject blockDef = new JsonObject();
            blockDef.addProperty("name", modId + ":" + blockPath);

            // Components
            JsonObject components = new JsonObject();

            // Geometry - use default cube
            JsonObject geometry = new JsonObject();
            geometry.addProperty("identifier", "geometry.full_block");
            components.add("minecraft:geometry", geometry);

            // Material instances - use mod texture
            JsonObject materialInstances = new JsonObject();
            JsonObject allFaces = new JsonObject();
            allFaces.addProperty("texture", modId + "_" + blockPath);
            allFaces.addProperty("render_method", "opaque");
            materialInstances.add("*", allFaces);
            components.add("minecraft:material_instances", materialInstances);

            // Light emission (default 0)
            components.addProperty("minecraft:light_emission", 0);

            blockDef.add("components", components);

            // Block state overrides if multiple states
            if (states.size() > 1) {
                JsonObject stateOverrides = new JsonObject();
                for (BlockState state : states) {
                    String stateKey = buildStateKey(fullId, state, block);
                    JsonObject stateOverride = new JsonObject();
                    // Same components for all states (can be extended)
                    stateOverrides.add(stateKey, stateOverride);
                }
                blockDef.add("state_overrides", stateOverrides);
            }

            blocksObj.add(fullId, blockDef);
            count++;
        }

        root.add("blocks", blocksObj);

        Path outputDir = server.getServerDirectory().resolve("custom_mappings");
        Files.createDirectories(outputDir);
        Path outputFile = outputDir.resolve("geysermap_blocks.json");
        Files.writeString(outputFile, GSON.toJson(root));

        GeyserMapMod.LOGGER.info("GeyserMap: Generated {} mod blocks", count);
        return count;
    }

    public static void generateBedrockPack(MinecraftServer server) throws IOException {
        Path outputDir = server.getServerDirectory().resolve("packs");
        Files.createDirectories(outputDir);
        Path packZip = outputDir.resolve("geysermap_pack.zip");

        // Collect all mod namespaces and their items/blocks
        Map<String, List<String>> modItems = new HashMap<>();
        Map<String, List<String>> modBlocks = new HashMap<>();

        for (Map.Entry<ResourceLocation, Item> entry : BuiltInRegistries.ITEM.entrySet()) {
            ResourceLocation rl = entry.getKey();
            if (VANILLA_NAMESPACES.contains(rl.getNamespace())) continue;
            if (entry.getValue() == Items.AIR) continue;
            modItems.computeIfAbsent(rl.getNamespace(), k -> new ArrayList<>()).add(rl.getPath());
        }

        for (Map.Entry<ResourceLocation, Block> entry : BuiltInRegistries.BLOCK.entrySet()) {
            ResourceLocation rl = entry.getKey();
            if (VANILLA_NAMESPACES.contains(rl.getNamespace())) continue;
            if (entry.getValue() == Blocks.AIR) continue;
            modBlocks.computeIfAbsent(rl.getNamespace(), k -> new ArrayList<>()).add(rl.getPath());
        }

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(packZip.toFile()))) {

            // manifest.json
            JsonObject manifest = buildManifest();
            writeZipEntry(zos, "manifest.json", GSON.toJson(manifest));

            // Item texture definitions
            JsonObject itemTextureJson = new JsonObject();
            itemTextureJson.addProperty("resource_pack_name", "GeyserMap Pack");
            itemTextureJson.addProperty("texture_name", "atlas.items");
            JsonObject textureData = new JsonObject();

            for (Map.Entry<String, List<String>> modEntry : modItems.entrySet()) {
                String modId = modEntry.getKey();
                for (String itemPath : modEntry.getValue()) {
                    JsonObject textureDef = new JsonObject();
                    JsonArray textures = new JsonArray();
                    textures.add("textures/items/" + modId + "_" + itemPath);
                    textureDef.add("textures", textures);
                    textureData.add(modId + "_" + itemPath, textureDef);
                }
            }
            itemTextureJson.add("texture_data", textureData);
            writeZipEntry(zos, "textures/item_texture.json", GSON.toJson(itemTextureJson));

            // Terrain texture definitions for blocks
            JsonObject terrainTextureJson = new JsonObject();
            terrainTextureJson.addProperty("resource_pack_name", "GeyserMap Pack");
            terrainTextureJson.addProperty("texture_name", "atlas.terrain");
            JsonObject terrainData = new JsonObject();

            for (Map.Entry<String, List<String>> modEntry : modBlocks.entrySet()) {
                String modId = modEntry.getKey();
                for (String blockPath : modEntry.getValue()) {
                    JsonObject textureDef = new JsonObject();
                    JsonArray textures = new JsonArray();
                    textures.add("textures/blocks/" + modId + "_" + blockPath);
                    textureDef.add("textures", textures);
                    terrainData.add(modId + "_" + blockPath, textureDef);
                }
            }
            terrainTextureJson.add("texture_data", terrainData);
            writeZipEntry(zos, "textures/terrain_texture.json", GSON.toJson(terrainTextureJson));

            // Placeholder textures (1x1 purple-ish fallback)
            byte[] placeholderPng = getPlaceholderPng();
            for (Map.Entry<String, List<String>> modEntry : modItems.entrySet()) {
                String modId = modEntry.getKey();
                for (String itemPath : modEntry.getValue()) {
                    String texPath = "textures/items/" + modId + "_" + itemPath + ".png";
                    zos.putNextEntry(new ZipEntry(texPath));
                    zos.write(placeholderPng);
                    zos.closeEntry();
                }
            }
            for (Map.Entry<String, List<String>> modEntry : modBlocks.entrySet()) {
                String modId = modEntry.getKey();
                for (String blockPath : modEntry.getValue()) {
                    String texPath = "textures/blocks/" + modId + "_" + blockPath + ".png";
                    zos.putNextEntry(new ZipEntry(texPath));
                    zos.write(placeholderPng);
                    zos.closeEntry();
                }
            }

            GeyserMapMod.LOGGER.info("GeyserMap: Bedrock pack generated at {}", packZip);
        }
    }

    // ---- Helpers ----

    private static String guessVanillaBase(Item item) {
        // Try to find a reasonable vanilla base by checking item properties
        // Default to stick as a generic base (can be improved)
        String className = item.getClass().getSuperclass().getSimpleName().toLowerCase();
        if (className.contains("sword")) return "minecraft:wooden_sword";
        if (className.contains("pickaxe")) return "minecraft:wooden_pickaxe";
        if (className.contains("axe")) return "minecraft:wooden_axe";
        if (className.contains("shovel")) return "minecraft:wooden_shovel";
        if (className.contains("hoe")) return "minecraft:wooden_hoe";
        if (className.contains("helmet") || className.contains("cap")) return "minecraft:leather_helmet";
        if (className.contains("chestplate") || className.contains("tunic")) return "minecraft:leather_chestplate";
        if (className.contains("leggings") || className.contains("pants")) return "minecraft:leather_leggings";
        if (className.contains("boots")) return "minecraft:leather_boots";
        if (className.contains("bow")) return "minecraft:bow";
        if (className.contains("food") || className.contains("edible")) return "minecraft:apple";
        if (className.contains("block")) return "minecraft:paper";
        return "minecraft:stick";
    }

    private static String toDisplayName(String path) {
        String[] parts = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                sb.append(part.substring(1));
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    @SuppressWarnings("unchecked")
    private static String buildStateKey(String baseId, BlockState state, Block block) {
        StringBuilder sb = new StringBuilder(baseId).append("[");
        Collection<Property<?>> props = state.getProperties();
        boolean first = true;
        for (Property<?> prop : props) {
            if (!first) sb.append(",");
            sb.append(prop.getName()).append("=").append(((Property<Comparable>) prop).getName(state.getValue(prop)));
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    private static JsonObject buildManifest() {
        JsonObject manifest = new JsonObject();
        manifest.addProperty("format_version", 2);

        JsonObject header = new JsonObject();
        header.addProperty("description", "Auto-generated by GeyserMap");
        header.addProperty("name", "GeyserMap Pack");
        JsonArray headerUuid = new JsonArray();
        // Fixed UUIDs for consistency
        header.addProperty("uuid", "a1b2c3d4-e5f6-7890-abcd-ef1234567890");
        JsonArray version = new JsonArray();
        version.add(1); version.add(0); version.add(0);
        header.add("version", version);
        header.addProperty("min_engine_version", "1.20.0");
        manifest.add("header", header);

        JsonArray modules = new JsonArray();
        JsonObject resourceModule = new JsonObject();
        resourceModule.addProperty("type", "resources");
        resourceModule.addProperty("uuid", "b2c3d4e5-f6a7-8901-bcde-f12345678901");
        resourceModule.add("version", version);
        modules.add(resourceModule);
        manifest.add("modules", modules);

        return manifest;
    }

    private static void writeZipEntry(ZipOutputStream zos, String name, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(content.getBytes());
        zos.closeEntry();
    }

    // Minimal valid 8x8 purple PNG (placeholder texture)
    private static byte[] getPlaceholderPng() {
        // Pre-baked minimal 8x8 solid purple PNG bytes
        return new byte[]{
            (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52, // IHDR chunk length + type
            0x00, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00, 0x08, // width=8, height=8
            0x08, 0x02, 0x00, 0x00, 0x00, 0x4B, 0x6D, 0x29, 0x59, // bit depth=8, color=RGB, CRC
            0x00, 0x00, 0x00, 0x33, 0x49, 0x44, 0x41, 0x54, // IDAT chunk
            0x78, (byte)0x9C, 0x62, (byte)0xFC, (byte)0xCF, (byte)0xC0, (byte)0xF0,
            (byte)0x9F, (byte)0x81, (byte)0x81, 0x21, 0x00, (byte)0xC8, (byte)0xCC,
            (byte)0xCC, (byte)0xCC, (byte)0xF0, (byte)0xFF, (byte)0xFF, 0x3F, 0x00,
            0x06, 0x06, 0x06, 0x06, (byte)0x86, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0x01, 0x00, 0x00, (byte)0xFF, (byte)0xFF, 0x12, (byte)0xE4,
            0x08, (byte)0xB2, 0x61, (byte)0xC5, 0x37, (byte)0xEB,
            0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte)0xAE, 0x42, 0x60, (byte)0x82 // IEND
        };
    }
}
