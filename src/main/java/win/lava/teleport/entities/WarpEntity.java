package win.lava.teleport.entities;

import win.lava.common._deps_.ormlite.field.DatabaseField;
import win.lava.common._deps_.ormlite.table.DatabaseTable;

import java.sql.Timestamp;

@DatabaseTable(tableName = "warps")
public class WarpEntity {
  @DatabaseField(generatedId = true) private int id;
  @DatabaseField private String name;
  @DatabaseField private String world;
  @DatabaseField private double x;
  @DatabaseField private double y;
  @DatabaseField private double z;
  @DatabaseField private float yaw;
  @DatabaseField private float pitch;

  @DatabaseField(readOnly = true, canBeNull = false) private Timestamp createdAt;
  @DatabaseField(readOnly = true, canBeNull = false) private Timestamp updatedAt;

  public WarpEntity() {

  }

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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
