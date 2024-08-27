package net.obmc.OBWEReplacer;

import java.util.ArrayList;
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

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class CommandListener implements CommandExecutor {

	static Logger log = Logger.getLogger("Minecraft");
	
	private String chatmsgprefix = null;
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
		if ( !command.getName().equalsIgnoreCase( "obrep" ) ) {
			return true;
		}

		// usage if incorrect arguments passed
		if ( args.length == 1 && !args[0].equals( "cancel" ) ) {
			Usage(sender);
			return true;
		}
		
		if ( args[0].equals( "cancel" ) ) {
			if ( !commandInProgress ) {
				sender.sendMessage( chatmsgprefix + "" + ChatColor.LIGHT_PURPLE + "No replace command running. Nothing to cancel" );
				return true;
			}
			runner.cancel();
			blockList.clear();
			commandInProgress = false;
			sender.sendMessage( chatmsgprefix + "" + ChatColor.GOLD + "Command canceled" );
			return true;
		}
		
		// check for command already running. can't support multiple commands yet
		if ( commandInProgress ) {
			sender.sendMessage( chatmsgprefix + "" + ChatColor.LIGHT_PURPLE + "Command already running! Wait or cancel it" );
			return true;
		}

		// parse arguments
		String from_s = args[0].toUpperCase();
		StringBuilder to_s_build = new StringBuilder( args[1].toUpperCase() );
		for ( int i = 2; i < args.length; i++ ) {
			to_s_build.append( args[i].toUpperCase() );
		}
		String to_s = to_s_build.toString().trim();
		log.log(Level.INFO, "debug - from_s: " + from_s);
		log.log(Level.INFO, "debug - to_s  : " + to_s);
		
		// check for block type to put into target item_frame '[' and ']' if target is an item frame
		String to_s_fill = null;
		String fillPattern = "\\[(.*)\\]";
		Matcher matcher = Pattern.compile( fillPattern ).matcher( to_s );
		if ( ( to_s.startsWith( "ITEM_FRAME" ) || to_s.startsWith( "GLOW_ITEM_FRAME" ) ) && matcher.find() ) {
			to_s_fill = matcher.group(1 );
			to_s = to_s.substring( 0, matcher.start() );
		}
		log.log(Level.INFO, "debug - target fill is " + to_s_fill);
		
		List<String> supportedTypes = OBWEReplacer.getInstance().getSupportedTypes();
		if ( !supportedTypes.contains( from_s ) ) {
			sender.sendMessage( chatmsgprefix + "" + ChatColor.RED + "<from> is not a supported block or entity type" );
			return true;
		}
		if ( !supportedTypes.contains( to_s ) ) {
			sender.sendMessage( chatmsgprefix + "" + ChatColor.RED + "<to> is not a supported block or entity type" );
			return true;
		}
		if ( from_s.equals( to_s ) && to_s_fill == null ) {
			sender.sendMessage( chatmsgprefix + "" + ChatColor.RED + "<from> and <to> are the same" );
			return true;			
		}
		if ( to_s_fill != null ) {
			
			// validate our fill block is valid
			try {
				Material checkFill = Material.valueOf( to_s_fill );
			} catch ( Exception e ) {
			
				sender.sendMessage( chatmsgprefix + "" + net.md_5.bungee.api.ChatColor.RED + "<to> fill material is not a valid block or entity" );
				return true;
			}
			//
			try {
				ItemStack checkItem = new ItemStack( Material.valueOf( to_s_fill ), 1 );
			} catch ( Exception e ) {
				sender.sendMessage( chatmsgprefix + "" + net.md_5.bungee.api.ChatColor.RED + "<to> fill material is not a valid for an item frame" );
				return true;
			}
		}

		// get world edit selection player has made
		com.sk89q.worldedit.entity.Player actor = (com.sk89q.worldedit.entity.Player) com.sk89q.worldedit.bukkit.BukkitAdapter.adapt( sender );
		com.sk89q.worldedit.session.SessionManager manager = com.sk89q.worldedit.WorldEdit.getInstance().getSessionManager();
		com.sk89q.worldedit.LocalSession localSession = manager.get( actor );
		com.sk89q.worldedit.world.World selectionWorld = localSession.getSelectionWorld();
		com.sk89q.worldedit.regions.Region region;
		try {
			if ( selectionWorld == null ) throw new com.sk89q.worldedit.IncompleteRegionException();
			region = localSession.getSelection();
		} catch ( com.sk89q.worldedit.IncompleteRegionException e ) {
			sender.sendMessage( chatmsgprefix + "" + ChatColor.LIGHT_PURPLE + "You need to make a region selection" );
			return true;
		}
		
		// keep region selections to something not likely to cause issues
		long regionVolume = region.getVolume();
		if ( regionVolume > 1000000L ) {
			sender.sendMessage( chatmsgprefix + "" + ChatColor.LIGHT_PURPLE + "Region selection too large! Try smaller areas less than 1,000,000 blocks" );
			return true;			
		}

        // build a list of blocks from the region over which we can iterate and
		// remove the block from the list to determine we've finished processing
		log.log(Level.INFO, "debug - region volume: " + region.getVolume());
        Iterator<com.sk89q.worldedit.math.BlockVector3> bvit = region.iterator();
		long startms = System.currentTimeMillis();
        while( bvit.hasNext() ) {
        	blockList.add( bvit.next() );
        }
		long endms = System.currentTimeMillis();
		log.log(Level.INFO, "debug - blockList time: " + (endms - startms));
		
		blockList.clear();

		Spliterator<com.sk89q.worldedit.math.BlockVector3> regionsplit = region.spliterator();
		log.log(Level.INFO, "debug - estimatesize: " + regionsplit.estimateSize());
		Consumer<com.sk89q.worldedit.math.BlockVector3> push = b -> blockList.add(b);
		startms = System.currentTimeMillis();
		regionsplit.forEachRemaining( push );
		endms = System.currentTimeMillis();
		log.log(Level.INFO, "debug - spliterator time: " + (endms - startms));

		blockList.clear();
		
		Stream<com.sk89q.worldedit.math.BlockVector3> regionStream = StreamSupport.stream(
			Spliterators.spliteratorUnknownSize(
				region.iterator(), Spliterator.ORDERED
			), false
		);
		startms = System.currentTimeMillis();
		regionStream.forEachOrdered( push );
		endms = System.currentTimeMillis();
		log.log(Level.INFO, "debug - block stream time: " + (endms - startms));
		
        
        if ( blockList.isEmpty() ) {
			sender.sendMessage( chatmsgprefix + "" + ChatColor.RED + "Unable to clone region selection for processing. Try a smaller region perhaps" );
			return true;
		}

		// determine if we are dealing with replacement of a block or entity and
		// pass block list to our runner which will perform the replacement
    	try {
    		EntityType fromType = EntityType.valueOf( from_s );
    		runner = new EntityRunner( com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(selectionWorld), blockList, from_s, to_s, to_s_fill );
    	} catch ( Exception e ) {
    		runner = new BlockRunner( com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(selectionWorld), blockList, from_s, to_s, to_s_fill );
    	}
		commandInProgress = true;
		runner.start();
		
		// need a task running in the background to check when replace task is done and report back to the user
		new BukkitRunnable() {

			@Override
			public void run() {
				
				if ( runner.isComplete() ) {
					sender.sendMessage( chatmsgprefix + "" + ChatColor.GOLD + "" + runner.getChangedCount() + " entit" + ( runner.getChangedCount() != 1 ? "ies" : "y" ) + " changed");
					commandInProgress = false;
					this.cancel();
				} else {
					int blocksProcessed = runner.getBlocksProcessed();
					sender.sendMessage( chatmsgprefix + "" + ChatColor.GOLD + "" + blocksProcessed + "/" + regionVolume + " (" + (int)( blocksProcessed * 100 / regionVolume ) + "%)" );
				}
			}
		}.runTaskTimer(OBWEReplacer.getInstance(), 10L, 40L);
		
		return true;
	}

    private void Usage(CommandSender sender) {
    	sender.sendMessage(chatmsgprefix + "/obrep <from torch|item frame> <to torch|item frame[fill item]>" + ChatColor.GOLD + " - Replace torches and item frames");
    	sender.sendMessage(chatmsgprefix + "/obrep cancel" + ChatColor.GOLD + " - Cancel the command");
    }
}
