package win.lava.teleport.entities;

import win.lava.common._deps_.ormlite.field.DatabaseField;
import win.lava.common._deps_.ormlite.table.DatabaseTable;

import java.sql.Timestamp;

@DatabaseTable(tableName = "positions_quit")
public class QuitPositionEntity {
  @DatabaseField(generatedId = true) private Integer id;
  @DatabaseField private String uuid;
  @DatabaseField private String world;
  @DatabaseField private double x;
  @DatabaseField private double y;
  @DatabaseField private double z;
  @DatabaseField private float yaw;
  @DatabaseField private float pitch;
  @DatabaseField private double velX;
  @DatabaseField private double velY;
  @DatabaseField private double velZ;
  @DatabaseField private boolean isGliding;
  @DatabaseField(readOnly = true, canBeNull = false) private Timestamp createdAt;
  @DatabaseField(readOnly = true, canBeNull = false) private Timestamp updatedAt;

  public QuitPositionEntity() {

  }

  public boolean isGliding() {
    return isGliding;
  }

  public void setGliding(boolean gliding) {
    isGliding = gliding;
  }

  public double getVelX() {
    return velX;
  }

  public void setVelX(double velX) {
    this.velX = velX;
  }

  public double getVelY() {
    return velY;
  }

  public void setVelY(double velY) {
    this.velY = velY;
  }

  public double getVelZ() {
    return velZ;
  }

  public void setVelZ(double velZ) {
    this.velZ = velZ;
  }

  public Integer getId() {
    return id;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getWorld() {
    return world;
  }

  public void setWorld(String world) {
    this.world = world;
  }

  public double getX() {
    return x;
  }

  public void setX(double x) {
    this.x = x;
  }

  public double getY() {
    return y;
  }

  public void setY(double y) {
    this.y = y;
  }

  public double getZ() {
    return z;
  }

  public void setZ(double z) {
    this.z = z;
  }

  public float getYaw() {
    return yaw;
  }

  public void setYaw(float yaw) {
    this.yaw = yaw;
  }

  public float getPitch() {
    return pitch;
  }

  public void setPitch(float pitch) {
    this.pitch = pitch;
  }

  public Timestamp getCreatedAt() {
    return createdAt;
  }

  public Timestamp getUpdatedAt() {
    return updatedAt;
  }
}
