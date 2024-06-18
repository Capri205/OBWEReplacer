# OBWEReplacer
Minecraft bukkit/spigot plugin to act as a companion to WorldEdit replace command<br>
to deal with blocks and entities that WorldEdit doesn't. For example, item frames.<br>

For now the replacer only supports item frames as that was the "driver" for creating this<br>
plugin. In our build server we use a lot of item frames for vehicle head lights and tail lights,<br>
as well as traffic lights. Using the glow item frame makes these come alive as it glows the<br>
item in the frame so at night these look fabulous! Converting these by hand would have been<br>
a mammoth undertaking, but with this we can swap thousands in one world edit selection!<br>

Regular item frames:<br>
<img src="https://ob-mc.net/repo/2023-11-26_10.15.19.png" width="450" height="247">

Glow item frames:<br>
<img src="https://ob-mc.net/repo/2023-11-26_10.15.53.png" width="450" height="247">

Traffic lights:<br>
<img src="https://ob-mc.net/repo/2023-11-26_10.08.13.png" width="450" height="247">

The goal will be to add more entities and blocks currently not covered by WorldEdit replace, or<br>
where it doesn't perform the replace correctly, like stair or fence blocks.

Select a region with worldedit, then use the '/obrep <from> <to>' command to perform the block<br>
replacement. 

For example, to switch item frame types<br>
/obrep item_frame glow_item_frame<br>
/obrep glow_item_frame item_frame<br>

Or cancel a long running replace command:<br>
/obrep cancel

Currently there are some hardcoded limits, such as number of blocks in the selection (1,000,000)<br>
and the number of blocks to process per server tick (1000). These will go into a config file soon.<br>

Compiled for 1.21 and Java 21.
