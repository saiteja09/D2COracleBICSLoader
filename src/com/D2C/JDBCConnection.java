package com.D2C;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Created by sbobba on 1/6/2016.
 */
public class JDBCConnection {
    private  D2CUser D2Cuser = null;
    private String DB = null;
    private Connection con = null;

    public JDBCConnection(D2CUser d2CUser, String DB)
    {
        this.D2Cuser = d2CUser;
        this.DB = DB;
    }

    public Connection CreateConnection()
    {

        try{
            con = DriverManager.getConnection("jdbc:datadirect:ddcloud://service.datadirectcloud.com;databaseName=" + DB,D2Cuser.getUserName(), D2Cuser.getPassword());
        }catch(Exception e){
            System.out.println(Constants.ANSI_RED + "Exception while creating JDBC connection." + Constants.ANSI_RESET);
            System.exit(-1);
        }
        return con;
    }



}
