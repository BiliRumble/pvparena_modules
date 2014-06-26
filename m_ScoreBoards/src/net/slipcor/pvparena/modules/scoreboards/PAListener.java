package net.slipcor.pvparena.modules.scoreboards;

import net.slipcor.pvparena.core.Debug;
import net.slipcor.pvparena.events.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PAListener implements Listener {
    private final ScoreBoards module;

    public PAListener(ScoreBoards ea) {
        this.module = ea;
    }

    @EventHandler
    public void onDeath(PADeathEvent event) {
        if (module.getArena() != null && module.getArena().equals(event.getArena()))
            module.update(event.getPlayer());
    }

    @EventHandler
    public void onEnd(PAEndEvent event) {
        if (module.getArena() != null && module.getArena().equals(event.getArena()))
            module.stop();
    }

    @EventHandler
    public void onExit(PAExitEvent event) {
        if (module.getArena() == null) {
            Debug lala = new Debug(111);
            lala.i("PAExitEvent");
        } else {
            module.getArena().getDebugger().i("PAExitEvent");
        }
        if (module.getArena() != null && module.getArena().equals(event.getArena()))
            module.remove(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PAJoinEvent event) {
        if (module.getArena() == null || !module.getArena().isFightInProgress()) {
            return;
        }
        if (module.getArena().equals(event.getArena())) {
            module.add(event.getPlayer());
        }
    }

    @EventHandler
    public void onJoin(PATeamChangeEvent event) {
        if (module.getArena() == null || !module.getArena().isFightInProgress()) {
            return;
        }
        if (module.getArena().equals(event.getArena())) {
            module.change(event.getPlayer(), event.getFrom(), event.getTo());
        }
    }

    @EventHandler
    public void onKill(PAKillEvent event) {
        if (module.getArena() != null && module.getArena().equals(event.getArena()))
            module.update(event.getPlayer());
    }

    @EventHandler
    public void onLeave(PALeaveEvent event) {
        if (module.getArena() == null) {
            Debug lala = new Debug(111);
            lala.i("PALeaveEvent");
        } else {
            module.getArena().getDebugger().i("PALeaveEvent");
        }
        if (module.getArena() != null && module.getArena().equals(event.getArena()))
            module.remove(event.getPlayer());
    }

    @EventHandler
    public void onLose(PALoseEvent event) {
        if (module.getArena() != null && module.getArena().equals(event.getArena()))
            module.remove(event.getPlayer());
    }

    @EventHandler
    public void onStart(PAStartEvent event) {
        if (module.getArena() != null && module.getArena().equals(event.getArena()))
            module.start();
    }
}
