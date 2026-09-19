import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class QueryDb {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/mindflow_db", "postgres", "Nikitha@03");
            Statement stmt = conn.createStatement();
            
            ResultSet rs = stmt.executeQuery("SELECT id, name, email, password_hash FROM users WHERE id = 4");
            if (rs.next()) {
                System.out.println("User 4: " + rs.getString("email") + " | pw: " + rs.getString("password_hash"));
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
