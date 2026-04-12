package com.ea.bfaq.mark;

import java.util.UUID;

public class MarkData
{
    private final UUID targetUUID;
    private final MarkType markType;
    private final boolean isFriendly;
    private final long timestamp;
    private static final long MARK_DURATION = 15000;

    public MarkData(UUID targetUUID, MarkType markType, boolean isFriendly)
    {
        this.targetUUID = targetUUID;
        this.markType = markType;
        this.isFriendly = isFriendly;
        this.timestamp = System.currentTimeMillis();
    }

    public UUID getTargetUUID()
    {
        return targetUUID;
    }

    public MarkType getMarkType()
    {
        return markType;
    }

    public boolean isFriendly()
    {
        return isFriendly;
    }

    public boolean isExpired()
    {
        return System.currentTimeMillis() - timestamp > MARK_DURATION;
    }

    public enum MarkType
    {
        ASSAULT("assault"),
        MEDIC("medic"),
        RECON("recon"),
        SUPPORT("support"),
        RAIDER("raider"),
        TANK("tank"),
        RIFLE("rifle"),
        PILOT("pilot"),
        FLAMETHROWER("flamethrower"),
        SENTRY("sentry"),
        TRENCH_FIGHTER("trench_fighter"),
        INVADER("invader");

        private final String textureName;

        MarkType(String textureName)
        {
            this.textureName = textureName;
        }

        public String getTextureName()
        {
            return textureName;
        }

        public static MarkType random()
        {
            MarkType[] types = {ASSAULT, MEDIC, RECON, SUPPORT};
            return types[(int) (Math.random() * types.length)];
        }
    }
}
