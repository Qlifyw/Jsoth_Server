package sample.sqlite;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import sample.sqlite.model.Device;
import sample.utils.NetUtils;

import java.io.File;
import java.sql.*;

public class DbHelper {

    public final static String dbName = "Device.db";
    public final static String IP_ADDRESS = NetUtils.getExternalIP();
    //emotpublic final static String IP_ADDRESS = "192.168.1.6";
    public final static int PORT = 4157;

    private final static Logger log = LogManager.getRootLogger();

    public static void main(String[] args) {

    }

    public static void createDeviceTable(String dbName) {
        String url = "jdbc:sqlite:" + System.getProperty("user.dir") + File.separator + dbName;

        try (Connection connection = DriverManager.getConnection(url)){
            if (connection != null ){
                String sql = "CREATE TABLE IF NOT EXISTS Devices (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "hwid TEXT NOT NULL," +
                        "model TEXT," +
                        "os_version TEXT," +
                        "api INTEGER," +
                        "key TEXT," +
                        "last_connection TEXT) ";
                Statement statement = connection.createStatement();
                statement.execute(sql);

            }
        } catch (SQLException e) {
            log.error("createDeviceTable: DB connection : | " + e.toString());
        }
    }

    public static void checkDeviceTableExists(Connection connection, String dbName) throws SQLException{
        String tableName = "Devices";
        DatabaseMetaData dbmd = connection.getMetaData();
        ResultSet tables =  dbmd.getTables(null, null, tableName, null);
        if (!tables.next()) {
            createDeviceTable(dbName);
        }
    }

    public static boolean checkRowByHWID(String dbName, String hwid) {
        String url = "jdbc:sqlite:" + System.getProperty("user.dir") + File.separator + dbName;
        String sql = "SELECT * FROM Devices WHERE hwid = ? AND key IS NOT NULL  LIMIT 1";
        boolean result = false;
        try (Connection connection = DriverManager.getConnection(url)){
            if (connection != null ){
                checkDeviceTableExists(connection, dbName);
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, hwid);
                ResultSet rs = preparedStatement.executeQuery();
                result = rs.next();
            }
        } catch (SQLException e) {
            log.error("CheckRowByHWID: DB connection : | " + e.toString());
        }
        return result;
    }

    public static void insertHWID(String dbName, String hwid) {
        String url = "jdbc:sqlite:" + System.getProperty("user.dir") + File.separator + dbName;
        String sql = "INSERT INTO Devices (hwid) VALUES(?) ";

        try (Connection connection = DriverManager.getConnection(url)){
            if (connection != null ){
                checkDeviceTableExists(connection, dbName);
                if (!checkRowByHWID(dbName, hwid)) {
                    PreparedStatement preparedStatement = connection.prepareStatement(sql);
                    preparedStatement.setString(1, hwid);
                    preparedStatement.executeUpdate();
                }

            }
        } catch (SQLException e) {
            log.error("insertHWID: DB connection : | " + e.toString());
        }
    }

    public static void updateKey(String dbName, String key, String hwid) {
        String url = "jdbc:sqlite:" + System.getProperty("user.dir") + File.separator + dbName;
        String sql = "UPDATE Devices SET key = ? WHERE hwid = ?";

        try (Connection connection = DriverManager.getConnection(url)){
            if (connection != null ){
                checkDeviceTableExists(connection, dbName);
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, key);
                preparedStatement.setString(2, hwid);
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            log.error("updateKey: DB connection : | " + e.toString());
        }
    }


    public static void insertDeviceInfo(String dbName, String model, String version, int api, String hwid) {
        String url = "jdbc:sqlite:" + System.getProperty("user.dir") + File.separator + dbName;
        String sql = "UPDATE Devices SET model = ?, os_version = ?, api = ?, last_connection = datetime('now', 'localtime') WHERE hwid = ?";

        try (Connection connection = DriverManager.getConnection(url)){
            if (connection != null ){
                checkDeviceTableExists(connection, dbName);
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, model);
                preparedStatement.setString(2, version);
                preparedStatement.setInt(3, api);
                preparedStatement.setString(4, hwid);
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            log.error("insertDeviceInfo: DB connection : | " + e.toString());
        }
    }

    public static void updateLastConnection(String dbName, String hwid) {
        String url = "jdbc:sqlite:" + System.getProperty("user.dir") + File.separator + dbName;
        String sql = "UPDATE Devices SET last_connection = datetime('now', 'localtime') WHERE hwid = ?";

        try (Connection connection = DriverManager.getConnection(url)){
            if (connection != null ){
                checkDeviceTableExists(connection, dbName);
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, hwid);
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            log.error("updateLastConnection: DB connection : | " + e.toString());
        }
    }

    public static ObservableList<Device> selectAll(String dbName) {
        String url = "jdbc:sqlite:" + System.getProperty("user.dir") + File.separator + dbName;
        String sql = "SELECT * FROM Devices";
        ObservableList<Device> userData = FXCollections.observableArrayList();

        try (Connection connection = DriverManager.getConnection(url)){
            if (connection != null ){
                checkDeviceTableExists(connection, dbName);
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(sql);

                while(rs.next()) {
                    userData.add(new Device(rs.getString("hwid"), rs.getString("model"),
                            rs.getString("os_version"), rs.getInt("api"),
                            rs.getString("last_connection")));
                }

            }
        } catch (SQLException e) {
            log.error("selectAll: DB connection : | " + e.toString() + "\n" + url);
        }
        return userData;
    }

    public static void delete(String dbName) {
        String url = "jdbc:sqlite:" + System.getProperty("user.dir") + File.separator + dbName;

        try (Connection connection = DriverManager.getConnection(url)){
            if (connection != null ){

            }
        } catch (SQLException e) {
            log.error("delete: DB connection : | " + e.toString());
        }
    }

    public static String getKey(String dbName, String hwid) {
        String url = "jdbc:sqlite:" + System.getProperty("user.dir") + File.separator + dbName;
        String sql = "SELECT key FROM Devices WHERE hwid = ?";
        String key = "";

        try (Connection connection = DriverManager.getConnection(url)){
            if (connection != null ){
                PreparedStatement pstmt = connection.prepareStatement(sql);
                pstmt.setString(1, hwid);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    key = rs.getString("key");
                }
            }
        } catch (SQLException e) {
            log.error("getKey: DB connection : | " + e.toString());
        }
        return key;
    }

}
