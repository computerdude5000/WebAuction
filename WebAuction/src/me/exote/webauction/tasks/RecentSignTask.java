package me.exote.webauction.tasks;

import java.util.ArrayList;
import java.util.List;

import me.exote.webauction.WebAuction;
import me.exote.webauction.dao.Auction;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.inventory.ItemStack;

public class RecentSignTask implements Runnable {

	private final WebAuction plugin;

	public RecentSignTask(WebAuction plugin) {
		this.plugin = plugin;
	}

	@Override
	public void run() {

		List<Location> toRemove = new ArrayList<Location>();

		int totalAuctionCount = plugin.dataQueries.getTotalAuctionCount();

		for (Location key : plugin.recentSigns.keySet()) {
			int offset = plugin.recentSigns.get(key);
			if (offset <= totalAuctionCount) {
				Auction offsetAuction = plugin.dataQueries.getAuctionForOffset(offset - 1);

				ItemStack stack = offsetAuction.getItemStack();
				int qty = stack.getAmount();
				String formattedPrice = plugin.economy.format(offsetAuction.getPrice());

				if (key.getBlock().getType() == Material.SIGN_POST || key.getBlock().getType() == Material.WALL_SIGN) {
					Sign thisSign = (Sign) key.getBlock().getState();
					thisSign.setLine(1, stack.getType().toString());
					thisSign.setLine(2, qty + "");
					thisSign.setLine(3, "" + formattedPrice);
					thisSign.update();
				} else {
					toRemove.add(key);
				}
			} else {
				if (key.getBlock().getType() == Material.SIGN_POST || key.getBlock().getType() == Material.WALL_SIGN) {
					Sign thisSign = (Sign) key.getBlock().getState();
					thisSign.setLine(1, "Recent");
					thisSign.setLine(2, offset + "");
					thisSign.setLine(3, "Not Available");
					thisSign.update();
				} else {
					toRemove.add(key);
				}
			}
		}

		// Remove any signs flagged for removal
		for (Location signLoc : toRemove) {
			plugin.recentSigns.remove(signLoc);
			plugin.dataQueries.removeRecentSign(signLoc);
			plugin.log.info(plugin.logPrefix + "Removed invalid sign at location: " + signLoc);
		}
	}
}
