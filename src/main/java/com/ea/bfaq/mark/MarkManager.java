package com.ea.bfaq.mark;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MarkManager
{
    private static final MarkManager INSTANCE = new MarkManager();
    private final Map<UUID, MarkData> marks = new ConcurrentHashMap<>();
    private final Map<UUID, MarkData.MarkType> savedMarkTypes = new ConcurrentHashMap<>();

    public static MarkManager getInstance()
    {
        return INSTANCE;
    }

    public void addMark(UUID targetUUID, MarkData.MarkType markType, boolean isFriendly)
    {
        MarkData.MarkType finalType = markType;
        
        if (savedMarkTypes.containsKey(targetUUID))
        {
            finalType = savedMarkTypes.get(targetUUID);
        }
        else
        {
            savedMarkTypes.put(targetUUID, finalType);
        }
        
        marks.put(targetUUID, new MarkData(targetUUID, finalType, isFriendly));
    }

    public void removeMark(UUID targetUUID)
    {
        marks.remove(targetUUID);
    }

    public MarkData getMark(UUID targetUUID)
    {
        MarkData mark = marks.get(targetUUID);
        if (mark != null && mark.isExpired())
        {
            marks.remove(targetUUID);
            return null;
        }
        return mark;
    }

    public boolean hasActiveMark(UUID targetUUID)
    {
        return getMark(targetUUID) != null;
    }

    public Collection<MarkData> getAllMarks()
    {
        marks.entrySet().removeIf(entry -> entry.getValue().isExpired());
        return marks.values();
    }

    public Map<UUID, MarkData> getAllMarksMap()
    {
        marks.entrySet().removeIf(entry -> entry.getValue().isExpired());
        return marks;
    }

    public void clear()
    {
        marks.clear();
    }
    
    public void clearAll()
    {
        marks.clear();
        savedMarkTypes.clear();
    }
}