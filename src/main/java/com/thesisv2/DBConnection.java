package com.thesisv2;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    //MariaDB connection objects
    public Connection connection;
    public Connection getConnection(){

//        //the legacy way
//        String dbName = "thesisdb";
//        String dbUser = "root";
//        String dbPw = "admin123";
//        String url = "jdbc:mariadb://192.168.1.120:3306/" + dbName;

        try {
            //Connection line
//            //the legacy way
//            Class.forName("org.mariadb.jdbc.Driver");
//            connection = DriverManager.getConnection(url, dbUser, dbPw);

            connection = DriverManager.getConnection("jdbc:mariadb://192.168.1.160:3306/thesisdb?user=root&password=admin123");
            System.out.println("Connection to database succesfully!");
        } catch (Exception e) {
            //error just in case
            System.out.println("Error to connection to database!");
            throw new RuntimeException(e);
        }

        return connection;
    }

}
