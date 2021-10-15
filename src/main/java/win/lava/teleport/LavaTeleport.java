package win.lava.teleport;

import co.aikar.commands.PaperCommandManager;
import com.earth2me.essentials.spawn.EssentialsSpawn;
import com.google.inject.Guice;
import com.google.inject.Injector;
import fr.xephi.authme.api.v3.AuthMeApi;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import win.lava.chat.LavaChat;
import win.lava.common.LavaCommon;
import win.lava.passive.LavaPassive;
import win.lava.teleport.commands.*;
import win.lava.teleport.entities.QuitPositionEntity;
import win.lava.teleport.listeners.AreaListener;
import win.lava.teleport.listeners.PlayerListener;

import java.io.File;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Callable;

public class LavaTeleport extends JavaPlugin {
  static YamlConfiguration lang;
  static int TPS = 20;
  private static LavaTeleport instance;
  private static LuckPerms lp;
  private static LavaCommon lavaCommon;
  private static LavaChat lavaChat;
  private static LavaPassive lavaPassive;
  private EssentialsSpawn essentialsSpawn;
  private YamlConfiguration config;
  private Settings settings;
  private TeleportManager teleportManager;
  private Injector injector;

  @Override
  public void onEnable() {
    instance = this;

    try {
      injector = Guice.createInjector(new Module(this));

      settings = injector.getInstance(Settings.class);
      teleportManager = injector.getInstance(TeleportManager.class);

      reload();
      getDependencies();
      registerCommands();
      registerEvents();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    LocationUtil.cacheMaterials();

    try {
      Database.setup();
    } catch (SQLException e) {
      e.printStackTrace();
    }

    handleReload();

    Bukkit.getScheduler().runTaskTimer(this, () -> {
      teleportManager.cleanup();
    }, TPS * 300, TPS * 300);
  }

  private void handleReload() {
    for (var it : Bukkit.getOnlinePlayers()) {
      teleportManager.onPlayerJoin(new PlayerJoinEvent(it, ""));
    }
  }

  private void registerEvents() {
    getServer().getPluginManager().registerEvents(injector.getInstance(TeleportManager.class), this);
    getServer().getPluginManager().registerEvents(injector.getInstance(CallManager.class), this);
    getServer().getPluginManager().registerEvents(injector.getInstance(PlayerListener.class), this);
    getServer().getPluginManager().registerEvents(injector.getInstance(AreaListener.class), this);
  }

  private void getDependencies() {
    essentialsSpawn = (EssentialsSpawn) Objects.requireNonNull(getServer().getPluginManager().getPlugin("EssentialsSpawn"));
    lavaCommon = (LavaCommon) Objects.requireNonNull(getServer().getPluginManager().getPlugin("LavaCommon"));
    lavaPassive = (LavaPassive) Objects.requireNonNull(getServer().getPluginManager().getPlugin("LavaPassive"));
    lavaChat = (LavaChat) Objects.requireNonNull(getServer().getPluginManager().getPlugin("LavaChat"));

    var provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
    if (provider != null) {
      lp = provider.getProvider();
    }
  }

  private void registerCommands() {
    var manager = new PaperCommandManager(this);

    manager.registerCommand(injector.getInstance(Ask.class));
    manager.registerCommand(injector.getInstance(Accept.class));
    manager.registerCommand(injector.getInstance(Deny.class));
    manager.registerCommand(injector.getInstance(Cancel.class));
    manager.registerCommand(injector.getInstance(Home.class));
    manager.registerCommand(injector.getInstance(HomeAdmin.class));
    manager.registerCommand(injector.getInstance(Rtp.class));
    manager.registerCommand(injector.getInstance(Reload.class));
    manager.registerCommand(injector.getInstance(Spawn.class));
    manager.registerCommand(injector.getInstance(Warp.class));
    manager.registerCommand(injector.getInstance(WarpAdmin.class));
    manager.registerCommand(injector.getInstance(ATeleport.class));

    var locale = new Locale("ru", "RU");
    manager.getLocales().setDefaultLocale(locale);

    manager.getCommandCompletions().registerAsyncCompletion("playersExcludingSender", Completions::playersExcludingSender);
    manager.getCommandCompletions().registerAsyncCompletion("playerHomes", Completions::playerHomes);
    manager.getCommandCompletions().registerAsyncCompletion("warps", Completions::warps);
  }

  @Override
  public void onDisable() {
    try {
      for (var player : Bukkit.getOnlinePlayers()) {
        if (!AuthMeApi.getInstance().isAuthenticated(player)) continue;

        var e = Database.getQuitPositionEntityDao().queryBuilder().where().eq("uuid", player.getUniqueId().toString()).queryForFirst();
        if (e == null) e = new QuitPositionEntity();

        var location = player.getLocation();
        e.setUuid(player.getUniqueId().toString());
        e.setWorld(location.getWorld().getName());
        e.setX(location.getX());
        e.setY(Math.ceil(location.getY()));
        e.setZ(location.getZ());
        e.setPitch(location.getPitch());
        e.setYaw(location.getYaw());

        e.setVelX(location.getX());
        e.setVelY(location.getY());
        e.setVelZ(location.getZ());

        e.setGliding(player.isGliding());

        Database.getQuitPositionEntityDao().createOrUpdate(e);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    Database.close();
  }

  public void reload() {
    try {
      {
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
          file.getParentFile().mkdirs();
          saveResource("config.yml", false);
        }

        config = new YamlConfiguration();
        config.load(file);

        settings.reload(config);
      }
      {
        File file = new File(getDataFolder(), "lang.yml");
        if (!file.exists()) {
          file.getParentFile().mkdirs();
          saveResource("lang.yml", false);
        }

        lang = new YamlConfiguration();
        lang.load(file);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void log(Player player, Location location, String reason) {
    var otherSymbols = new DecimalFormatSymbols(Locale.getDefault());
    otherSymbols.setDecimalSeparator('.');
    otherSymbols.setGroupingSeparator('\'');

    var df = new DecimalFormat("#.##", otherSymbols);

    getLogger().info("Teleported " + player.getName() + " to " + df.format(location.getX()) + " " + df.format(location.getY()) + " " + df.format(location.getZ()) + " " + location.getWorld().getName() + " (" + reason + ")");
  }

  public TeleportManager getTeleportManager() {
    return teleportManager;
  }

  public Injector getInjector() {
    return injector;
  }

  public EssentialsSpawn getEssentialsSpawn() {
    return essentialsSpawn;
  }

  public Settings getSettings() {
    return settings;
  }

  public static LavaPassive getLavaPassive() {
    return lavaPassive;
  }

  public static LavaChat getLavaChat() {
    return lavaChat;
  }

  public static YamlConfiguration getLanguageConfig() {
    return lang;
  }

  public static LuckPerms getLuckPerms() {
    return lp;
  }
}
