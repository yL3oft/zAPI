## General Changes
- Complete code rework
- Improved performance and stability
- Migration from old minecraft format to minimessage
- This API now requires Java 17 or higher
- New YAML builder system for easier yaml management
- Removed cooldown system for commands
- New Logger system with different log levels & support for MiniMessage
- Improved Folia support and compatibility
- Improved Version Detection system
- bStats is now shaded into the API jar

## Class Changes
- `me.yleoft.zAPI.folia.FoliaRunnable` -> `me.yleoft.zAPI.scheduler.FoliaRunnable`
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
- `me.yleoft.zAPI.utils.SchedulerUtils` -> `me.yleoft.zAPI.utility.scheduler.Scheduler`
- `me.yleoft.zAPI.utils.SkullUtils` -> `me.yleoft.zAPI.skull.SkullBuilder`
- `me.yleoft.zAPI.utils.StringUtils` -> `me.yleoft.zAPI.utility.TextFormatter`


**Full Changelog**: https://github.com/yL3oft/zAPI/compare/1.5.2...2.0.0