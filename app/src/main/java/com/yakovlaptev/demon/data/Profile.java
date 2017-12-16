package com.yakovlaptev.demon.data;

import android.annotation.SuppressLint;

import java.io.Serializable;

public class Profile implements Serializable {

    private String name, email, avatar;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("name:%s   email:%s   email:%s", name, email, avatar);
    }
}
