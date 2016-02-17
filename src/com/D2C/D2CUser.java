package com.D2C;

/**
 * Created by sbobba on 1/5/2016.
 */
public class D2CUser {

    private String userName = null;
    private String password = null;

    public D2CUser(String userName, String password)
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
