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

    @SuppressLint("NewApi")
    public Connection connDb() {
        String ip = "192.168.1.244", port = "1433", db = "app_casillas", username = "sa", password = "Juanp123";

        StrictMode.ThreadPolicy a = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(a);
        String connectURL = null;
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            connectURL = "jdbc:jtds:sqlserver://" + ip + ":" + port + ";" + "databasename=" + db + ";user=" + username + ";" + "password=" + password + ";";
            con = DriverManager.getConnection(connectURL);

        }
        catch (Exception e) {
            Log.d("Error: " , e.getMessage());
        }

        return con;
    }
}
