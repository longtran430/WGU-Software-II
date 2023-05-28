package helper;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Contains methods for connecting to database
 *
 * @author Long Tran
 */
public abstract class JDBC {
    private static final String protocol = "jdbc";
    private static final String vendor = ":mysql:";
    private static final String location = "//localhost/";
    private static final String databaseName = "client_schedule";
    private static final String jdbUrl = protocol + vendor + location + databaseName + "?connectionTimeZone = SERVER";
    private static final String driver = "com.mysql.cj.jdbc.Driver";
    private static final String userName = "sqlUser";
    private static final String password = "Passw0rd!";
    public static Connection connection;

    /**
     * The method for connecting to database.
     */
    public static void openConnection(){
        try{
           Class.forName(driver);
           connection = DriverManager.getConnection(jdbUrl, userName, password);
           System.out.println("Connection Successful!");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /**
     * The method for disconnecting from database.
     */
    public static void closeConnection(){
        try{
            connection.close();
            System.out.println("Connection Closed!");
        } catch(Exception e){
            System.out.println("Error: " + e.getMessage());
        }
    }
}
