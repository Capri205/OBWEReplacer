# OBWEReplacer
Minecraft bukkit/spigot plugin to act as a companion to WorldEdit replace command<br>
to deal with blocks and entities that WorldEdit doesn't. For example, item frames.<br>

For now the replacer only supports item frames and torches, as that was the "driver" for creating<br>
this plugin. In our build server we use a lot of item frames for vehicle head lights and tail lights,<br>
as well as traffic lights and torches for airport lights. Using the glow item frame makes these come<br>
alive as it glows the item in the frame so at night these look fabulous! They are by far the best<br>
for simulating different colored point lights, especially if you make the frame invisible. Converting<br>
these by hand would have been an absolutely mammoth undertaking, but with this plugin we can swap out<br>
thousands in one world edit selection! You can even provide the content which which to fill the <to><br>
item frame and set whether the frame should be visible or invisible!<br>

Regular item frames:<br>
<img src="https://ob-mc.net/repo/obwereplacer_2.png" width="450" height="247">

Glow item frames:<br>
<img src="https://ob-mc.net/repo/obwereplacer_3.png" width="450" height="247">

Traffic lights:<br>
<img src="https://ob-mc.net/repo/obwereplacer_1.png" width="450" height="247">

Torches to item frames:<br>
<img src="https://ob-mc.net/repo/obwereplacer_7.png" width="450" height="247">
<img src="https://ob-mc.net/repo/obwereplacer_5.png" width="450" height="247">
<img src="https://ob-mc.net/repo/obwereplacer_4.png" width="450" height="247">

Cool creations: <br>
<img src="https://ob-mc.net/repo/obwereplacer_6.png" width="450" height="247">
<img src="https://ob-mc.net/repo/obwereplacer_8.png" width="450" height="247">

The goal will be to add more entities and blocks currently not covered by WorldEdit replace, or<br>
where it doesn't perform the replace correctly.

Select a region with worldedit, then use the '/obrep &lt;from&gt; &lt;to&gt;[material]' command to perform the block<br>
replacement. 

Examples:<br>
Switch between item frames keeping the existing content<br>
/obrep item_frame glow_item_frame<br>
/obrep glow_item_frame item_frame<br>

Add new item to target item frame<br>
/obrep item_frame glow_item_frame[ cyan_wool ]<br>

Switch between any type of torch or to an item frame, preserving direction of source torch<br>
/obrep redstone_torch glow_item_frame[red_wool]<br>
/obrep wall_torch item_frame[yellow_concrete]<br>

Make the frame invisible<br>
/obrep soul_torch glow_item_frame[diamond_axe, invisible]<br>
/obrep soul_wall_torch item_frame[ visible, diamond_axe]<br>

Or cancel a long running replace command:<br>
/obrep cancel<br>

Currently there are some hardcoded limits, such as number of blocks in the selection (1,000,000)<br>
and the number of blocks to process per server tick (1000). These will go into a config file soon.<br>

Compiled for 1.21 and Java 21.
