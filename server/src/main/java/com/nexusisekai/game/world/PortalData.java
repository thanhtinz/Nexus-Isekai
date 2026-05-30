package com.nexusisekai.game.world;

/**
 * Dữ liệu một portal (cổng dịch chuyển) trên map
 */
public class PortalData {

    private final int   portalId;
    private final int   sourceMapId;
    private final int   targetMapId;
    private final float destX;
    private final float destY;
    private final int   requiredLevel;

    public PortalData(int portalId, int sourceMapId, int targetMapId,
                      float destX, float destY, int requiredLevel) {
        this.portalId      = portalId;
        this.sourceMapId   = sourceMapId;
        this.targetMapId   = targetMapId;
        this.destX         = destX;
        this.destY         = destY;
        this.requiredLevel = requiredLevel;
    }

    public int   getPortalId()      { return portalId; }
    public int   getSourceMapId()   { return sourceMapId; }
    public int   getTargetMapId()   { return targetMapId; }
    public float getDestX()         { return destX; }
    public float getDestY()         { return destY; }
    public int   getRequiredLevel() { return requiredLevel; }
}
