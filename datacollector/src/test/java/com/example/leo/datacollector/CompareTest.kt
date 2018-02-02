package com.example.leo.datacollector

import com.example.leo.datacollector.compare.CompareActivity
import junit.framework.Assert
import org.junit.Test

/**
 * Created by Leo on 09.01.2018.
 */
class CompareTest{
    @Test
    fun testDiscretize(){
        val compareactivity = CompareActivity()
        Assert.assertEquals(16.0, compareactivity.discretize(2.1f))
        Assert.assertEquals(15.0, compareactivity.discretize(2f))
        Assert.assertEquals(15.0, compareactivity.discretize(1.81f))
        Assert.assertEquals(14.0, compareactivity.discretize(1.61f))
        Assert.assertEquals(13.0, compareactivity.discretize(1.41f))
        Assert.assertEquals(12.0, compareactivity.discretize(1.21f))
        Assert.assertEquals(11.0, compareactivity.discretize(1.01f))
        Assert.assertEquals(10.0, compareactivity.discretize(0.99f))
        Assert.assertEquals(9.0, compareactivity.discretize(0.81f))
        Assert.assertEquals(8.0, compareactivity.discretize(0.71f))
        Assert.assertEquals(7.0, compareactivity.discretize(0.61f))
        Assert.assertEquals(6.0, compareactivity.discretize(0.51f))
        Assert.assertEquals(5.0, compareactivity.discretize(0.499f))
        Assert.assertEquals(4.0, compareactivity.discretize(0.31f))
        Assert.assertEquals(3.0, compareactivity.discretize(0.21f))
        Assert.assertEquals(2.0, compareactivity.discretize(0.1f))
        Assert.assertEquals(1.0, compareactivity.discretize(0.000001f))
        Assert.assertEquals(0.0, compareactivity.discretize(0f))
        Assert.assertEquals(-16.0, compareactivity.discretize(-2.1f))
        Assert.assertEquals(-15.0, compareactivity.discretize(-1.81f))
        Assert.assertEquals(-14.0, compareactivity.discretize(-1.61f))
        Assert.assertEquals(-13.0, compareactivity.discretize(-1.41f))
        Assert.assertEquals(-12.0, compareactivity.discretize(-1.21f))
        Assert.assertEquals(-11.0, compareactivity.discretize(-1.01f))
        Assert.assertEquals(-10.0, compareactivity.discretize(-0.99f))
        Assert.assertEquals(-9.0, compareactivity.discretize(-0.81f))
        Assert.assertEquals(-8.0, compareactivity.discretize(-0.71f))
        Assert.assertEquals(-7.0, compareactivity.discretize(-0.61f))
        Assert.assertEquals(-6.0, compareactivity.discretize(-0.51f))
        Assert.assertEquals(-5.0, compareactivity.discretize(-0.5f))
        Assert.assertEquals(-4.0, compareactivity.discretize(-0.31f))
        Assert.assertEquals(-3.0, compareactivity.discretize(-0.21f))
        Assert.assertEquals(-2.0, compareactivity.discretize(-0.11f))
        Assert.assertEquals(-1.0, compareactivity.discretize(-0.000001f))
    }
}