package me.exote.webauction.listeners;

import me.exote.webauction.WebAuction;

import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.Listener;

public class WebAuctionServerListener implements Listener {
	private WebAuction plugin;

	public WebAuctionServerListener(WebAuction webAuction) {
		this.plugin = webAuction;
	}

	public void onPluginEnable(PluginEnableEvent event) {
		if (plugin.economy != null) {
			if (plugin.economy.isEnabled()) {
				//System.out.println("[" + plugin.getDescription().getName() + "] Payment method enabled: " + plugin.economy.getName() + ".");
			}
		}
		if (plugin.permission != null) {
			if (plugin.permission.isEnabled()) {
				//System.out.println("[" + plugin.getDescription().getName() + "] Permission method enabled: " + plugin.permission.getName() + ".");
			}
		}
	}
}