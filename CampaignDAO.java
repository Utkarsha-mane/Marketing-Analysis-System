import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CampaignDAO {

    // Marketing team creates a new campaign. CurrentBudget always starts at 0. 
    public int createCampaign(int platformId, LocalDate startDate, LocalDate endDate,
                               BigDecimal budget, String objective) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            int newId = getNextCampaignId(conn);

            String sql = "INSERT INTO Campaign " +
                    "(CampaignID, PlatformID, StartDate, EndDate, Budget, CurrentBudget, Objective) " +
                    "VALUES (?, ?, ?, ?, ?, 0, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, newId);
                ps.setInt(2, platformId);
                ps.setDate(3, Date.valueOf(startDate));
                ps.setDate(4, Date.valueOf(endDate));
                ps.setBigDecimal(5, budget);
                ps.setString(6, objective);
                ps.executeUpdate();
            }
            return newId;
        }
    }

    public void updateCampaignBudget(int campaignId, BigDecimal newBudget) throws SQLException {
        String sql = "UPDATE Campaign SET Budget = ? WHERE CampaignID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBudget);
            ps.setInt(2, campaignId);
            ps.executeUpdate();
        }
    }

    public List<String> listCampaigns() throws SQLException {
        String sql = "SELECT CampaignID, PlatformID, StartDate, EndDate, Budget, CurrentBudget, Objective " +
                     "FROM Campaign ORDER BY CampaignID";
        List<String> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(String.format(
                        "CampaignID=%d | PlatformID=%d | %s to %s | Budget=%.2f | Spent=%.2f | Objective=%s",
                        rs.getInt("CampaignID"), rs.getInt("PlatformID"),
                        rs.getDate("StartDate"), rs.getDate("EndDate"),
                        rs.getBigDecimal("Budget"), rs.getBigDecimal("CurrentBudget"),
                        rs.getString("Objective")));
            }
        }
        return rows;
    }

    // Pulls from vw_CampaignPerformance so ROI/spend/CTR are already computed.
    public List<String> getCampaignPerformance() throws SQLException {
        String sql = "SELECT * FROM vw_CampaignPerformance ORDER BY ROIPercent DESC";
        List<String> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(String.format(
                        "CampaignID=%d | %s | Platform=%s | Budget=%.2f | Spend=%.2f (%.1f%%) | Revenue=%.2f | ROI=%s%% | Ads=%d",
                        rs.getInt("CampaignID"), rs.getString("Objective"), rs.getString("PlatformName"),
                        rs.getBigDecimal("Budget"), rs.getBigDecimal("TotalSpend"),
                        rs.getBigDecimal("BudgetUtilizationPct"), rs.getBigDecimal("ActualOrderRevenue"),
                        rs.getBigDecimal("ROIPercent"), rs.getInt("NumAds")));
            }
        }
        return rows;
    }

    private int getNextCampaignId(Connection conn) throws SQLException {
        String sql = "SELECT IFNULL(MAX(CampaignID), 0) + 1 AS nextId FROM Campaign";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt("nextId") : 1;
        }
    }
}
