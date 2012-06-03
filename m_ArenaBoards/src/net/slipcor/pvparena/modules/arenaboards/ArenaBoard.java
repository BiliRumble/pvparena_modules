package net.slipcor.pvparena.modules.arenaboards;

import java.util.HashMap;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Debug;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.managers.Arenas;
import net.slipcor.pvparena.managers.Statistics;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class ArenaBoard {

	public static final Debug db = new Debug(10);

	private Location location;
	protected static ArenaBoardManager abm;
	public Arena arena;

	public Statistics.type sortBy = Statistics.type.KILLS;

	private HashMap<Statistics.type, ArenaBoardColumn> columns = new HashMap<Statistics.type, ArenaBoardColumn>();

	/**
	 * create an arena board instance
	 * 
	 * @param loc
	 *            the location to hook to
	 * @param a
	 *            the arena to save the board to
	 */
	public ArenaBoard(Location loc, Arena a) {
		location = loc;
		arena = a;

		db.i("constructing arena board");
		construct();
	}

	/**
	 * actually construct the arena board, read colums, save signs etc
	 */
	private void construct() {
		Location l = location;
		int border = 10;
		try {
			Sign s = (Sign) l.getBlock().getState();
			BlockFace bf = getRightDirection(s);
			db.i("parsing signs:");
			do {
				Statistics.type t = null;
				try {
					t = Statistics.getTypeBySignLine(s.getLine(0));
				} catch (Exception e) {
					// nothing
				}

				columns.put(t, new ArenaBoardColumn(this, l));
				db.i("putting column type " + toString());
				l = l.getBlock().getRelative(bf).getLocation();
				s = (Sign) l.getBlock().getState();
			} while (border-- > 0);
		} catch (Exception e) {
			// no more signs, out!
		}
	}

	public Location getLocation() {
		return location;
	}

	/**
	 * get the right next board direction from the attachment data
	 * 
	 * @param s
	 *            the sign to check
	 * @return the blockface of the direction of the next column
	 */
	private BlockFace getRightDirection(Sign s) {
		byte data = s.getRawData();

		if (data == 2)
			return BlockFace.NORTH;
		if (data == 3)
			return BlockFace.SOUTH;
		if (data == 4)
			return BlockFace.WEST;
		if (data == 5)
			return BlockFace.EAST;

		return null;
	}

	/**
	 * save arena board statistics to each column
	 */
	public void update() {
		db.i("ArenaBoard update()");
		for (Statistics.type t : Statistics.type.values()) {
			db.i("checking stat: " + t.name());
			if (!columns.containsKey(t)) {
				continue;
			}
			db.i("found! reading!");
			String[] s = Statistics.read(
					Statistics.getStats(this.arena, sortBy), t, arena==null);
			columns.get(t).write(s);
		}
	}

	/**
	 * check if a player clicked a leaderboard sign
	 * 
	 * @param event
	 *            the InteractEvent
	 * @return true if the player clicked a leaderboard sign, false otherwise
	 */
	public static boolean checkInteract(PlayerInteractEvent event) {
		
		Player player = event.getPlayer();
		
		if (event.getClickedBlock() == null) {
			return false;
		}

		if (!abm.boards.containsKey(event.getClickedBlock().getLocation())) {
			return false;
		}

		ArenaBoard ab = abm.boards
				.get(event.getClickedBlock().getLocation());
		
		if (ab.arena == null) {
			if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
				ab.sortBy = Statistics.type.next(ab.sortBy);
				Arenas.tellPlayer(player,
						Language.parse("sortingby", ab.sortBy.toString()));
				return true;
			} else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
				ab.sortBy = Statistics.type.last(ab.sortBy);
				Arenas.tellPlayer(player,
						Language.parse("sortingby", ab.sortBy.toString()));
				return true;
			}
		} else {
			if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
				ab.sortBy = Statistics.type.next(ab.sortBy);
				Arenas.tellPlayer(player,
						Language.parse("sortingby", ab.sortBy.toString()), ab.arena);
				return true;
			} else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
				ab.sortBy = Statistics.type.last(ab.sortBy);
				Arenas.tellPlayer(player,
						Language.parse("sortingby", ab.sortBy.toString()), ab.arena);
				return true;
			}
		}
		
		return false;
	}

	public void destroy() {
		// TODO clear signs
		if (arena == null) {
			PVPArena.instance.getConfig().set("leaderboard", null);
			PVPArena.instance.saveConfig();
		} else {
			arena.cfg.set("spawns.leaderboard", null);
			arena.cfg.save();
		}
	}
}
