## General Changes
- Added LanguageManager to load and manage language YAML files
- Fixed time parsing/formatting in StringUtils for consistent milliseconds handling
- Hardened utility APIs: ConfigUtils made non-instantiable; legacy colors exposed as an unmodifiable constant (ItemStackUtils.LEGACY_COLORS)
- Added unit tests for pure-logic utilities and Messages

**Full Changelog**: https://github.com/yL3oft/zAPI/compare/1.4.8...1.4.9