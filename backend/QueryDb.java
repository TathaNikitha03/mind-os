import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class QueryDb {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/mindflow_db", "postgres", "Nikitha@03");
            Statement stmt = conn.createStatement();
            
            ResultSet rs = stmt.executeQuery("SELECT id, name, email, created_at FROM users ORDER BY id DESC LIMIT 20");
            while (rs.next()) {
                System.out.println("User ID: " + rs.getInt("id") + " | Name: " + rs.getString("name") + " | Email/Mobile: " + rs.getString("email") + " | Created: " + rs.getTimestamp("created_at"));
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
