# SeedChecker
A Minecraft mod to speed up checking Minecraft seeds by hand.

## Motivation
There are a multitude of programs that help you find rare Minecraft seeds. However a lot of them require a lot of manual labor, by going into Minecraft and loading the world by hand and checking, if the seed meets the requirements.

This tool speeds up the process by allowing you to quickly generate worlds with the specified seeds

## Setup
1. In the main menu, click the button "Open seed list" (or open `.minecraft/seedchecker/seedlist.txt`)
2. Add your seeds into the text editor (each seed a new line)
3. **Save the list**
4. Press the keybind <kbd>O</kbd>

Now you should load into a world and it will have the first seed that you specified in the file

> [!TIP]
> You can also add text as a seed, just like in the vanilla GUI

Pressing <kbd>O</kbd> again will generate a world with the next seed in the file.

Once a world is generated, the first seed in the file will be removed, so restarting the game will keep your progress!
## Config
Set specific world configuration in `.minecraft/config/seedchecker.json`
```json
{
  "difficulty": "EASY",     // EASY, NORMAL, HARD, PEACEFUL
  "worldType": "normal",    // normal, flat, amplified, single_biome
  "hardcore": "false",            // true, false
  "worldFolderName": "SeedChecker World",   // Anything you want!
  "allowCommands": "true",    // true, false
  "gameMode": "CREATIVE"      // SURVIVAL, CREATIVE, SPECTATOR, ADVENTURE
}
```
> [!TIP]
> You can keep the game running while changing the config, it will automatically load once you save