package win.lava.teleport.tasks;

import com.earth2me.essentials.utils.LocationUtil;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import io.papermc.lib.PaperLib;
import org.bukkit.*;
import org.bukkit.entity.Player;
import win.lava.teleport.CancelReason;
import win.lava.teleport.LavaTeleport;
import win.lava.teleport.PlayerTeleportData;
import win.lava.teleport.Settings;

import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static win.lava.teleport.Lang.t;

public class RandomTeleportTask extends TeleportTask {
  protected Chunk chunk = null;
  protected STATE state = STATE.SEARCHING;

  protected @Inject
  LavaTeleport plugin;
  protected @Inject
  Settings settings;

  protected static HashSet<Material> dangerousMaterials = new HashSet<>();

  static {
    dangerousMaterials.add(Material.LAVA);
    dangerousMaterials.add(Material.WATER);
    dangerousMaterials.add(Material.FIRE);
    dangerousMaterials.add(Material.AIR);
    dangerousMaterials.add(Material.CAVE_AIR);
    dangerousMaterials.add(Material.VOID_AIR);
  }

  @AssistedInject
  public RandomTeleportTask(@Assisted Player player) {
    super(player, null);

    this.setCancelable(true);
    this.setOverwriteAllowed(false);
  }

  @Override
  public void run() {
    super.run();

    if (!player.isOnline()) {
      cleanup();
      return;
    }

    var playerData = teleportManager.getPlayerTeleportData(player);

    if (state == STATE.SEARCHING) {
      location = findRandomLocation();
      if (location == null) {
        player.sendMessage(t("rtp failed"));
        cleanup();
        return;
      }

      PaperLib.getChunkAtAsync(location, true).thenAccept(chunk -> {
        if (!player.isOnline()) {
          cleanup();
          return;
        }

        this.chunk = chunk;

        var block = location.getWorld().getHighestBlockAt(location.getBlockX(), location.getBlockZ());
        var y = block.getY();

        if (dangerousMaterials.contains(block.getType()) && player.getGameMode() != GameMode.SPECTATOR) {
          rerun(STATE.SEARCHING);
          return;
        }

        chunk.addPluginChunkTicket(plugin);

        player.sendBlockChange(block.getLocation(), block.getBlockData());

        location.setY(y + 3);
        location = LocationUtil.getRoundedDestination(location);

        rerun(STATE.TELEPORTING);
      });
    } else if (state == STATE.TELEPORTING) {
      PaperLib.teleportAsync(player, location).thenAccept(result -> {
        if (!player.isOnline()) {
          cleanup();
          return;
        }

        plugin.log(player, location, "rtp");

        player.sendMessage(
            t("rtp teleported", Map.of(
                "[x]", Integer.toString(location.getBlockX()),
                "[y]", Integer.toString(location.getBlockY()),
                "[z]", Integer.toString(location.getBlockZ())
                )
            )
        );
        playerData.setLastLocation(location);
        playerData.setLastTeleportTime(PlayerTeleportData.TELEPORT_KIND.RTP);
        cleanup();
      });
    }
  }

  @Override
  public void cancel(CancelReason reason) {
    super.cancel(reason);
  }

  void rerun(STATE state) {
    this.state = state;
    Bukkit.getScheduler().runTask(plugin, this);
  }

  void cleanup() {
    if (player.isOnline()) teleportManager.setPlayerTeleportTask(player, null);

    if (chunk != null) chunk.removePluginChunkTicket(plugin);
  }

  public Location findRandomLocation() {
    var world = player.getWorld();

    var border = world.getWorldBorder();
    var radius = (double) settings.getRtpRadius();
    if (radius == -1) radius = border.getSize() / 2;

    var center = border.getCenter();

    var minX = center.getX() - radius;
    var maxX = center.getX() + radius;

    var minZ = center.getZ() - radius;
    var maxZ = center.getZ() + radius;

    for (int i = 0; i < 16; ++i) {
      var x = ThreadLocalRandom.current().nextInt((int) minX, (int) maxX);
      var z = ThreadLocalRandom.current().nextInt((int) minZ, (int) maxZ);

      if (areThereAnyRegionsAtPlace(world, x, z)) continue;

      return new Location(world, x, 0, z);
    }

    return null;
  }

  public boolean areThereAnyRegionsAtPlace(World world, int x, int z) {
    var cx = (x >> 4) * 16;
    var cz = (z >> 4) * 16;

    var min = BlockVector3.at(cx, 0, cz);
    var max = BlockVector3.at(cx + 15, 255, cz + 15);

    var region = new ProtectedCuboidRegion(UUID.randomUUID().toString(), min, max);

    var manager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
    if (manager == null) return true;

    return manager.getApplicableRegions(region).size() > 0;
  }

  enum STATE {
    SEARCHING,
    TELEPORTING,
  }

  public interface Factory {
    RandomTeleportTask create(@Assisted Player player);
  }

  @Override
  public PlayerTeleportData.TELEPORT_KIND getTeleportKind() {
    return PlayerTeleportData.TELEPORT_KIND.RTP;
  }
}
