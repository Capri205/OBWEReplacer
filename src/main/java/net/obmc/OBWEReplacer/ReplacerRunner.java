package net.obmc.OBWEReplacer;

public interface ReplacerRunner extends Runnable {

	@Override
	public void run();
	
	public void start();
	
	public void cancel();

	public boolean isComplete();

	public int getBlocksProcessed();

	public int getChangedCount();

}
