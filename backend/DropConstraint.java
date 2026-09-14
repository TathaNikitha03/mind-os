import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DropConstraint {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/mindflow_db", "postgres", "Nikitha@03");
            Statement stmt = conn.createStatement();
            stmt.execute("ALTER TABLE documents DROP CONSTRAINT IF EXISTS documents_status_check");
            stmt.execute("ALTER TABLE documents DROP CONSTRAINT IF EXISTS chk_document_status");
            System.out.println("Constraints dropped successfully!");
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
