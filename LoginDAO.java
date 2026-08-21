import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// Very simple authentication gate so this dashboard cannot be opened by a "general user" as required. Requires the MarketingUser table
// Methodology: The password is hashed using SHA-256 and compared to the stored hash in the database. Fast and sufficient for this project no extensive computation is required.
public class LoginDAO {

    public boolean login(String username, String plainPassword) throws SQLException {
        String hash = sha256(plainPassword);

        String sql = "SELECT PasswordHash FROM MarketingUser WHERE Username = ? AND Role = 'MARKETING'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false; // no such marketing user
                }
                String storedHash = rs.getString("PasswordHash");
                return storedHash.equals(hash);
            }
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }
}
