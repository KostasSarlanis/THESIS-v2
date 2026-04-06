package com.thesisv2;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class DBConnection {
    public Connection connection;
    public Connection getConnection() {

        try {
            Properties props = AppConfig.load();

            String host = props.getProperty("db.host");
            String port = props.getProperty("db.port");
            String dbName = props.getProperty("db.name");
            String dbUser = props.getProperty("db.user");
            String dbPw = props.getProperty("db.password");

            if (host == null || port == null || dbName == null || dbUser == null || dbPw == null) {
                throw new RuntimeException("Δεν βρέθηκαν αποθηκευμένες ρυθμίσεις βάσης.");
            }

            String url = "jdbc:mariadb://" + host + ":" + port + "/" + dbName;

            connection = DriverManager.getConnection(url, dbUser, dbPw);
            System.out.println("Connection to database succesfully!");

        } catch (Exception e) {
            System.out.println("Error to connection to database!");
            throw new RuntimeException(e);
        }

        return connection;
    }

}
