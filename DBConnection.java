import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// Central place to obtain a JDBC connection. Update URL/USER/PASSWORD for your environment, or better, load them
public class DBConnection {

    private static final String URL =
            "jdbc:mysql://localhost:3306/marketing_db?useSSL=false&serverTimezone=UTC";
    private static final String USER = "your_db_user";
    private static final String PASSWORD = "your_db_password";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
