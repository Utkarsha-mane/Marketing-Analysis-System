import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdDAO {

    /*
     * Creates a new ad. trg_CheckCampaignBudget_Insert trigger will
     * reject this (SQLSTATE 45000) if it would exceed the campaign budget, automatically on success.
     */
    public int createAd(int campaignId, String adName, int impressions, int clicks,
                         BigDecimal costPerAd, String[] reasonOut) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            int newId = getNextAdId(conn);

            String sql = "INSERT INTO Ad " +
                    "(AdID, CampaignID, AdName, Impressions, Clicks, ConversionRate, CostPerAd, AttributedRevenue) " +
                    "VALUES (?, ?, ?, ?, ?, 0, ?, 0)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, newId);
                ps.setInt(2, campaignId);
                ps.setString(3, adName);
                ps.setInt(4, impressions);
                ps.setInt(5, clicks);
                ps.setBigDecimal(6, costPerAd);
                ps.executeUpdate();
            } catch (SQLException e) {
                if ("45000".equals(e.getSQLState())) {
                    reasonOut[0] = e.getMessage(); // business rule message from SIGNAL
                    return -1;
                }
                throw e;
            }
            return newId;
        }
    }

    // Update impressions/clicks (e.g. a daily sync) then recompute ConversionRate/AttributedRevenue. 
    public void updateAdMetrics(int adId, int impressions, int clicks) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "UPDATE Ad SET Impressions = ?, Clicks = ? WHERE AdID = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, impressions);
                ps.setInt(2, clicks);
                ps.setInt(3, adId);
                ps.executeUpdate();
            }
            try (CallableStatement cs = conn.prepareCall("{call sp_RecalcAdMetrics(?)}")) {
                cs.setInt(1, adId);
                cs.execute();
            }
        }
    }

    public List<String> getAdPerformance(Integer campaignIdOrNull) throws SQLException {
        String sql = "SELECT * FROM vw_AdPerformance" +
                (campaignIdOrNull != null ? " WHERE CampaignID = ?" : "") +
                " ORDER BY ROAS DESC";
        List<String> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (campaignIdOrNull != null) {
                ps.setInt(1, campaignIdOrNull);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(String.format(
                            "AdID=%d | %s | CampaignID=%d | CTR=%s%% | ConvRate=%s | CPC=%s | ROAS=%s | Revenue=%.2f",
                            rs.getInt("AdID"), rs.getString("AdName"), rs.getInt("CampaignID"),
                            rs.getBigDecimal("CTR"), rs.getBigDecimal("ConversionRate"),
                            rs.getBigDecimal("CostPerClick"), rs.getBigDecimal("ROAS"),
                            rs.getBigDecimal("AttributedRevenue")));
                }
            }
        }
        return rows;
    }

    private int getNextAdId(Connection conn) throws SQLException {
        String sql = "SELECT IFNULL(MAX(AdID), 0) + 1 AS nextId FROM Ad";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt("nextId") : 1;
        }
    }
}
