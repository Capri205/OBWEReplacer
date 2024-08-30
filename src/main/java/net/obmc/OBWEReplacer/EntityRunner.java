package net.obmc.OBWEReplacer;

import java.util.ArrayList;
import java.util.Collection;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;

import com.sk89q.worldedit.math.BlockVector3;

public class EntityRunner implements ReplacerRunner
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
    private final boolean visible;

    private int taskId = 0;
 
    private int iteratorCount = 0;
    private int totalBlocksProcessed = 0;
    private int changedCount = 0;
    private boolean complete = false;
    
    private final int maxIterationsPerTick = 1000;
 
    public EntityRunner( World world, List<BlockVector3> blockList, String from_s, String to_s, String to_s_fill, boolean visible )
    {
        this.myPlugin = OBWEReplacer.getInstance();

        this.world = world;
        this.blockList = blockList;
        this.from_s = from_s;
        this.to_s = to_s;
        this.to_s_fill = to_s_fill;
        this.visible = visible;

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
 
 
        // iterate over block(s) in selection and look for nearby supported types that are the block or attached to the block and perform replacement
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
        	
        	// use a bounding box around current block and get entities within that box
        	BoundingBox box = new BoundingBox();
        	box.resize( mcBlock.getLocation().getX()-1, mcBlock.getLocation().getY()-1, mcBlock.getLocation().getZ()-1,
        				mcBlock.getLocation().getX()+2, mcBlock.getLocation().getY()+2, mcBlock.getLocation().getZ()+2 );
        	Collection<Entity> blockEntities = mcBlock.getWorld().getNearbyEntities( box, e -> e.getType() == EntityType.valueOf( from_s ) );     	
        	if ( blockEntities.isEmpty() ) {
        		continue;
        	}
        	
        	// iterate over entities and determine if entity is attached to current block or not
        	Iterator<Entity> eit = blockEntities.iterator();
        	while ( eit.hasNext() ) {
        		
        		// get entity and other data we need
        		Entity entity = eit.next();
        		ItemFrame fromItemframe = (ItemFrame)entity;
        		BlockFace frameAttachedFace = fromItemframe.getAttachedFace();
        		BlockFace frameFacing = fromItemframe.getFacing();
        		Block checkBlock = fromItemframe.getLocation().getBlock().getRelative( frameAttachedFace );

        		// get block frame is attached to and make sure it's the block we're currently processing
        		if ( checkBlock.getX() != bv.getX() || checkBlock.getY() != bv.getBlockY() || checkBlock.getZ() != bv.getZ() ) {
        			continue;
        		}
        		fromItemframe.getItem();
        		Rotation frameRotation = fromItemframe.getRotation();
        		
        		fromItemframe.remove();
        		
        		// replace the <from> entity with the <to> entity or block
        		switch (to_s) {
        		
        		    case "GLOW_ITEM_FRAME":
        		    case "ITEM_FRAME":
        		    	log.log(Level.INFO, "debug - to_s_fill: " + to_s_fill);
        		        ItemFrame newFrame = (ItemFrame) fromItemframe.getWorld().spawnEntity( fromItemframe.getLocation(), EntityType.valueOf( to_s ) );
        		        newFrame.setFacingDirection(frameFacing);
        		        if ( to_s_fill == null ) {
        		            newFrame.setItem( fromItemframe.getItem() );
        		        } else {
        		            newFrame.setItem( new ItemStack( Material.valueOf( to_s_fill ), 1 ) );
        		        }
                        if ( visible == false ) {
                        	newFrame.setVisible( false );
                        }
        		        newFrame.setRotation( frameRotation );
        		        break;
        		        
        		    default:
        		    	
        		        Material blockMaterial = Material.valueOf( to_s );
        		        
        		        fromItemframe.getLocation().getBlock().setType( Material.AIR );

        		        if ( blockMaterial.createBlockData() instanceof Directional && fromItemframe.getFacing() != BlockFace.UP ) {
        		        	
        		            Directional blockData = (Directional)blockMaterial.createBlockData();
        		            blockData.setFacing( fromItemframe.getFacing() );
        		            Block torchBlock = fromItemframe.getWorld().getBlockAt( fromItemframe.getLocation() );
        		            torchBlock.setType( blockMaterial );
        		            torchBlock.setBlockData( blockData );
        		            
        		        } else {
        		        	
        		            if ( fromItemframe.getFacing().equals( BlockFace.UP ) ) {
        		            	
        		                switch (to_s) {
        		                    case "REDSTONE_WALL_TORCH":
        		                        fromItemframe.getLocation().getBlock().setType( Material.REDSTONE_TORCH );
        		                        break;
        		                    case "WALL_TORCH":
        		                        fromItemframe.getLocation().getBlock().setType( Material.TORCH );
        		                        break;
        		                    case "SOUL_WALL_TORCH":
        		                        fromItemframe.getLocation().getBlock().setType( Material.SOUL_TORCH );
        		                        break;
        		                    default:
        		                        fromItemframe.getLocation().getBlock().setType( blockMaterial );
        		                        break;
        		                }
        		            } else {
        		            	
        		                fromItemframe.getLocation().getBlock().setType( blockMaterial );
        		            }
        		        }
        		        break;
        		}

        		changedCount++;
            }
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
