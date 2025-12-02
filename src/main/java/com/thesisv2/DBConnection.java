package com.thesisv2;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    //MariaDB connection objects
    public Connection connection;
    public Connection getConnection(){
        try {
            //Connection line
            connection = DriverManager.getConnection("jdbc:mariadb://192.168.1.120:3306/DB?user=root&password=admin123");
            System.out.println("Connection to database succesfully!");
        } catch (SQLException e) {
            //error just in case
            System.out.println("Error to connection to database!");
            throw new RuntimeException(e);
        }

        return connection;
    }

}
