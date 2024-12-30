package net.obmc.OBWEReplacer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class CommandListener implements CommandExecutor {

	static Logger log = Logger.getLogger("Minecraft");
	
	private Component chatmsgprefix = null;
	private String logmsgprefix = null;
	
	private ReplacerRunner runner = null;
	private boolean commandInProgress = false;
	
    private List<com.sk89q.worldedit.math.BlockVector3> blockList = new ArrayList<>();

    
	public CommandListener() {
		chatmsgprefix = OBWEReplacer.getInstance().getChatMsgPrefix();
		logmsgprefix = OBWEReplacer.getInstance().getLogMsgPrefix();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		// process the command and any arguments
		if (!command.getName().equalsIgnoreCase("obrep")) {
			return true;
		}

		// usage if incorrect arguments passed
		if (args.length == 0 || (args.length == 1 && !args[0].equals("cancel"))) {
			Usage(sender);
			return true;
		}
		
		if (args[0].equals("cancel")) {
			if (!commandInProgress) {
				sender.sendMessage(chatmsgprefix.append(Component.text("No replace command running. Nothing to cancel", NamedTextColor.LIGHT_PURPLE)));
				return true;
			}
			runner.cancel();
			blockList.clear();
			commandInProgress = false;
			sender.sendMessage(chatmsgprefix.append(Component.text("Command canceled", NamedTextColor.GOLD)));
			return true;
		}
		
		// check for command already running. can't support multiple commands yet
		if (commandInProgress) {
			sender.sendMessage(chatmsgprefix.append(Component.text("Command already running! Wait or cancel it", NamedTextColor.LIGHT_PURPLE)));
			return true;
		}

		// parse arguments
		String from_s = args[0].toUpperCase();
		StringBuilder to_s_build = new StringBuilder(args[1].toUpperCase());
		for (int i = 2; i < args.length; i++) {
			to_s_build.append(args[i].toUpperCase());
		}
		String to_s = to_s_build.toString().trim();

		// get the somma separated options for the target if provided
		List<String> to_s_fillOptions = new ArrayList<String>();
		String fillPattern = "\\[(.*)\\]";
		Matcher matcher = Pattern.compile(fillPattern).matcher(to_s);
		if ((to_s.startsWith("ITEM_FRAME") || to_s.startsWith("GLOW_ITEM_FRAME")) && matcher.find()) {

			// get the target item frame
			to_s = to_s.substring(0, matcher.start());
			// get the item frame options
			String fillOptions = matcher.group(1);
			to_s_fillOptions = Arrays.asList(fillOptions.split(","));
		}
		
		// validate options if provided
		boolean visible = true;
		String to_s_fill = null;
		Iterator<String> foit = to_s_fillOptions.iterator();
		while(foit.hasNext()) {
			
			String option = foit.next();
			
			if (option.equals("VISIBLE")) {
				visible = true;
			} else if (option.equals("INVISIBLE")) {
				visible = false;
			} else {
				try {
					if (!fillIsValid(option)) {
						throw new Exception();
					}
					to_s_fill = option;
				} catch (Exception e) {
					sender.sendMessage(chatmsgprefix.append(Component.text("Invalid option provided '" + option + "'. See usage", NamedTextColor.RED)));
					return true;
				}
			}
		}

		// check from and to are supported and not the same 
		List<String> supportedTypes = OBWEReplacer.getInstance().getSupportedTypes();
		if (!supportedTypes.contains(from_s)) {
			sender.sendMessage(chatmsgprefix.append(Component.text("<from> is not a supported block or entity type", NamedTextColor.RED)));
			return true;
		}
		if (!supportedTypes.contains(to_s)) {
			sender.sendMessage(chatmsgprefix.append(Component.text("<to> is not a supported block or entity type", NamedTextColor.RED)));
			return true;
		}
		if (from_s.equals(to_s) && to_s_fillOptions.size() == 0) {
			sender.sendMessage(chatmsgprefix.append(Component.text("<from> and <to> are the same", NamedTextColor.RED)));
			return true;
		}

		// get world edit selection player has made
		com.sk89q.worldedit.entity.Player actor = (com.sk89q.worldedit.entity.Player) com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(sender);
		com.sk89q.worldedit.session.SessionManager manager = com.sk89q.worldedit.WorldEdit.getInstance().getSessionManager();
		com.sk89q.worldedit.LocalSession localSession = manager.get(actor);
		com.sk89q.worldedit.world.World selectionWorld = localSession.getSelectionWorld();
		com.sk89q.worldedit.regions.Region region;
		try {
			if (selectionWorld == null) throw new com.sk89q.worldedit.IncompleteRegionException();
			region = localSession.getSelection();
		} catch (com.sk89q.worldedit.IncompleteRegionException e) {
			sender.sendMessage(chatmsgprefix
			    .append(Component.text("You need to make a region selection", NamedTextColor.LIGHT_PURPLE))
			);
			return true;
		}
		
		// keep region selections to something not likely to cause issues
		long regionVolume = region.getVolume();
		if (regionVolume > 1000000L) {
			sender.sendMessage(chatmsgprefix
			    .append(Component.text("Region selection too large! Try smaller areas less than 1,000,000 blocks", NamedTextColor.LIGHT_PURPLE))
			);
			return true;			
		}

        // build a list of blocks from the region over which we can iterate and
		// remove the block from the list to determine we've finished processing
        Iterator<com.sk89q.worldedit.math.BlockVector3> bvit = region.iterator();
		long startms = System.currentTimeMillis();
        while(bvit.hasNext()) {
        	blockList.add(bvit.next());
        }
		long endms = System.currentTimeMillis();
		
		blockList.clear();

		Spliterator<com.sk89q.worldedit.math.BlockVector3> regionsplit = region.spliterator();
		Consumer<com.sk89q.worldedit.math.BlockVector3> push = b -> blockList.add(b);
		startms = System.currentTimeMillis();
		regionsplit.forEachRemaining(push);
		endms = System.currentTimeMillis();

		blockList.clear();
		
		Stream<com.sk89q.worldedit.math.BlockVector3> regionStream = StreamSupport.stream(
			Spliterators.spliteratorUnknownSize(
				region.iterator(), Spliterator.ORDERED
			), false
		);
		startms = System.currentTimeMillis();
		regionStream.forEachOrdered(push);
		endms = System.currentTimeMillis();
        
        if (blockList.isEmpty()) {
			sender.sendMessage(chatmsgprefix
			    .append(Component.text("Unable to clone region selection for processing. Try a smaller region perhaps", NamedTextColor.RED))
			);
			return true;
		}

		// determine if we are dealing with replacement of a block or entity and
		// pass block list to our runner which will perform the replacement
    	try {
    		runner = new EntityRunner(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(selectionWorld), blockList, from_s, to_s, to_s_fill, visible);
    	} catch (Exception e) {
    		runner = new BlockRunner(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(selectionWorld), blockList, from_s, to_s, to_s_fill, visible);
    	}
		commandInProgress = true;
		runner.start();
		
		// need a task running in the background to check when replace task is done and report back to the user
		new BukkitRunnable() {

			@Override
			public void run() {
				
				if (runner.isComplete()) {
					sender.sendMessage(chatmsgprefix
					    .append(Component.text(runner.getChangedCount() + " entit" + (runner.getChangedCount() != 1 ? "ies" : "y") + " changed", NamedTextColor.GOLD))
					);
					commandInProgress = false;
					this.cancel();
				} else {
					int blocksProcessed = runner.getBlocksProcessed();
					sender.sendMessage(chatmsgprefix
					    .append(Component.text(blocksProcessed + "/" + regionVolume + " (" + (int)(blocksProcessed * 100 / regionVolume) + "%)", NamedTextColor.GOLD))
					);
				}
			}
		}.runTaskTimer(OBWEReplacer.getInstance(), 10L, 40L);
		
		return true;
	}

    private void Usage(CommandSender sender) {
    	sender.sendMessage(chatmsgprefix.append(Component.text("/obrep <from torch|item frame> <to torch|item frame>"))
    	    .append(Component.text(" - Replace torches and item frames", NamedTextColor.GOLD))
    	);
    	sender.sendMessage(chatmsgprefix.append(Component.text("<to item frame>[fill options]"))
    	    .append(Component.text(" - Specify options for item frame in square brackets.", NamedTextColor.GOLD))
    	);
    	sender.sendMessage(chatmsgprefix
    	    .append(Component.text("Fill options are the item or block to put in the frame, and whether the frame is visible or invisible", NamedTextColor.GOLD))
    	);
    	sender.sendMessage(chatmsgprefix
    	    .append(Component.text("Eg. [RED_WOOL] or [LIME_WOOL,INVISIBLE] default is visible, but you can put VISIBLE if you want.", NamedTextColor.GOLD))
    	);
    	sender.sendMessage(chatmsgprefix.append(Component.text("/obrep cancel"))
    	    .append(Component.text(" - Cancel the command", NamedTextColor.GOLD))
    	);
    }
    
    // validate a fill block or item is valid
    private boolean fillIsValid(String to_s_fill) {
    	
		try {
			@SuppressWarnings("unused")
            Material checkFill = Material.valueOf(to_s_fill);
		} catch (Exception e) {
			return false;
		}
		//
		try {
			@SuppressWarnings("unused")
            ItemStack checkItem = new ItemStack(Material.valueOf(to_s_fill), 1);
		} catch (Exception e) {
			return false;
		}
		
		return true;
    }
}
