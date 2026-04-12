package com.ea.bfaq.client.animation;

import com.ea.bfaq.BF1Q;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

import java.util.Map;
import java.util.TreeMap;

public class AnimationController
{
    private static AnimationController instance;
    
    private AnimationData animationData;
    private float animationTime;
    private boolean isPlaying;
    private long lastUpdateTime;
    
    private AnimationController()
    {
        loadAnimationData();
        animationTime = 0;
        isPlaying = false;
        lastUpdateTime = System.currentTimeMillis();
    }
    
    public static AnimationController getInstance()
    {
        if (instance == null)
        {
            instance = new AnimationController();
        }
        return instance;
    }
    
    private void loadAnimationData()
    {
        ResourceLocation location = new ResourceLocation(BF1Q.MODID, "mark.animation.json");
        animationData = AnimationData.load(Minecraft.getInstance().getResourceManager(), location);
        
        if (animationData == null)
        {
            System.err.println("Failed to load animation data from " + location);
        }
    }
    
    public void playMarkEnemyAnimation()
    {
        animationTime = 0;
        isPlaying = true;
        lastUpdateTime = System.currentTimeMillis();
    }
    
    public void update()
    {
        if (!isPlaying || animationData == null)
        {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
        lastUpdateTime = currentTime;
        
        animationTime += deltaTime;
        
        AnimationData.Animation animation = animationData.getMarkEnemyAnimation();
        if (animation != null && animationTime >= animation.getAnimationLength())
        {
            isPlaying = false;
            animationTime = 0;
        }
    }
    
    public Vector3f getLeftArmRotation()
    {
        if (!isPlaying || animationData == null)
        {
            return new Vector3f(0, 0, 0);
        }
        
        AnimationData.Animation animation = animationData.getMarkEnemyAnimation();
        if (animation == null || animation.getBones() == null)
        {
            return new Vector3f(0, 0, 0);
        }
        
        AnimationData.BoneAnimation leftArm = animation.getBones().get("Left Arm");
        if (leftArm == null || leftArm.getRotation() == null)
        {
            return new Vector3f(0, 0, 0);
        }
        
        return interpolateKeyFrames(leftArm.getRotation(), animationTime);
    }
    
    public Vector3f getLeftArmPosition()
    {
        if (!isPlaying || animationData == null)
        {
            return new Vector3f(0, 0, 0);
        }
        
        AnimationData.Animation animation = animationData.getMarkEnemyAnimation();
        if (animation == null || animation.getBones() == null)
        {
            return new Vector3f(0, 0, 0);
        }
        
        AnimationData.BoneAnimation leftArm = animation.getBones().get("Left Arm");
        if (leftArm == null || leftArm.getPosition() == null)
        {
            return new Vector3f(0, 0, 0);
        }
        
        return interpolateKeyFrames(leftArm.getPosition(), animationTime);
    }
    
    private Vector3f interpolateKeyFrames(Map<String, AnimationData.VectorKeyframe> keyFrames, float time)
    {
        if (keyFrames == null || keyFrames.isEmpty())
        {
            return new Vector3f(0, 0, 0);
        }
        
        TreeMap<Float, Vector3f> sortedFrames = new TreeMap<>();
        for (Map.Entry<String, AnimationData.VectorKeyframe> entry : keyFrames.entrySet())
        {
            try
            {
                float keyTime = Float.parseFloat(entry.getKey());
                Vector3f value = entry.getValue().toVector3f();
                sortedFrames.put(keyTime, value);
            }
            catch (NumberFormatException e)
            {
                e.printStackTrace();
            }
        }
        
        if (sortedFrames.isEmpty())
        {
            return new Vector3f(0, 0, 0);
        }
        
        Float lowerKey = sortedFrames.floorKey(time);
        Float higherKey = sortedFrames.ceilingKey(time);
        
        if (lowerKey == null && higherKey != null)
        {
            return sortedFrames.get(higherKey);
        }
        
        if (higherKey == null && lowerKey != null)
        {
            return sortedFrames.get(lowerKey);
        }
        
        if (lowerKey == null)
        {
            return new Vector3f(0, 0, 0);
        }
        
        if (lowerKey.equals(higherKey))
        {
            return sortedFrames.get(lowerKey);
        }
        
        Vector3f lowerValue = sortedFrames.get(lowerKey);
        Vector3f higherValue = sortedFrames.get(higherKey);
        
        float blend = (time - lowerKey) / (higherKey - lowerKey);
        
        float x = lerp(lowerValue.x(), higherValue.x(), blend);
        float y = lerp(lowerValue.y(), higherValue.y(), blend);
        float z = lerp(lowerValue.z(), higherValue.z(), blend);
        
        return new Vector3f(x, y, z);
    }
    
    private float lerp(float a, float b, float t)
    {
        return a + (b - a) * t;
    }
    
    public boolean isPlaying()
    {
        return isPlaying;
    }
    
    public float getAnimationTime()
    {
        return animationTime;
    }
    
    public float getAnimationLength()
    {
        if (animationData == null)
        {
            return 2.7917f;
        }
        AnimationData.Animation animation = animationData.getMarkEnemyAnimation();
        if (animation == null)
        {
            return 2.7917f;
        }
        return animation.getAnimationLength();
    }
}
