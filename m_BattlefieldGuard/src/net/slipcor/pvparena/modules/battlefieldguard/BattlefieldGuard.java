package net.slipcor.pvparena.modules.battlefieldguard;

import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.loadables.ArenaModule;

public class BattlefieldGuard extends ArenaModule {
	protected HashMap<Arena, Integer> runnables = new HashMap<Arena, Integer>();
	
	private boolean setup = false;

	public BattlefieldGuard() {
		super("BattlefieldGuard");
	}
	
	@Override
	public String version() {
		return "v0.10.3.0";
	}
	
	@Override
	public boolean hasSpawn(String s) {
		return s.equalsIgnoreCase("exit");
	}

	@Override
	public void configParse(YamlConfiguration config) {
		if (setup)
			return;
		Bukkit.getScheduler().scheduleSyncRepeatingTask(PVPArena.instance, new BattleRunnable(), 20L, 20L);
		setup = true;
	}
}
