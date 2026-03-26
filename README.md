# ContainerNotifier Plugin

A lightweight Minecraft plugin that monitors storage containers for items exceeding configured limits and sends Discord webhook alerts. Use v1 for Minecraft versions below 1.21, and v2 or newer for Minecraft 1.21 or newer.

## Commands

- `/containernotifier help` - Show plugin information
- `/containernotifier reload` - Reload configuration
- `/containernotifier status` - Show plugin status
- `/containernotifier additem [threshold]` - Save held item into config

## Permissions

- `containernotifier.admin` - Access to plugin commands (default: op)
- `containernotifier.bypass.item.<id>` - Bypass monitoring for specific item. Here ID is the item ID in threshold config. Use `containernotifier.bypass.item.*` to bypass all checks (default: false)

## Building from Source

1. Clone the repository
2. Ensure you have Maven installed
3. Run `mvn clean install`
4. The compiled JAR will be in the `target/` directory

## Support

If you encounter issues join the Discord server: https://discord.com/invite/EBM9MKkD7F or open a issue in this repo.

## License

This project is licensed under the GPLv3 license.