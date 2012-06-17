package net.slipcor.pvparena.modules.economy;

import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaPlayer.Status;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.command.PAAJoin;
import net.slipcor.pvparena.command.PAA_Command;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.managers.Arenas;
import net.slipcor.pvparena.managers.Teams;
import net.slipcor.pvparena.neworder.ArenaModule;
import net.slipcor.pvparena.register.payment.Method;
import net.slipcor.pvparena.register.payment.Method.MethodAccount;

public class EconomySupport extends ArenaModule {

	protected static Method eco = null;
	private static HashMap<String, Double> paPlayersBetAmount = new HashMap<String, Double>();
	private HashMap<String, Double> paPlayersJoinAmount = new HashMap<String, Double>();

	public EconomySupport() {
		super("EconomySupport");
	}

	@Override
	public String version() {
		return "v0.8.9.0";
	}

	@Override
	public void addSettings(HashMap<String, String> types) {
		types.put("money.entry", "int");
		types.put("money.reward", "int");
		types.put("money.killreward", "double");
		types.put("money.minbet", "double");
		types.put("money.maxbet", "double");
		types.put("money.betWinFactor", "double");
		types.put("money.betTeamWinFactor", "double");
		types.put("money.betPlayerWinFactor", "double");
		types.put("money.usePot", "boolean");
		types.put("money.winFactor", "double");
	}

	public boolean checkJoin(Arena arena, Player player) {
		if (arena.cfg.getInt("money.entry", 0) > 0) {
			if (EconomySupport.eco != null) {
				MethodAccount ma = EconomySupport.eco.getAccount(player
						.getName());
				if (ma == null) {
					db.s("Account not found: " + player.getName());
					return false;
				}
				if (!ma.hasEnough(arena.cfg.getInt("money.entry", 0))) {
					// no money, no entry!
					Arenas.tellPlayer(player, Language.parse("notenough",
							EconomySupport.eco.format(arena.cfg.getInt(
									"money.entry", 0))), arena);
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public void commitCommand(Arena arena, CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			Language.parse("onlyplayers");
			return;
		}

		db.i("bet command: " + StringParser.parseArray(args));
		
		Player player = (Player) sender;

		ArenaPlayer ap = ArenaPlayer.parsePlayer(player);
		
		// /pa bet [name] [amount]
		if (Teams.getTeam(arena, ap) != null) {
			Arenas.tellPlayer(player, Language.parse("betnotyours"), arena);
			return;
		}

		if (EconomySupport.eco == null)
			return;

		if (args[0].equalsIgnoreCase("bet")) {

			Player p = Bukkit.getPlayer(args[1]);
			if (p != null) {
				ap = ArenaPlayer.parsePlayer(p);
			}
			if ((p == null) && (Teams.getTeam(arena, args[1]) == null)
					&& (Teams.getTeam(arena, ap) == null)) {
				Arenas.tellPlayer(player, Language.parse("betoptions"), arena);
				return;
			}

			double amount = 0;

			try {
				amount = Double.parseDouble(args[2]);
			} catch (Exception e) {
				Arenas.tellPlayer(player,
						Language.parse("invalidamount", args[2]), arena);
				return;
			}
			MethodAccount ma = EconomySupport.eco.getAccount(player.getName());
			if (ma == null) {
				db.s("Account not found: " + player.getName());
				return;
			}
			if (!ma.hasEnough(amount)) {
				// no money, no entry!
				Arenas.tellPlayer(
						player,
						Language.parse("notenough",
								EconomySupport.eco.format(amount)), arena);
				return;
			}

			if (amount < arena.cfg.getDouble("money.minbet")
					|| (amount > arena.cfg.getDouble("money.maxbet"))) {
				// wrong amount!
				Arenas.tellPlayer(player, Language.parse("wrongamount",
						EconomySupport.eco.format(arena.cfg
								.getDouble("money.minbet")), EconomySupport.eco
								.format(arena.cfg.getDouble("money.maxbet"))),
						arena);
				return;
			}

			ma.subtract(amount);
			Arenas.tellPlayer(player, Language.parse("betplaced", args[1]),
					arena);
			paPlayersBetAmount.put(player.getName() + ":" + args[1], amount);
		} else {

			double amount = 0;

			try {
				amount = Double.parseDouble(args[0]);
			} catch (Exception e) {
				return;
			}
			MethodAccount ma = EconomySupport.eco.getAccount(player.getName());
			if (ma == null) {
				db.s("Account not found: " + player.getName());
				return;
			}
			if (!ma.hasEnough(amount)) {
				// no money, no entry!
				Arenas.tellPlayer(
						player,
						Language.parse("notenough",
								EconomySupport.eco.format(amount)), arena);
				return;
			}
			
			PAA_Command command = new PAAJoin();
			
			if (!command.checkJoin(arena, player)) {
				return;
			}

			ma.subtract(amount);
			Arenas.tellPlayer(player, Language.parse("betplaced", args[1]),
					arena);
			paPlayersJoinAmount.put(player.getName(), amount);
			command.commit(arena, player, null);
		}
	}

	@Override
	public boolean commitEnd(Arena arena, ArenaTeam aTeam) {
		if (EconomySupport.eco != null) {
			db.i("eConomy set, parse bets");
			for (String nKey : paPlayersBetAmount.keySet()) {
				db.i("bet: " + nKey);
				String[] nSplit = nKey.split(":");

				if (Teams.getTeam(arena, nSplit[1]) == null
						|| Teams.getTeam(arena, nSplit[1]).getName()
								.equals("free"))
					continue;

				if (nSplit[1].equalsIgnoreCase(aTeam.getName())) {
					double teamFactor = arena.cfg
							.getDouble("money.betTeamWinFactor")
							* arena.teamCount;
					if (teamFactor <= 0) {
						teamFactor = 1;
					}
					teamFactor *= arena.cfg.getDouble("money.betWinFactor");

					double amount = paPlayersBetAmount.get(nKey) * teamFactor;

					MethodAccount ma = EconomySupport.eco.getAccount(nSplit[0]);
					if (ma == null) {
						db.s("Account not found: " + nSplit[0]);
						return true;
					}
					ma.add(amount);
					try {
						Arenas.tellPlayer(
								Bukkit.getPlayer(nSplit[0]),
								Language.parse("youwon",
										EconomySupport.eco.format(amount)),
								arena);
					} catch (Exception e) {
						// nothing
					}
				}
			}
		}
		return false;
	}
	
	public void commitPlayerDeath(Arena arena, Player p,
			EntityDamageEvent cause) {
		killreward(arena, p, ArenaPlayer.getLastDamagingPlayer(cause));
	}

	@Override
	public void configParse(Arena arena, YamlConfiguration config, String type) {
		config.addDefault("money.entry", Integer.valueOf(0));
		config.addDefault("money.reward", Integer.valueOf(0));
		config.addDefault("money.killreward", Double.valueOf(0));
		config.addDefault("money.minbet", Double.valueOf(0));
		config.addDefault("money.maxbet", Double.valueOf(0));
		config.addDefault("money.betWinFactor", Double.valueOf(1));
		config.addDefault("money.betTeamWinFactor", Double.valueOf(1));
		config.addDefault("money.betPlayerWinFactor", Double.valueOf(1));
		config.addDefault("money.usePot", Boolean.valueOf(false));
		config.addDefault("money.winFactor", Double.valueOf(2));

		config.options().copyDefaults(true);
	}

	@Override
	public void giveRewards(Arena arena, Player player) {
		if (EconomySupport.eco != null) {
			for (String nKey : paPlayersBetAmount.keySet()) {
				String[] nSplit = nKey.split(":");

				if (nSplit[1].equalsIgnoreCase(player.getName())) {
					double playerFactor = arena.playerCount
							* arena.cfg.getDouble("money.betPlayerWinFactor");

					if (playerFactor <= 0) {
						playerFactor = 1;
					}

					playerFactor *= arena.cfg.getDouble("money.betWinFactor");

					double amount = paPlayersBetAmount.get(nKey) * playerFactor;

					MethodAccount ma = EconomySupport.eco.getAccount(nSplit[0]);
					ma.add(amount);
					try {

						PVPArena.instance.getAmm().announcePrize(
								arena,
								Language.parse("awarded",
										EconomySupport.eco.format(amount)));
						Arenas.tellPlayer(
								Bukkit.getPlayer(nSplit[0]),
								Language.parse("youwon",
										EconomySupport.eco.format(amount)),
								arena);
					} catch (Exception e) {
						// nothing
					}
				}
			}
			if (arena.cfg.getInt("money.reward", 0) > 0) {
				if (EconomySupport.eco != null) {
					MethodAccount ma = EconomySupport.eco.getAccount(player
							.getName());
					ma.add(arena.cfg.getInt("money.reward", 0));
					Arenas.tellPlayer(player, Language.parse("awarded",
							EconomySupport.eco.format(arena.cfg.getInt(
									"money.reward", 0))), arena);
				}
			}
			for (String nKey : paPlayersJoinAmount.keySet()) {

				if (nKey.equalsIgnoreCase(player.getName())) {
					double playerFactor = arena.cfg.getDouble("money.betWinFactor");

					double amount = paPlayersJoinAmount.get(nKey) * playerFactor;

					MethodAccount ma = EconomySupport.eco.getAccount(nKey);
					ma.add(amount);
					try {

						PVPArena.instance.getAmm().announcePrize(
								arena,
								Language.parse("awarded",
										EconomySupport.eco.format(amount)));
						Arenas.tellPlayer(
								Bukkit.getPlayer(nKey),
								Language.parse("youwon",
										EconomySupport.eco.format(amount)),
								arena);
					} catch (Exception e) {
						// nothing
					}
				}
			}
		}
	}

	@Override
	public void initLanguage(YamlConfiguration config) {
		config.addDefault("log.iconomyon", "<3 eConomy");
		config.addDefault("log.iconomyoff", "</3 eConomy");

		config.addDefault("lang.notenough", "You don't have %1%.");
		config.addDefault("lang.betnotyours",
				"Cannot place bets on your own match!");
		config.addDefault("lang.betoptions",
				"You can only bet on team name or arena player!");
		config.addDefault("lang.wrongamount",
				"Bet amount must be between %1% and %2%!");
		config.addDefault("lang.invalidamount", "Invalid amount: %1%");
		config.addDefault("lang.betplaced", "Your bet on %1% has been placed.");
		config.addDefault("lang.youwon", "You won %1%");
		config.addDefault("lang.joinpay", "You paid %1% to join the arena!");

		config.addDefault("lang.killreward", "You received %1% for killing %2%!");
		
		config.addDefault("lang.refunding", "Refunding %1%!");
	}
	
	private void killreward(Arena arena, Player p, Entity damager) {
		
	}

	@Override
	public void onEnable() {
		if (PVPArena.instance.getAmm().getModule("VaultSupport") == null) {
			Bukkit.getServer().getPluginManager()
					.registerEvents(new ServerListener(), PVPArena.instance);
		}
	}

	@Override
	public boolean parseCommand(String cmd) {
		try {
			double amount = Double.parseDouble(cmd);
			db.i("parsing join bet amount: " + amount);
			return true;
		} catch (Exception e) {
			return cmd.equalsIgnoreCase("bet");
		}

	}

	@Override
	public void parseInfo(Arena arena, CommandSender player) {
		player.sendMessage("");
		player.sendMessage("�6Economy:�f entry: "
				+ StringParser.colorVar(arena.cfg.getInt("money.entry", 0))
				+ " || reward: "
				+ StringParser.colorVar(arena.cfg.getInt("money.reward", 0))
				+ " || killreward: "
				+ StringParser.colorVar(arena.cfg.getDouble("money.killreward", 0))
				+ " || winFactor: "
				+ StringParser.colorVar(arena.cfg.getDouble("money.winFactor", 0)));

		player.sendMessage("minbet: "
				+ StringParser.colorVar(arena.cfg.getDouble("money.minbet", 0))
				+ " || maxbet: "
				+ StringParser.colorVar(arena.cfg.getDouble("money.maxbet", 0))
				+ " || betWinFactor: "
				+ StringParser.colorVar(arena.cfg.getDouble("money.betWinFactor", 0)));
		
		player.sendMessage("betTeamWinFactor: "
				+ StringParser.colorVar(arena.cfg.getDouble("money.betTeamWinFactor", 0))
				+ " || betPlayerWinFactor: "
				+ StringParser.colorVar(arena.cfg.getDouble("money.betPlayerWinFactor", 0)));
	}
	
	@Override
	public void parseJoin(Arena arena, Player player, String coloredTeam) {
		int entryfee = arena.cfg.getInt("money.entry", 0);
		if (entryfee > 0) {
			if (EconomySupport.eco != null) {
				MethodAccount ma = EconomySupport.eco.getAccount(player
						.getName());
				ma.subtract(entryfee);
				Arenas.tellPlayer(
						player,
						Language.parse("joinpay",
								EconomySupport.eco.format(entryfee)), arena);
			}
		}
	}

	@Override
	public void parseRespawn(Arena arena, Player player, ArenaTeam team,
			int lives, DamageCause cause, Entity damager) {
		killreward(arena, player, damager);
	}

	protected void pay(Arena arena, HashSet<String> result) {
		if (result == null || result.size() == arena.teamCount) {
			return;
		}
		if (EconomySupport.eco != null) {
			
			double pot = 0;
			double winpot = 0;
			
			for (String s : paPlayersBetAmount.keySet()) {
				String[] nSplit = s.split(":");
				
				pot += paPlayersBetAmount.get(s);
				
				if (result.contains(nSplit)) {
					winpot += paPlayersBetAmount.get(s);
				}
			}
			
			// placingplayer:aim, amount
			
			for (String nKey : paPlayersBetAmount.keySet()) {
				String[] nSplit = nKey.split(":");
				ArenaTeam team = Teams.getTeam(arena, nSplit[1]);
				if (team == null || team.getName().equals("free")) {
					if (Bukkit.getPlayerExact(nSplit[1]) == null) {
						continue;
					}
				}

				if (result.contains(nSplit[1])) {

					double amount = 0;
					
					if (arena.cfg.getBoolean("money.usePot")) {
						if (winpot > 0) {
							amount = pot * paPlayersBetAmount.get(nKey) / winpot;
						}
					} else {
						double teamFactor = arena.cfg
								.getDouble("money.betTeamWinFactor")
								* arena.teamCount;
						if (teamFactor <= 0) {
							teamFactor = 1;
						}
						teamFactor *= arena.cfg.getDouble("money.betWinFactor");
						amount = paPlayersBetAmount.get(nKey) * teamFactor;
					}

					MethodAccount ma = EconomySupport.eco.getAccount(nSplit[0]);
					if (ma == null) {
						db.s("Account not found: " + nSplit[0]);
						continue;
					}
					ma.add(amount);
					try {
						Arenas.tellPlayer(
								Bukkit.getPlayer(nSplit[0]),
								Language.parse("youwon",
										EconomySupport.eco.format(amount)),
								arena);
					} catch (Exception e) {
						// nothing
					}
				}
			}
		}
	}

	@Override
	public void reset(Arena arena, boolean force) {
		paPlayersBetAmount.clear();
		paPlayersJoinAmount.clear();
	}
	
	@Override
	public void resetPlayer(Arena arena, Player player) {
		ArenaPlayer ap = ArenaPlayer.parsePlayer(player);
		if (ap.getStatus().equals(Status.LOBBY) ||
				ap.getStatus().equals(Status.READY)) {
			int entryfee = arena.cfg.getInt("money.entry", 0);
			if (entryfee < 1) {
				return;
			}
			Arenas.tellPlayer(player, Language.parse("refunding", eco.format(entryfee)));
			MethodAccount ma = EconomySupport.eco.getAccount(player.getName());
			if (ma == null) {
				db.s("Account not found: " + player.getName());
				return;
			}
			ma.add(entryfee);
		}
	}

	@Override
	public void timedEnd(Arena arena, HashSet<String> result) {
		pay(arena, result);
	}
}
