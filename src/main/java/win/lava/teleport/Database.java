package win.lava.teleport;

import win.lava.common._deps_.ormlite.dao.Dao;
import win.lava.common._deps_.ormlite.dao.DaoManager;
import win.lava.common._deps_.ormlite.db.MysqlDatabaseType;
import win.lava.common._deps_.ormlite.jdbc.DataSourceConnectionSource;
import win.lava.common._deps_.ormlite.support.ConnectionSource;
import win.lava.common.LavaCommon;
import win.lava.teleport.entities.PositionEntity;
import win.lava.teleport.entities.QuitPositionEntity;
import win.lava.teleport.entities.WarpEntity;

import java.sql.SQLException;

public class Database {
  private static ConnectionSource connectionSource;
  private static Dao<PositionEntity, Integer> positionEntityDao;
  private static Dao<QuitPositionEntity, Integer> quitPositionEntityDao;
  private static Dao<WarpEntity, Integer> warpEntityDao;

  public static void setup() throws SQLException {
    connectionSource = createConnection();
    positionEntityDao = DaoManager.createDao(connectionSource, PositionEntity.class);
    quitPositionEntityDao = DaoManager.createDao(connectionSource, QuitPositionEntity.class);
    warpEntityDao = DaoManager.createDao(connectionSource, WarpEntity.class);
  }

  public static void close() {
    connectionSource.closeQuietly();
  }

  public static ConnectionSource createConnection() throws SQLException {
    return new DataSourceConnectionSource(LavaCommon.getDataSourceLocal(), new MysqlDatabaseType());
  }

  public static ConnectionSource getConnectionSource() {
    return connectionSource;
  }

  public static Dao<WarpEntity, Integer> getWarpEntityDao() {
    return warpEntityDao;
  }

  public static Dao<QuitPositionEntity, Integer> getQuitPositionEntityDao() {
    return quitPositionEntityDao;
  }

  public static Dao<PositionEntity, Integer> getPositionEntityDao() {
    return positionEntityDao;
  }
}
