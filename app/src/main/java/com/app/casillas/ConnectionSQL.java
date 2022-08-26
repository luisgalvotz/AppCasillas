package com.app.casillas;

import android.annotation.SuppressLint;
import android.os.StrictMode;
import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class ConnectionSQL {
    Connection con;

    public Connection connDb() {
        String url = "jdbc:mysql://162.241.62.141:3306/disenow5_db_Casillas";
        String username = "disenow5_casilla", password = "cocodrilo1";

        try {
            Class.forName("com.mysql.jdbc.Driver");
            con = DriverManager.getConnection(url, username, password);

        }
        catch (Exception e) {
            Log.d("Error: " , e.getMessage());
        }

        return con;
    }
}
