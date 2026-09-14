import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class QueryDb2 {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/mindflow_db", "postgres", "Nikitha@03");
            Statement stmt = conn.createStatement();
            
            ResultSet rs = stmt.executeQuery("SELECT model_name FROM document_chunk_embeddings WHERE chunk_id IN (SELECT id FROM document_chunks WHERE document_id IN (SELECT id FROM documents WHERE user_id = 11))");
            if (rs.next()) {
                System.out.println("Model name in DB: " + rs.getString(1));
            } else {
                System.out.println("No embeddings found.");
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
