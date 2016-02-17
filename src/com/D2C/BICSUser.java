package com.D2C;

/**
 * Created by sbobba on 1/7/2016.
 */
public class BICSUser {
    private String userName = null;
    private String password = null;

    public BICSUser(String userName, String password)
    {
        this.userName = userName;
        this.password = password;
    }

    public String getUserName()
    {
        return userName;
    }


    public String getPassword()
    {
        return password;
    }
}
