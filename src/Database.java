import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Central JDBC connection factory; environment variables override local defaults. */
public final class Database {
    private static final Properties LOCAL_CONFIG = loadLocalConfig();
    private static final String URL = setting("FLAPPY_DB_URL", "url", "jdbc:mysql://127.0.0.1:3306/flappy_bird");
    private static final String USER = setting("FLAPPY_DB_USER", "user", "root");
    private static final String PASSWORD = setting("FLAPPY_DB_PASSWORD", "password", "");

    private Database() { }

    private static Properties loadLocalConfig() {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream("flappy-db.properties")) {
            properties.load(input);
        } catch (IOException error) {
            System.err.println("Database configuration was not loaded from flappy-db.properties: "
                    + error.getMessage());
        }
        return properties;
    }

    private static String setting(String environmentName, String propertyName, String fallback) {
        String value = System.getenv(environmentName);
        if (value == null || value.trim().isEmpty()) value = LOCAL_CONFIG.getProperty(propertyName);
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    /** Opens a new short-lived connection; callers close it with try-with-resources. */
    public static Connection getConnection() throws SQLException {
        ensureDriverLoaded();
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private static void ensureDriverLoaded() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException error) {
            throw new SQLException("MySQL Connector/J driver was not found. Keep mysql-connector-j-9.4.0.jar in lib/ and ensure VS Code references lib/**/*.jar.", error);
        }
    }
}
