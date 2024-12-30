package net.obmc.OBWEReplacer;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class OBWEReplacer extends JavaPlugin {

	static Logger log = Logger.getLogger("Minecraft");
	
	public static OBWEReplacer instance;

	private static String plugin = "OBWEReplacer";
	private static String pluginprefix = "[" + plugin + "]";
    private static Component chatmsgprefix = Component.text()
            .color(NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true)
            .append(Component.text(plugin))
            .append(Component.text(" » ", NamedTextColor.DARK_GRAY, TextDecoration.BOLD))
            .append(Component.text("", NamedTextColor.LIGHT_PURPLE))
            .build();
	private static String logmsgprefix = pluginprefix + " » ";

    private List<String> supportedTypes = Arrays.asList(
        "ITEM_FRAME", "GLOW_ITEM_FRAME", "REDSTONE_TORCH", "REDSTONE_WALL_TORCH",
    	"TORCH", "WALL_TORCH", "SOUL_TORCH", "SOUL_WALL_TORCH" );

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
	public Component getChatMsgPrefix() {
		return chatmsgprefix;
	}
	public String getLogMsgPrefix() {
		return logmsgprefix;
	}
	
	public List<String> getSupportedTypes() {
		return this.supportedTypes;
	}
}
