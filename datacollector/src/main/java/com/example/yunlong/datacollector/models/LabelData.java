package com.example.yunlong.datacollector.models;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseUser;

/**
 * Created by Yunlong on 5/10/2016.
 */
@ParseClassName("LabelData")
public class LabelData extends ParseObject {

    public LabelData() {
    }

    public String getTitle() {
        return getString("title");
    }

    public void setTitle(String title) {
        put("title", title);
    }

    public void setAuthor(ParseUser user) {
        put("author", user);
    }

    public void setUserName(String userName) {
        put("userName", userName);
    }

    public void setTime(String time){
        put("time",time);
    }
    public void setActivityLabel(String activityLabel){
        put("activityLabel",activityLabel);
    }

    public void setType(String type){
        put("type",type);
    }

    public void setMood(int mood){
        put("mood",mood);
    }

    public void setIsTypicalRoutine(String isTypicalRoutine){
        put("isTypicalRoutine",isTypicalRoutine);
    }

}
