package win.lava.teleport;

public enum CancelReason {
    EXPIRED,
    MOVEMENT,
    DAMAGE,
    SRC_CANCEL,
    SRC_LEAVE,
    DEST_DENY,
    DEST_LEAVE,
    ANOTHER_REQUEST,
    OVERRIDE,
    CANNOT_OVERRIDE
}
