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
        String ip = "", port = "", db = "", username = "sa", password = "";

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

    public void readLocation(String cve) {
        ConnectionSQL c = new ConnectionSQL();
        Connection connection = c.connDb();

        if (c != null) {
            try {
                String sqlStatement = "SELECT * FROM casillas";
                Statement smt = connection.createStatement();
                ResultSet set = smt.executeQuery(sqlStatement);
                while (set.next()) {

                }
                connection.close();
            }
            catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }
        }
    }
}
