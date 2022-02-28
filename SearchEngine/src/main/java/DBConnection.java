import java.sql.*;

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
                connection.createStatement().execute("DROP TABLE IF EXISTS field");
                connection.createStatement().execute("DROP TABLE IF EXISTS lemma");
                connection.createStatement().execute("DROP TABLE IF EXISTS aindex");

                connection.createStatement().execute("CREATE TABLE page(" +
                        "id INT NOT NULL AUTO_INCREMENT, " +
                        "path TEXT NOT NULL, " +
                        "code INT NOT NULL, " +
                        "content MEDIUMTEXT NOT NULL," +
                        "PRIMARY KEY(id), KEY(path(50))," +
                        "UNIQUE KEY name_date(path(50)))");
                connection.createStatement().execute("CREATE TABLE field(" +
                        "id INT NOT NULL AUTO_INCREMENT, " +
                        "name VARCHAR(255) NOT NULL, " +
                        "selector VARCHAR(255) NOT NULL, " +
                        "weight FLOAT NOT NULL," +
                        "PRIMARY KEY(id))");
                connection.createStatement().execute("CREATE TABLE lemma(" +
                        "id INT NOT NULL AUTO_INCREMENT, " +
                        "lemma VARCHAR(255) NOT NULL, " +
                        "frequency INT NOT NULL, " +
                        "PRIMARY KEY(id))");
                connection.createStatement().execute("CREATE TABLE aindex(" +
                        "id INT NOT NULL AUTO_INCREMENT, " +
                        "page_id INT NOT NULL, " +
                        "lemma_id INT NOT NULL, " +
                        "arank FLOAT NOT NULL, " +
                        "PRIMARY KEY(id) )");

                defaultField(connection);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return connection;
    }

    public static void defaultField(Connection connection) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement("INSERT IGNORE INTO field(name, selector,weight) " +
                "VALUES('title','title','1.0'),('body','body','0.8')");
        preparedStatement.executeUpdate();
    }
}
