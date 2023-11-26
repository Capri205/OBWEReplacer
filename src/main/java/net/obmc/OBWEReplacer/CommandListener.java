package net.obmc.OBWEReplacer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

public class CommandListener implements CommandExecutor {

	static Logger log = Logger.getLogger("Minecraft");
	
	private String chatmsgprefix = null;
	private String logmsgprefix = null;
	
	private StaggeredRunnable stag = null;
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
		if ( args.length < 1 ) {
			Usage(sender);
			return true;
		}
		if ( args[0].equals( "cancel" ) ) {
			if ( !commandInProgress ) {
				sender.sendMessage( chatmsgprefix + "" + ChatColor.LIGHT_PURPLE + "No replace command running. Nothing to cancel" );
				return true;
			}
			stag.cancel();
			blockList.clear();
			commandInProgress = false;
			sender.sendMessage( chatmsgprefix + "" + ChatColor.GOLD + "Command canceled" );
			return true;
		}
		if ( args.length != 2 ) {
			Usage(sender);
			return true;
		}
		
		// check for command already running. can't support multiple commands yet
		if ( commandInProgress ) {
			sender.sendMessage( chatmsgprefix + "" + ChatColor.LIGHT_PURPLE + "Command already running! Wait or cancel it" );
			return true;
		}
		
		String from_s = args[0].toUpperCase();
		String to_s = args[1].toUpperCase();

		if ( !from_s.equals( "ITEM_FRAME") && !from_s.equals( "GLOW_ITEM_FRAME" ) ) {
			sender.sendMessage( chatmsgprefix + "" + ChatColor.RED + "<from> is not an item_frame or glow_item_frame" );
			return true;
		}
		if ( !to_s.equals( "ITEM_FRAME") && !to_s.equals( "GLOW_ITEM_FRAME" ) ) {
			sender.sendMessage( chatmsgprefix + "" + ChatColor.RED + "<to> is not an item_frame or glow_item_frame" );
			return true;
		}
		if ( from_s.equals( to_s ) ) {
			sender.sendMessage( chatmsgprefix + "" + ChatColor.RED + "<from> and <to> are the same" );
			return true;			
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
			sender.sendMessage( chatmsgprefix + "" + ChatColor.LIGHT_PURPLE + "Region selection too large! Try smaller areas less than 1000000 blocks" );
			return true;			
		}

        // build a list of blocks from the region over which we can iterate and
		// remove the block from the list to determine we've finished processing
        Iterator<com.sk89q.worldedit.math.BlockVector3> bvit = region.iterator();
        while( bvit.hasNext() ) {
        	blockList.add( bvit.next() );
        }
		if ( blockList.isEmpty() ) {
			sender.sendMessage( chatmsgprefix + "" + ChatColor.RED + "Unable to clone region selection for processing. Try a smaller region perhaps" );
			return true;
		}
		
		// pass block list to our worker routine which will perform the entity and
		// block iteration in a runnable so as not to cause problems to the server
		stag = new StaggeredRunnable( com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(selectionWorld), blockList, from_s, to_s );
		commandInProgress = true;
		stag.start();
		
		// need a task running in the background to check when replace task is done and report back to the user
		new BukkitRunnable() {

			@Override
			public void run() {
				
				if ( stag.isComplete() ) {
					sender.sendMessage( chatmsgprefix + "" + ChatColor.GOLD + "" + stag.getChangedEntityCount() + " entit" + ( stag.getChangedEntityCount() != 1 ? "ies" : "y" ) + " changed");
					commandInProgress = false;
					this.cancel();
				} else {
					int blocksProcessed = stag.getBlocksProcessed();
					sender.sendMessage( chatmsgprefix + "" + ChatColor.GOLD + "" + blocksProcessed + "/" + regionVolume + " (" + (int)( blocksProcessed * 100 / regionVolume ) + "%)" );
				}
			}
		}.runTaskTimer(OBWEReplacer.getInstance(), 10L, 40L);
		
		return true;
	}

    private void Usage(CommandSender sender) {
    	sender.sendMessage(chatmsgprefix + "/obrep <from itemframe> <to itemframe>" + ChatColor.GOLD + " - Replace item frame");
    	sender.sendMessage(chatmsgprefix + "/obrep cancel" + ChatColor.GOLD + " - Cancel the command");
    }
}
