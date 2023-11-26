package net.obmc.OBWEReplacer;

import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;


public class OBWEReplacer extends JavaPlugin {

	static Logger log = Logger.getLogger("Minecraft");
	
	public static OBWEReplacer instance;

	private static String plugin = "OBWEReplacer";
	private static String pluginprefix = "[" + plugin + "]";
	private static String chatmsgprefix = ChatColor.AQUA + "" + ChatColor.BOLD + plugin + ChatColor.DARK_GRAY + ChatColor.BOLD + " » " + ChatColor.LIGHT_PURPLE + "";
	private static String logmsgprefix = pluginprefix + " » ";

	public OBWEReplacer() {
		instance = this;
	}
	
	// make our (public) main class methods and variables available to other classes
	public static OBWEReplacer getInstance() {
		return instance;
	}
	
	// enable the plugin
	public void onEnable() {
		
		/**
		 * Initialize Stuff
		 */
		initializeStuff();

		/**
		 * Register stuff
		 */
		registerStuff();
	}

	// save config disable the plugin
	public void onDisable() {
	}
	
	/**
	 * Initialize Stuff
	 */
	public void initializeStuff() {
	}
	
	/**
	 * Register things
	 */
	public void registerStuff() {
		// event listener for commands
		this.getCommand("obrep").setExecutor(new CommandListener());
	}

	/**
	 * Routine getters and setters
	 */

	// consistent messaging
	public static String getPluginName() {
		return plugin;
	}
	public static String getPluginPrefix() {
		return pluginprefix;
	}
	public String getChatMsgPrefix() {
		return chatmsgprefix;
	}
	public String getLogMsgPrefix() {
		return logmsgprefix;
	}
}
