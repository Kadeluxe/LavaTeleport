package win.lava.teleport.tasks;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import io.papermc.lib.PaperLib;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import win.lava.teleport.LavaTeleport;
import win.lava.teleport.LocationUtil;

import static win.lava.teleport.Lang.t;

public class TeleportSafeAsyncTask implements Runnable {
  protected @Inject
  LavaTeleport plugin;

  protected Player player;
  protected Location location;

  protected Chunk chunk = null;

  @AssistedInject
  public TeleportSafeAsyncTask(@Assisted Player player, @Assisted Location location) {
    this.player = player;
    this.location = location;
  }

  @Override
  public void run() {
    PaperLib.getChunkAtAsync(location, true).thenAccept(this::afterChunkLoaded);
  }

  Material getFillBlockForWorld(World world) {
    if (world.getEnvironment() == World.Environment.NORMAL) {
      return Material.STONE;
    }
    if (world.getEnvironment() == World.Environment.NETHER) {
      return Material.NETHERRACK;
    }
    if (world.getEnvironment() == World.Environment.THE_END) {
      return Material.END_STONE;
    }

    return Material.STONE;
  }

  public boolean isGroundBlockSafe(Block block) {
    if (LocationUtil.HOLLOW_MATERIALS.contains(block.getType())) {
      return false;
    }

    var faceBlock = block.getLocation().add(0, 2, 0).getBlock();
    return LocationUtil.HOLLOW_MATERIALS.contains(faceBlock.getType());
  }

  public void afterChunkLoaded(Chunk chunk) {
    if (!player.isOnline()) {
      cleanup();
      return;
    }

    this.chunk = chunk;
    chunk.addPluginChunkTicket(plugin);

    if (player.getGameMode() == GameMode.SPECTATOR) {
      teleport(location, true);
      return;
    }

    var y = location.getY();

    Block block = location.clone().subtract(0, 1, 0).getBlock();
    var wasSafe = true;
    var foundSafe = false;

    if (!LocationUtil.isLocationSafe(location)) {
      wasSafe = false;

      for (int i = -1; i <= 1; i++) {
        for (var it : LocationUtil.VECTOR_LIST) {
          var testVector = it.clone();
          testVector.setY(y + i);

          block = chunk.getBlock(testVector.getBlockX(), testVector.getBlockY(), testVector.getBlockZ());
          if (LocationUtil.isLocationSafe(block.getLocation())) {
            location = block.getLocation();
            foundSafe = true;
            break;
          }
        }
      }
    }

    if (!wasSafe && !foundSafe) {
      block = location.getWorld().getHighestBlockAt(location);
      location = block.getLocation();

      if (location.getWorld().getEnvironment() == World.Environment.NETHER && block.getType() == Material.BEDROCK) {
        var d1 = block.getRelative(BlockFace.DOWN);
        var d2 = d1.getRelative(BlockFace.DOWN);
        var d3 = d2.getRelative(BlockFace.DOWN);

        d1.setType(Material.AIR);
        d2.setType(Material.AIR);
        d3.setType(getFillBlockForWorld(location.getWorld()));

        block = d3;
        location = d3.getLocation();
      } else if (LocationUtil.DANGEROUS_BLOCKS.contains(block.getType()) || LocationUtil.HOLLOW_MATERIALS.contains(block.getType())) {
        block = block.getRelative(BlockFace.UP);
        block.setType(getFillBlockForWorld(location.getWorld()));
        location = block.getLocation();
      }
    }

    location.setY(block.getY() + 1);

    player.sendBlockChange(block.getLocation(), block.getBlockData());

    location.setX(location.getBlockX() + 0.5D);
    location.setZ(location.getBlockZ() + 0.5D);

    teleport(location, wasSafe);
  }

  public void teleport(Location location, boolean isSafe) {
    PaperLib.teleportAsync(player, location).thenAccept(this::afterTeleport).thenAccept((x) -> {
      plugin.log(player, location, "home");

      if (!isSafe) player.sendMessage(t("position unsafe"));
    });
  }

  public void afterTeleport(boolean result) {
    cleanup();
  }

  public void cleanup() {
    chunk.removePluginChunkTicket(plugin);
  }

  public interface Factory {
    TeleportSafeAsyncTask create(@Assisted Player player, @Assisted Location location);
  }
}
