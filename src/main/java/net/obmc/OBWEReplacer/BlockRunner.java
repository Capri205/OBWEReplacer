package net.obmc.OBWEReplacer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Rotation;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.RedstoneWallTorch;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Torch;
import org.bukkit.plugin.Plugin;
import com.sk89q.worldedit.math.BlockVector3;

public class BlockRunner implements ReplacerRunner
{
	
	static Logger log = Logger.getLogger("Minecraft");
	
	private String chatmsgprefix = null;
	private String logmsgprefix = null;
	
    private final Plugin myPlugin;
    
    private final World world;
    private List<BlockVector3> blockList = new ArrayList<>();
    private Iterator<com.sk89q.worldedit.math.BlockVector3> bvit = null;
    
    private final String from_s;
    private final String to_s;
    private final String to_s_fill;
    
    private int taskId = 0;
 
    private int iteratorCount = 0;
    private int totalBlocksProcessed = 0;
    private int changedCount = 0;
    private boolean complete = false;
    
    private final int maxIterationsPerTick = 1000;
 
    public BlockRunner( World world, List<BlockVector3> blockList, String from_s, String to_s, String to_s_fill )
    {
        this.myPlugin = OBWEReplacer.getInstance();
        
        this.world = world;
        this.blockList = blockList;
        this.from_s = from_s;
        this.to_s = to_s;
        this.to_s_fill = to_s_fill;
        
		chatmsgprefix = OBWEReplacer.getInstance().getChatMsgPrefix();
		logmsgprefix = OBWEReplacer.getInstance().getLogMsgPrefix();
    }
 
    public void start()
    {
        // reset whenever we call this method
        iteratorCount = 0;
        totalBlocksProcessed = 0;

        long delayBeforeStarting = 10;
        long delayBetweenRestarting = 10;

        // synchronous - thread safe
        this.taskId = this.myPlugin.getServer().getScheduler().runTaskTimer(
        		this.myPlugin, this, delayBeforeStarting, delayBetweenRestarting
        ).getTaskId();
 
        // asynchronous - NOT thread safe
        //this.taskId = this.myPlugin.getServer().getScheduler().runTaskTimerAsynchronously(this.myPlugin, this, delay_before_starting, delay_between_restarting).getTaskId();
 
        // Choose one or the other, not both.
        // They are both here simply for the sake of completion.
    }
 
    // this example will stagger parsing a huge list
 
    @Override
    public void run()
    {
        iteratorCount = 0;
        

        // while the list isnt empty, and we havent exceeded matIteraternsPerTick....
        // the loop will stop when it reaches 300 iterations OR the list becomes empty
        // this ensures that the server will be happy chappy, not doing too much per tick.
 
 
        // iterate over block(s) in selection and perform replacement if a match
        bvit = blockList.iterator();
        while( bvit.hasNext() && iteratorCount < maxIterationsPerTick ) {
        
        	// get block
        	com.sk89q.worldedit.math.BlockVector3 bv = bvit.next();
        	bvit.remove();

        	// keep track of blocks processed this run and overall
            iteratorCount++;
            totalBlocksProcessed++;
        	
        	// get minecraft block for world edit block, and mc world and location
        	Location mcLocation = new Location( world, bv.getX(), bv.getY(), bv.getZ() );
        	Block mcBlock = world.getBlockAt( mcLocation );
        	if ( !mcBlock.getType().name().equals( from_s ) ) {
        		continue;
        	}
        	
        	// get source block directional data if directional, otherwise default values
        	Directional blockDirectional = null;
        	BlockFace blockFacing = BlockFace.UP;
    		
        	// determine if source block is directional - wall torches for now, deal with rotatable later if required
        	if ( mcBlock.getBlockData() instanceof Directional ) {
        		blockDirectional = (Directional)mcBlock.getBlockData();
        		blockFacing = blockDirectional.getFacing();
        	}
    		
        	mcBlock.setType( Material.AIR );
        	
        	// replace the <from> block with the <to> entity or block
        	switch (to_s) {
            case "GLOW_ITEM_FRAME":
            case "ITEM_FRAME":
                ItemFrame newFrame = (ItemFrame) mcBlock.getWorld().spawnEntity(mcBlock.getLocation(), EntityType.valueOf(to_s));
                newFrame.setFacingDirection(blockFacing);
                if (to_s_fill != null) {
                    newFrame.setItem(new ItemStack(Material.valueOf(to_s_fill), 1));
                }
                break;
            default:
                mcBlock.setType(Material.valueOf(to_s));
                if (to_s.endsWith("WALL_TORCH")) {
                    Directional newBlockData = (Directional) mcBlock.getBlockData();
                    newBlockData.setFacing(blockFacing);
                    mcBlock.setBlockData(newBlockData);
                }
                break;
        	}

        	changedCount++;
        }
 
        // if we're done processing selection cancel the task
        if ( !bvit.hasNext() ) {
        	this.complete = true;
            this.myPlugin.getServer().getScheduler().cancelTask( this.taskId );
        }
 
    }

	public List<com.sk89q.worldedit.math.BlockVector3> getBlockListSize() {
		return blockList;
	}
	
	public int getChangedCount() {
		return this.changedCount;
	}
	
	public int getBlocksProcessed() {
		return this.totalBlocksProcessed;
	}

	public boolean isComplete() {
		return this.complete;
	}

	public int getId() {
		return this.taskId;
	}

	public void cancel() {
		this.complete = true;
        this.myPlugin.getServer().getScheduler().cancelTask( this.taskId );
	}

}
