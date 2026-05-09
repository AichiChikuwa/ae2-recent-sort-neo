# Applied History

Applied History is a Minecraft NeoForge mod that extends Applied Energistics 2 (AE2) terminals with a recent-history pin system.

![b74wF.gif](https://s13.gifyu.com/images/b74wF.gif)
![b74wU.gif](https://s13.gifyu.com/images/b74wU.gif)
![b74wV.gif](https://s13.gifyu.com/images/b74wV.gif)
![b74wZ.gif](https://s13.gifyu.com/images/b74wZ.gif)


## What

- adds a dedicated `History` toggle button under AE2's sort button
- pins recently interacted items in terminal rows *(not a full sort replacement)*
- tracks interactions such as import, export, and autocraft requests
- appends a history sentence to item tooltips (when history is enabled)
- stores history server-side per ME network.

## Why

- Refined Storage has this super dope last-modified sort. Unfortunately, nobody has made a submod for AE2 yet, so I decided to make my own.

## Q&As

### {insert modloader} support?

- No unless I see enough issues requesting it.

### {insert Minecraft version} support?

- 26.1.2 branch exists as a beta line, while 1.21.1 remains the main stable focus.
- 1.20.1 backport can be a thing if enough issues are requested.
- Probably won't support 1.19 or lower. Feel free to fork and do it yourself tho.

## Configuration

Options are available for:

- `historyRows`: number of history rows to pin
- `debugChat`: enables/disables in-game debug chat logging

History buffer size follows:

`historyRows * 9 + 27`

## License

This project is licensed under the GNU General Public License v3.0 only.  
See the `LICENSE` file for full text.

*This mod is made by human with help from AI.*