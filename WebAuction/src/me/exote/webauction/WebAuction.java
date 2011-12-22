package me.exote.webauction;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import me.exote.webauction.listeners.WebAuctionBlockListener;
import me.exote.webauction.listeners.WebAuctionPlayerListener;
import me.exote.webauction.listeners.WebAuctionServerListener;
import me.exote.webauction.tasks.RecentSignTask;
import me.exote.webauction.tasks.SaleAlertTask;
import me.exote.webauction.tasks.ShoutSignTask;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class WebAuction extends JavaPlugin {

	public String logPrefix = "[WebAuction] ";
	public Logger log = Logger.getLogger("Minecraft");

	private final WebAuctionPlayerListener playerListener = new WebAuctionPlayerListener(this);
	private final WebAuctionBlockListener blockListener = new WebAuctionBlockListener(this);
	private final WebAuctionServerListener serverListener = new WebAuctionServerListener(this);

	public MySQLDataQueries dataQueries;

	public Map<Player, Long> lastSignUse = new HashMap<Player, Long>();
	public Map<Location, Integer> recentSigns = new HashMap<Location, Integer>();
	public Map<Location, Integer> shoutSigns = new HashMap<Location, Integer>();

	public int signDelay = 0;

	public Permission permission = null;
	public Economy economy = null;

	public long getCurrentMilli() {
		return System.currentTimeMillis();
	}

	@Override
	public void onEnable() {

		log.info(logPrefix + "WebAuction is initializing");

		initConfig();
		String dbHost = getConfig().getString("MySQL.Host");
		String dbUser = getConfig().getString("MySQL.Username");
		String dbPass = getConfig().getString("MySQL.Password");
		String dbPort = getConfig().getString("MySQL.Port");
		String dbDatabase = getConfig().getString("MySQL.Database");
		boolean getMessages = getConfig().getBoolean("Misc.ReportSales");
		signDelay = getConfig().getInt("Misc.SignDelay");

		getCommand("wa").setExecutor(new WebAuctionCommands(this));

		setupEconomy();
		setupPermissions();

		// Set up DataQueries
		dataQueries = new MySQLDataQueries(this, dbHost, dbPort, dbUser, dbPass, dbDatabase);

		// Init tables
		log.info(logPrefix + "MySQL Initializing");
		dataQueries.initTables();

		// Build shoutSigns map
		shoutSigns.putAll(dataQueries.getShoutSignLocations());

		// Build recentSigns map
		recentSigns.putAll(dataQueries.getRecentSignLocations());

		// If reporting sales in game, schedule sales alert task
		if (getMessages == true) {
			getServer().getScheduler().scheduleAsyncRepeatingTask(this, new SaleAlertTask(this), 1 * 30L, 1 * 30L);
		}

		getServer().getScheduler().scheduleAsyncRepeatingTask(this, new ShoutSignTask(this), 1 * 90L, 1 * 90L);
		getServer().getScheduler().scheduleAsyncRepeatingTask(this, new RecentSignTask(this), 1 * 160L, 1 * 160L);

		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.SIGN_CHANGE, blockListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.PLUGIN_ENABLE, serverListener, Priority.Monitor, this);
	}

	@Override
	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);
		log.info(logPrefix + "Disabled. Bye :D");
	}

	private void initConfig() {
		getConfig().addDefault("MySQL.Host", "localhost");
		getConfig().addDefault("MySQL.Username", "root");
		getConfig().addDefault("MySQL.Password", "password123");
		getConfig().addDefault("MySQL.Port", "3306");
		getConfig().addDefault("MySQL.Database", "minecraft");
		getConfig().addDefault("Misc.ReportSales", false);
		getConfig().addDefault("Misc.SignDelay", 1000);
		getConfig().options().copyDefaults(true);
		saveConfig();
	}

	private Boolean setupPermissions() {
		RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(Permission.class);
		if (permissionProvider != null) {
			permission = permissionProvider.getProvider();
		}
		return (permission != null);
	}

	private Boolean setupEconomy() {
		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
		if (economyProvider != null) {
			economy = economyProvider.getProvider();
		}

		return (economy != null);
	}
}
