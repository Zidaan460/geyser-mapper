# GeyserMap

Auto-generates Geyser custom mappings for NeoForge mods.

## Setup

1. Upload `GeyserMap-*.jar` ke folder `mods/` server kamu
2. Restart server
3. Join server sebagai operator (OP)

## Commands

| Command | Fungsi |
|---------|--------|
| `/geysermap generate` | Generate semua (item + block) |
| `/geysermap items` | Generate item saja |
| `/geysermap blocks` | Generate block saja |

## Output

Setelah command dijalankan, file akan muncul di:
- `custom_mappings/geysermap_items.json` → mapping item untuk Geyser
- `custom_mappings/geysermap_blocks.json` → mapping block untuk Geyser
- `packs/geysermap_pack.zip` → resource pack Bedrock (taruh di folder `packs/` Geyser)

## Catatan

- Hanya item/block dari **mod** yang di-generate (vanilla Minecraft dilewati)
- Texture placeholder warna ungu akan dipakai jika texture asli tidak tersedia
- Kamu perlu replace texture placeholder dengan texture asli dari mod

## Build dari source

Push ke GitHub, GitHub Actions akan otomatis build. Download JAR dari tab **Actions**.
