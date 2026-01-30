## Dependency Changes
- [FoliaLib](https://github.com/TechnicallyCoded/FoliaLib) is now shaded into the API jar
- [bStats](https://github.com/Bastian/bStats) is now shaded into the API jar

## General Changes
- Full codebase rework
- Performance and stability improvements
- Migrated legacy Minecraft formatting to MiniMessage
- Java 17+ is now required
- New YAML builder system for easier configuration management
- Removed command cooldown system
- New logger system with multiple log levels and MiniMessage support
- Improved Folia support and compatibility
- Improved version detection system
- New command registration system (parameters, subcommands, tab completion, and more)
- Improved PlaceholderAPI hook system
- New hook registration system for easier hook management
- Dropped support for Minecraft 1.18.2 and below
- New external dependency management system with automatic downloads and shaded dependency handling
- zAPI can now be preloaded (recommended for hooks and early-init systems)
- New MathExpressionEvaluator for evaluating mathematical expressions from strings
- Reworked InventoryBuilder to support advanced slot parsing (ranges, lists, math, placeholders)
- Improved item loading with display conditions and per-slot placeholder resolution
- Added inventory-level placeholders (%rows%, %title%, %command%) available to all items
- Improved PluginYAML command instance tracking and cooldown bypass logic
- Added and improved Javadoc comments across multiple classes for clarity

## Class Changes
- `me.yleoft.zAPI.folia.FoliaRunnable` -> Replaced by FoliaLib
- `me.yleoft.zAPI.utils.SchedulerUtils` -> Replaced by FoliaLib
- `me.yleoft.zAPI.inventory.CustomInventory` -> `me.yleoft.zAPI.inventory.InventoryBuilder`
- `me.yleoft.zAPI.managers.LanguageManager` -> `me.yleoft.zAPI.configuration.LanguageManager`
- `me.yleoft.zAPI.managers.LogManager` -> `me.yleoft.zAPI.logging.LogManager`
- `me.yleoft.zAPI.managers.PluginYAMLManager` -> `me.yleoft.zAPI.utility.PluginYAML`
- `me.yleoft.zAPI.managers.UpdateManager` -> `me.yleoft.zAPI.update.UpdateManager`
- `me.yleoft.zAPI.mutable.MutableBlockLocation` -> `me.yleoft.zAPI.location.LocationHandler.MutableBlockLocation`
- `me.yleoft.zAPI.utils.ConfigUtils` -> `me.yleoft.zAPI.configuration.Path`
- `me.yleoft.zAPI.utils.HeadUtils` -> `me.yleoft.zAPI.skull.HeadProvider`
- `me.yleoft.zAPI.utils.InventoryUtils#cleanInventory` -> `me.yleoft.zAPI.item.NbtHandler#cleanInventory`
- `me.yleoft.zAPI.utils.ItemStackUtils` -> `me.yleoft.zAPI.item.ItemBuilder`
- `me.yleoft.zAPI.utils.LocationUtils` -> `me.yleoft.zAPI.location.LocationHandler`
- `me.yleoft.zAPI.utils.LogUtils` -> `me.yleoft.zAPI.logging.FileLogger`
- `me.yleoft.zAPI.utils.ModrinthDownloader` -> `me.yleoft.zAPI.update.ModrinthDownloader`
- `me.yleoft.zAPI.utils.NbtUtils` -> `me.yleoft.zAPI.item.NbtHandler`
- `me.yleoft.zAPI.utils.PlayerUtils` -> `me.yleoft.zAPI.player.PlayerHandler`
- `me.yleoft.zAPI.utils.ProtocolUtils` -> `me.yleoft.zAPI.utility.Version`
- `me.yleoft.zAPI.utils.SkullUtils` -> `me.yleoft.zAPI.skull.SkullBuilder`
- `me.yleoft.zAPI.utils.StringUtils` -> `me.yleoft.zAPI.utility.TextFormatter`


**Full Changelog**: https://github.com/yL3oft/zAPI/compare/1.5.2...2.0.0