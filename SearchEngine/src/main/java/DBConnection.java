import org.springframework.beans.factory.annotation.Value;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection
{
    private static Connection connection;
    private static String dbName = "search_engine";
    private static String dbUser = "root";
    private static String dbPass = "q1w2e3r4t5A1";

    public static Connection getConnection()
    {

        if (connection == null){
            try {
                connection = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/" + dbName +
                                "?user=" + dbUser + "&password=" + dbPass + "&createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true");
                connection.createStatement().execute("DROP TABLE IF EXISTS page");
                connection.createStatement().execute("CREATE TABLE page(" +
                        "id INT NOT NULL AUTO_INCREMENT, " +
                        "path TEXT NOT NULL, " +
                        "code INT NOT NULL, " +
                        "content MEDIUMTEXT NOT NULL," +
                        "PRIMARY KEY(id), KEY(path(50))," +
                        "UNIQUE KEY name_date(path(50)))");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return connection;
    }
}
