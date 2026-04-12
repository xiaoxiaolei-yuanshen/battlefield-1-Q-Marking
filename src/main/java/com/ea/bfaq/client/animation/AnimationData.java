package com.ea.bfaq.client.animation;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Vector3f;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class AnimationData
{
    @SerializedName("format_version")
    private String formatVersion;
    
    private Map<String, Animation> animations;
    
    public static class Animation
    {
        @SerializedName("animation_length")
        private float animationLength;
        
        private Map<String, BoneAnimation> bones;
        
        public float getAnimationLength()
        {
            return animationLength;
        }
        
        public Map<String, BoneAnimation> getBones()
        {
            return bones;
        }
    }
    
    public static class BoneAnimation
    {
        private Map<String, VectorKeyframe> rotation;
        private Map<String, VectorKeyframe> position;
        private Map<String, VectorKeyframe> scale;
        
        public Map<String, VectorKeyframe> getRotation()
        {
            return rotation;
        }
        
        public Map<String, VectorKeyframe> getPosition()
        {
            return position;
        }
        
        public Map<String, VectorKeyframe> getScale()
        {
            return scale;
        }
    }
    
    public static class VectorKeyframe
    {
        private float[] vector;
        
        public Vector3f toVector3f()
        {
            if (vector == null || vector.length < 3)
            {
                return new Vector3f(0, 0, 0);
            }
            return new Vector3f(vector[0], vector[1], vector[2]);
        }
    }
    
    public Map<String, Animation> getAnimations()
    {
        return animations;
    }
    
    public Animation getMarkEnemyAnimation()
    {
        if (animations == null)
        {
            return null;
        }
        return animations.get("mark_enemy");
    }
    
    public static AnimationData load(ResourceManager resourceManager, ResourceLocation location)
    {
        try
        {
            Resource resource = resourceManager.getResource(location).orElse(null);
            if (resource == null)
            {
                return null;
            }
            Gson gson = new Gson();
            return gson.fromJson(new InputStreamReader(resource.open()), AnimationData.class);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
