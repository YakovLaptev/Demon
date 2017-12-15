package com.yakovlaptev.demon.data;

import android.annotation.SuppressLint;

import java.io.Serializable;

public class Profile implements Serializable {

    private String name, email;

    public Profile() {}

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("name:%s   email:%s", name, email);
    }
}
