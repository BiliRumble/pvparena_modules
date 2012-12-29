package net.slipcor.pvparena.modules.autovote;

import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.classes.PACheck;
import net.slipcor.pvparena.commands.PAA__Command;
import net.slipcor.pvparena.commands.PAG_Join;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.runnables.StartRunnable;

public class AutoVote extends ArenaModule {
	private static Arena a = null;
	private static HashMap<String, String> votes = new HashMap<String, String>();

	public AutoVote() {
		super("AutoVote");
	}

	@Override
	public String version() {
		return "v0.10.2.13";
	}
	
	@Override
	public boolean checkCommand(String cmd) {
		return cmd.startsWith("vote") || cmd.equals("!av") || cmd.equals("autovote");
	}

	@Override
	public PACheck checkJoin(CommandSender sender,
			PACheck res, boolean b) {
		if (res.hasError() || !b) {
			return res;
		}
		
		if (a != null && !arena.equals(a)) {
			res.setError(this, Language.parse(MSG.MODULE_AUTOVOTE_ARENARUNNING, arena.getName()));
			return res;
		}
		
		return res;
	}

	@Override
	public void commitCommand(CommandSender sender, String[] args) {

		if (args[0].startsWith("vote")) {

			votes.put(sender.getName(), arena.getName());
			Arena.pmsg(sender, Language.parse(MSG.MODULE_AUTOVOTE_YOUVOTED, arena.getName()));
			
			if (!arena.getArenaConfig().getBoolean(CFG.MODULES_ARENAVOTE_EVERYONE)) {
				return;
			}
			
			for (Player p : Bukkit.getOnlinePlayers()) {
				if (p == null) {
					continue;
				}
				Arena.pmsg(p, Language.parse(MSG.MODULE_AUTOVOTE_PLAYERVOTED, arena.getName(), sender.getName()));
			}
			return;
		} else {
			if (!PVPArena.hasAdminPerms(sender)
					&& !(PVPArena.hasCreatePerms(sender, arena))) {
				arena.msg(
						sender,
						Language.parse(MSG.ERROR_NOPERM,
								Language.parse(MSG.ERROR_NOPERM_X_ADMIN)));
				return;
			}
			
			if (!PAA__Command.argCountValid(sender, arena, args, new Integer[] { 2,3 })) {
				return;
			}
			
			// !av everyone
			// !av readyup X
			// !av seconds X
			
			if (args.length < 3 || args[1].equals("everyone")) {
				boolean b = arena.getArenaConfig().getBoolean(CFG.MODULES_ARENAVOTE_EVERYONE);
				arena.getArenaConfig().set(CFG.MODULES_ARENAVOTE_EVERYONE, !b);
				arena.getArenaConfig().save();
				arena.msg(sender, Language.parse(MSG.SET_DONE, CFG.MODULES_ARENAVOTE_EVERYONE.getNode(), String.valueOf(!b)));
				return;
			} else {
				CFG c = null;
				if (args[1].equals("readyup")) {
					c = CFG.MODULES_ARENAVOTE_READYUP;
				} else if (args[1].equals("seconds")) {
					c = CFG.MODULES_ARENAVOTE_SECONDS;
				}
				if (c != null) {
					int i = 0;
					try {
						i = Integer.parseInt(args[2]);
					} catch (Exception e) {
						arena.msg(sender, Language.parse(MSG.ERROR_NOT_NUMERIC, args[2]));
						return;
					}
					
					arena.getArenaConfig().set(c, i);
					arena.getArenaConfig().save();
					arena.msg(sender, Language.parse(MSG.SET_DONE, c.getNode(), String.valueOf(i)));
					return;
				}
			}
			
			arena.msg(sender, Language.parse(MSG.ERROR_ARGUMENT, args[1], "everyone | readyup | seconds"));
			return;
		}
	}

	@Override
	public void displayInfo(CommandSender player) {
		player.sendMessage("");
		player.sendMessage("�6ArenaVote:�f seconds:"
				+ StringParser.colorVar(arena.getArenaConfig().getInt(CFG.MODULES_ARENAVOTE_SECONDS))
				+ " | readyup: "
				+ StringParser.colorVar(arena.getArenaConfig().getInt(CFG.MODULES_ARENAVOTE_READYUP))
				+ " | everyone: "
				+ StringParser.colorVar(arena.getArenaConfig().getBoolean(CFG.MODULES_ARENAVOTE_EVERYONE)));
	}

	@Override
	public void reset(boolean force) {
		votes.clear();
		a = null;

		new AutoVoteRunnable(arena,
				arena.getArenaConfig().getInt(CFG.MODULES_ARENAVOTE_SECONDS));
	}

	public static void commit() {
		HashMap<String, Integer> counts = new HashMap<String, Integer>();
		int max = 0;
		for (String name : votes.values()) {
			int i = 0;
			if (counts.containsKey(name)) {
				i = counts.get(name);
			}

			counts.put(name, ++i);

			if (i > max) {
				max = i;
			}
		}

		for (String name : counts.keySet()) {
			if (counts.get(name) == max) {
				a = ArenaManager.getArenaByName(name);
			}
		}

		if (a == null) {
			a = ArenaManager.getFirst();
		}

		if (a == null) {
			return;
		}

		PAG_Join pj = new PAG_Join();

		HashSet<String> toTeleport = new HashSet<String>();
		
		if (a.getArenaConfig().getBoolean(CFG.MODULES_ARENAVOTE_EVERYONE)) {
			for (Player p : Bukkit.getOnlinePlayers()) {
				toTeleport.add(p.getName());
			}
		} else {
			for (String s : votes.keySet()) {
				toTeleport.add(s);
			}
		}
		
		for (String pName : toTeleport) {
			Player p = Bukkit.getPlayerExact(pName);
			if (p == null) {
				continue;
			}

			pj.commit(a, p, new String[0]);
		}

		new StartRunnable(a,
				a.getArenaConfig().getInt(CFG.MODULES_ARENAVOTE_READYUP));
	}
}
