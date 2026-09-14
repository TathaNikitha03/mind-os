import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class QueryDb {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/mindflow_db", "postgres", "Nikitha@03");
            Statement stmt = conn.createStatement();
            
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM documents WHERE user_id = 11");
            rs.next();
            System.out.println("Documents for user 11: " + rs.getInt(1));
            
            rs = stmt.executeQuery("SELECT count(*) FROM document_chunks dc JOIN documents d ON d.id = dc.document_id WHERE d.user_id = 11");
            rs.next();
            System.out.println("Chunks for user 11: " + rs.getInt(1));

            rs = stmt.executeQuery("SELECT count(*) FROM document_chunk_embeddings dce JOIN document_chunks dc ON dc.id = dce.chunk_id JOIN documents d ON d.id = dc.document_id WHERE d.user_id = 11");
            rs.next();
            System.out.println("Embeddings for user 11: " + rs.getInt(1));
            
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
