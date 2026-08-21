import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CampaignProductDAO {

    // Trigger trg_CampaignProduct_SellingPrice_Insert computes SellingPrice from MRP + Discount. 
    public void addProductToCampaign(int campaignId, int productId, BigDecimal discountPct) throws SQLException {
        String sql = "INSERT INTO CampaignProduct (CampaignID, ProductID, Discount, SellingPrice) " +
                     "VALUES (?, ?, ?, NULL)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, campaignId);
            ps.setInt(2, productId);
            ps.setBigDecimal(3, discountPct);
            ps.executeUpdate();
        }
    }

    public void updateDiscount(int campaignId, int productId, BigDecimal newDiscountPct) throws SQLException {
        String sql = "UPDATE CampaignProduct SET Discount = ? WHERE CampaignID = ? AND ProductID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newDiscountPct);
            ps.setInt(2, campaignId);
            ps.setInt(3, productId);
            ps.executeUpdate();
        }
    }

    public List<String> listCampaignProducts(int campaignId) throws SQLException {
        String sql = "SELECT cp.ProductID, p.ProductName, p.MRP, cp.Discount, cp.SellingPrice " +
                     "FROM CampaignProduct cp JOIN Product p ON p.ProductID = cp.ProductID " +
                     "WHERE cp.CampaignID = ? ORDER BY p.ProductName";
        List<String> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, campaignId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(String.format(
                            "ProductID=%d | %s | MRP=%.2f | Discount=%s%% | SellingPrice=%.2f",
                            rs.getInt("ProductID"), rs.getString("ProductName"),
                            rs.getBigDecimal("MRP"), rs.getBigDecimal("Discount"),
                            rs.getBigDecimal("SellingPrice")));
                }
            }
        }
        return rows;
    }
}
