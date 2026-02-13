## General Changes
- Added [MiniPlaceholders](https://modrinth.com/plugin/miniplaceholders) support
- PlaceholdersHandler now works for both MiniPlaceholders and PlaceholderAPI at the same time, allowing you to use placeholders from both plugins without any issues.
- Changed the way placeholders are registered, in order to support both MiniPlaceholders and PlaceholderAPI, the placeholders are now registered in a more flexible way, allowing for better compatibility with both plugins.
- Fixes for offline player getter from PlayerHandler, you can now implement a custom offline player resolver to handle offline players.


**Full Changelog**: https://github.com/yL3oft/zAPI/compare/2.0.0...2.0.1