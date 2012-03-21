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

	public Map<String, Long> lastSignUse = new HashMap<String , Long>();
	public Map<Location, Integer> recentSigns = new HashMap<Location, Integer>();
	public Map<Location, Integer> shoutSigns = new HashMap<Location, Integer>();

	public int signDelay = 0;
	public int numberOfRecentLink = 0;
	
	public Boolean useSignLink = false;
	public Boolean useOriginalRecent = true;
	
	public Boolean showSalesOnJoin = false;

	public Permission permission = null;
	public Economy economy = null;

	public long getCurrentMilli() {
		return System.currentTimeMillis();
	}

	@Override
	public void onEnable() {

		log.info(logPrefix + "WebAuction is initializing.");

		initConfig();
		String dbHost = getConfig().getString("MySQL.Host");
		String dbUser = getConfig().getString("MySQL.Username");
		String dbPass = getConfig().getString("MySQL.Password");
		String dbPort = getConfig().getString("MySQL.Port");
		String dbDatabase = getConfig().getString("MySQL.Database");
		long saleAlertFrequency = getConfig().getLong("Updates.SaleAlertFrequency");
		long shoutSignUpdateFrequency = getConfig().getLong("Updates.ShoutSignUpdateFrequency");
		long recentSignUpdateFrequency = getConfig().getLong("Updates.RecentSignUpdateFrequency");
		boolean getMessages = getConfig().getBoolean("Misc.ReportSales");
		useOriginalRecent = getConfig().getBoolean("Misc.UseOriginalRecentSigns");
		showSalesOnJoin = getConfig().getBoolean("Misc.ShowSalesOnJoin");
		boolean useMultithreads = getConfig().getBoolean("Development.UseMultithreads");
		signDelay = getConfig().getInt("Misc.SignDelay");
		useSignLink = getConfig().getBoolean("SignLink.UseSignLink");
		numberOfRecentLink = getConfig().getInt("SignLink.NumberOfLatestAuctionsToTrack");

		getCommand("wa").setExecutor(new WebAuctionCommands(this));

		setupEconomy();
		setupPermissions();

		// Set up DataQueries
		dataQueries = new MySQLDataQueries(this, dbHost, dbPort, dbUser, dbPass, dbDatabase);

		// Init tables
		log.info(logPrefix + "MySQL Initializing.");
		dataQueries.initTables();

		// Build shoutSigns map
		shoutSigns.putAll(dataQueries.getShoutSignLocations());

		// Build recentSigns map
		recentSigns.putAll(dataQueries.getRecentSignLocations());

		// If reporting sales in game, schedule sales alert task
		if (useMultithreads){
			log.info(logPrefix + "Using Multiple Threads.");
			if (getMessages) {
				getServer().getScheduler().scheduleAsyncRepeatingTask(this, new SaleAlertTask(this), saleAlertFrequency, saleAlertFrequency);
			}

			getServer().getScheduler().scheduleAsyncRepeatingTask(this, new ShoutSignTask(this), shoutSignUpdateFrequency, shoutSignUpdateFrequency);
			getServer().getScheduler().scheduleAsyncRepeatingTask(this, new RecentSignTask(this), recentSignUpdateFrequency, recentSignUpdateFrequency);
		}else{
			log.info(logPrefix + "Using Single Thread.");
			if (getMessages) {
				getServer().getScheduler().scheduleSyncRepeatingTask(this, new SaleAlertTask(this), saleAlertFrequency, saleAlertFrequency);
			}

			getServer().getScheduler().scheduleSyncRepeatingTask(this, new ShoutSignTask(this), shoutSignUpdateFrequency, shoutSignUpdateFrequency);
			getServer().getScheduler().scheduleSyncRepeatingTask(this, new RecentSignTask(this), recentSignUpdateFrequency, recentSignUpdateFrequency);	
		}

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
		getConfig().addDefault("Misc.UseOriginalRecentSigns", false);
		getConfig().addDefault("Misc.ShowSalesOnJoin", false);
		getConfig().addDefault("Development.UseMultithreads", false);
		getConfig().addDefault("Misc.SignDelay", 1000);
		getConfig().addDefault("SignLink.UseSignLink", false);
		getConfig().addDefault("SignLink.NumberOfLatestAuctionsToTrack", 10);
		getConfig().addDefault("Updates.SaleAlertFrequency", 30L);
		getConfig().addDefault("Updates.ShoutSignUpdateFrequency", 90L);
		getConfig().addDefault("Updates.RecentSignUpdateFrequency", 160L);
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
