package win.lava.teleport.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.lib.PaperLib;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import win.lava.area.events.AreaKickPlayerEvent;
import win.lava.teleport.LavaTeleport;
import win.lava.teleport.PlayerTeleportData;
import win.lava.teleport.TeleportManager;
import win.lava.teleport.tasks.TeleportTask;

@Singleton
public class AreaListener implements Listener {
  @Inject private LavaTeleport plugin;
  @Inject private TeleportManager teleportManager;

  @EventHandler
  public void onAreaKickPlayer(AreaKickPlayerEvent ev) {
    teleportManager.teleportPlayer(new TeleportTask(ev.getTarget(), plugin.getEssentialsSpawn().getSpawn("default")) {
          @Override
          public void run() {
            super.run();

            PaperLib.teleportAsync(player, location);
            plugin.log(player, location, "spawn");
          }

          @Override
          public PlayerTeleportData.TELEPORT_KIND getTeleportKind() {
            return null;
          }
        }
            .setCancelable(false)
            .setHighPriority(true)
    );
  }
}
