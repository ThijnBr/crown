# Crown Plugin

A Spigot/Bukkit plugin that adds a special crown item with unique properties.

## Features

- Special golden helmet with customizable properties
- Unbreakable and with the stats of a Netherite helmet
- Grants extra health to the wearer (default: +2 hearts)
- Has curse of binding (configurable)
- Only the assigned owner can wear the crown
- Crown is automatically destroyed on death and returned to the owner on respawn
- Cannot be picked up by other players

## Installation

1. Download the latest release JAR file
2. Place the JAR file in your server's `plugins` folder
3. Start or restart your server
4. Edit the configuration file at `plugins/Crown/config.yml` if needed
5. Run `/crown reload` in-game to apply changes

## Commands

- `/crown` - Give yourself a crown (requires `crown.use` permission)
- `/crown give <player>` - Give a crown to a player (requires `crown.admin` permission)
- `/crown remove <player>` - Remove a crown from a player (requires `crown.admin` permission)
- `/crown reload` - Reload the plugin configuration (requires `crown.admin` permission)

## Permissions

- `crown.use` - Allows using the crown command to give yourself a crown
- `crown.admin` - Allows using administrative crown commands (give, remove, reload)

## Configuration

The plugin is highly configurable. You can customize:

- Material of the crown (default: golden helmet)
- Display name and lore
- Health bonus
- Armor value
- Enchantments and curses

See the `config.yml` file for all configuration options.

## Building from Source

1. Clone the repository
2. Build with Maven: `mvn clean package`
3. The built JAR file will be in the `target` directory 