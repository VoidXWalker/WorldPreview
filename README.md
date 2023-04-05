# World Preview
[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/voidxwalker)

Minecraft mod that draws a preview of the world on the loading screen during chunk generation.
- **Legal** for the minecraft java edition leaderboards
- The **full chunk map** must be shown in the recording to get a run verified
- Switching to the F3 + Esc menu will make your run **count as f3**
- **Compatible** with all allowed mods
- World preview **doesn't modify world generation** in any way. It simply takes already completed chunks, converts them to client chunks (Chunks minecraft can render) and then draws these chunks on the loading screen.
- There are **no server side modifications** besides minecraft getting the random value for the player spawn earlier (Random is Random so it doesn't change anything)
- You will not be able to reset after ~ when the chunk map reaches 100%. This is intentional

![F3 + Esc example](https://github.com/VoidXWalker/WorldPreview/blob/1.16.1/WorldPreview-example.png?raw=true)
## Usage
The preview will always render when you create a world.

There are 5 ways the player can interact with the preview:

**Direct Inputs**
- Leave World Generation and Reset: Press the "Save and Quit to Title" button to instantly leave the World Generation to the title screen
- F3 + Esc: Press F3 and Esc at the same time to switch the pause menu to the F3+Esc pause menu (shown below) and press Esc to switch back. This will make your run count as f3.

![F3 + Esc example](https://github.com/VoidXWalker/WorldPreview/blob/1.16.1/WorldPreview-f3esc-example.png?raw=true)
--------
**Hotkeys**
(These can be changed in the Controls Screen)
- Leave World Generation and Reset with Hotkey, default button "g" (English keyboard): Press the hotkey button to instantly leave the World Generation to the title screen
- Cycle Chunk Map, default button "h" (English keyboard): Cycle through the 4 positions of the chunk map
- Freeze Preview, default button "j" (English keyboard): Freezes the preview (helps with CPU performance)

## For Macro Makers and Verifiers

The **State File** is created while the game is running and can be found
in `.minecraft/wpstateout.txt`. The file contains a single line of text containing
information about the game's current state, and overwrites itself whenever the state
changes. The following states will appear as lines in the file:
- `waiting`
- `inworld,paused`
- `inworld,unpaused`
- `inworld,gamescreenopen`
- `title`
- `generating,[percent]` (before preview starts)
- `previewing,[percent]`

**Extra logs**: In addition to the state file messages also appearing in the
log files, the mod prints 3 log lines:
- "Starting preview at (x, y, z)" at the start of the preview (Reset buttons unlocked)
- "Leaving world generation" when leaving world generation (Reset buttons locked)
- "Freezing Preview / Unfreezing Preview" when the freezing / unfreezing the preview
  You will not be able to reset after ~ when the chunk map reaches 100%. This is intentional.

## Authors

- [@Void_X_Walker](https://www.github.com/voidxwalker) (https://ko-fi.com/voidxwalker)
- [DuncanRuns](https://www.github.com/DuncanRuns)
- [pixfumy](https://www.github.com/pixfumy)


