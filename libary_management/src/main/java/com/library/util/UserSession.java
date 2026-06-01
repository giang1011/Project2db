package com.library.util;

import com.library.model.User;

public class UserSession {
    private static UserSession instance;
    private User loggedInUser;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
    }

    public User getLoggedInUser() {
        return this.loggedInUser;
    }

    public void clearSession() {
        this.loggedInUser = null;
    }
}
