package win.lava.teleport;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import win.lava.teleport.tasks.RandomTeleportTask;
import win.lava.teleport.tasks.TeleportSafeAsyncTask;

public class Module extends AbstractModule {
  protected LavaTeleport plugin;

  public Module(LavaTeleport plugin) {
    this.plugin = plugin;
  }

  @Override
  protected void configure() {
    bind(LavaTeleport.class).toInstance(plugin);

    install(
        new FactoryModuleBuilder()
            .implement(Request.class, Request.class)
            .build(Request.Factory.class)
    );
    install(
        new FactoryModuleBuilder()
            .implement(RandomTeleportTask.class, RandomTeleportTask.class)
            .build(RandomTeleportTask.Factory.class)
    );
    install(
        new FactoryModuleBuilder()
            .implement(TeleportSafeAsyncTask.class, TeleportSafeAsyncTask.class)
            .build(TeleportSafeAsyncTask.Factory.class)
    );
  }
}
