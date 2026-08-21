import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * VIEWS + FUNCTION OUTCOMES for marketing analysis reports:
 *   - region performance (revenue, orders, ROI)
 *   - budget alerts (campaigns close to exhausting budget)
 *   - campaigns ending soon
 *   - top ads by ROAS
 *   - underperforming ads (CTR well below their campaign's average)
 *   - segment-platform affinity (who buys best, on which platform)
 */
public class ReportDAO {

    public List<String> regionPerformance() throws SQLException {
        return runSimple("SELECT * FROM vw_RegionPerformance ORDER BY TotalRevenue DESC");
    }

    public List<String> segmentPerformance() throws SQLException {
        return runSimple("SELECT * FROM vw_SegmentPerformance ORDER BY TotalRevenue DESC");
    }

    public List<String> platformPerformance() throws SQLException {
        return runSimple("SELECT * FROM vw_PlatformPerformance ORDER BY TotalRevenue DESC");
    }

    public List<String> productPerformance() throws SQLException {
        return runSimple("SELECT * FROM vw_ProductPerformance ORDER BY RevenueGenerated DESC");
    }

    public List<String> campaignBySegment(int campaignId) throws SQLException {
        String sql = "SELECT * FROM vw_CampaignBySegment WHERE CampaignID = ? ORDER BY Revenue DESC";
        List<String> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, campaignId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(String.format("Segment=%s (%s, %s) | Orders=%d | Revenue=%.2f",
                            rs.getString("SegmentName"), rs.getString("AgeGroup"), rs.getString("IncomeCategory"),
                            rs.getInt("Orders"), rs.getBigDecimal("Revenue")));
                }
            }
        }
        return rows;
    }

    // flags campaigns that are close to running out of budget so marketing can react. 
    public List<String> budgetAlerts(double thresholdPct) throws SQLException {
        String sql = "SELECT CampaignID, Objective, Budget, TotalSpend, BudgetUtilizationPct " +
                     "FROM vw_CampaignPerformance WHERE BudgetUtilizationPct >= ? " +
                     "ORDER BY BudgetUtilizationPct DESC";
        List<String> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, thresholdPct);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(String.format("CampaignID=%d | %s | Budget=%.2f | Spent=%.2f | Utilization=%s%%",
                            rs.getInt("CampaignID"), rs.getString("Objective"),
                            rs.getBigDecimal("Budget"), rs.getBigDecimal("TotalSpend"),
                            rs.getBigDecimal("BudgetUtilizationPct")));
                }
            }
        }
        return rows;
    }

    // Campaigns wrapping up soon, so marketing can plan renewals/follow-ups. 
    public List<String> campaignsEndingSoon(int withinDays) throws SQLException {
        String sql = "SELECT CampaignID, Objective, EndDate, DATEDIFF(EndDate, CURDATE()) AS DaysLeft " +
                     "FROM Campaign WHERE EndDate >= CURDATE() AND DATEDIFF(EndDate, CURDATE()) <= ? " +
                     "ORDER BY DaysLeft ASC";
        List<String> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, withinDays);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(String.format("CampaignID=%d | %s | Ends=%s | DaysLeft=%d",
                            rs.getInt("CampaignID"), rs.getString("Objective"),
                            rs.getDate("EndDate"), rs.getInt("DaysLeft")));
                }
            }
        }
        return rows;
    }

    // Best ads by return-on-ad-spend, across all campaigns. 
    public List<String> topAdsByROAS(int limit) throws SQLException {
        String sql = "SELECT AdID, AdName, CampaignID, ROAS, CostPerAd, AttributedRevenue " +
                     "FROM vw_AdPerformance WHERE ROAS IS NOT NULL ORDER BY ROAS DESC LIMIT ?";
        List<String> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(String.format("AdID=%d | %s | CampaignID=%d | ROAS=%s | Spend=%.2f | Revenue=%.2f",
                            rs.getInt("AdID"), rs.getString("AdName"), rs.getInt("CampaignID"),
                            rs.getBigDecimal("ROAS"), rs.getBigDecimal("CostPerAd"),
                            rs.getBigDecimal("AttributedRevenue")));
                }
            }
        }
        return rows;
    }

    // Ads whose CTR is less than half their own campaign's average CTR - candidates to pause/rework.
    public List<String> underperformingAds() throws SQLException {
        String sql =
            "SELECT ap.AdID, ap.AdName, ap.CampaignID, ap.CTR, camp_avg.avg_ctr " +
            "FROM vw_AdPerformance ap " +
            "JOIN (SELECT CampaignID, AVG(CTR) AS avg_ctr FROM vw_AdPerformance GROUP BY CampaignID) camp_avg " +
            "  ON camp_avg.CampaignID = ap.CampaignID " +
            "WHERE ap.CTR < (camp_avg.avg_ctr * 0.5) " +
            "ORDER BY ap.CTR ASC";
        return runSimple(sql, rs -> String.format(
                "AdID=%d | %s | CampaignID=%d | CTR=%s%% (campaign avg %.4f%%)",
                get(rs, "AdID"), rsGetString(rs, "AdName"), get(rs, "CampaignID"),
                rsGetBigDecimal(rs, "CTR"), rs.getDouble("avg_ctr")));
    }

    // Which customer segment responds best on which platform - guides targeting/spend allocation.
    public List<String> segmentPlatformAffinity() throws SQLException {
        String sql =
            "SELECT cs.SegmentName, pl.PlatformName, " +
            "       COUNT(DISTINCT o.OrderID) AS Orders, IFNULL(SUM(o.TotalAmount),0) AS Revenue " +
            "FROM Orders o " +
            "JOIN Customer cu ON cu.CustomerID = o.CustomerID " +
            "JOIN CustomerSegment cs ON cs.SegmentID = cu.SegmentID " +
            "JOIN Campaign c ON c.CampaignID = o.CampaignID " +
            "JOIN Platform pl ON pl.PlatformID = c.PlatformID " +
            "GROUP BY cs.SegmentName, pl.PlatformName " +
            "ORDER BY Revenue DESC";
        List<String> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(String.format("Segment=%s | Platform=%s | Orders=%d | Revenue=%.2f",
                        rs.getString("SegmentName"), rs.getString("PlatformName"),
                        rs.getInt("Orders"), rs.getBigDecimal("Revenue")));
            }
        }
        return rows;
    }

    // helpers to reduce boilerplate for simple queries that return a list of strings, one per row. The first version just concatenates all columns, the second allows a custom formatter.

    private List<String> runSimple(String sql) throws SQLException {
        List<String> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            while (rs.next()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i <= cols; i++) {
                    sb.append(meta.getColumnLabel(i)).append('=').append(rs.getString(i));
                    if (i < cols) sb.append(" | ");
                }
                rows.add(sb.toString());
            }
        }
        return rows;
    }

    private interface RowFormatter { String format(ResultSet rs) throws SQLException; }

    private List<String> runSimple(String sql, RowFormatter formatter) throws SQLException {
        List<String> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(formatter.format(rs));
            }
        }
        return rows;
    }

    private int get(ResultSet rs, String col) {
        try { return rs.getInt(col); } catch (SQLException e) { return 0; }
    }
    private String rsGetString(ResultSet rs, String col) {
        try { return rs.getString(col); } catch (SQLException e) { return ""; }
    }
    private java.math.BigDecimal rsGetBigDecimal(ResultSet rs, String col) {
        try { return rs.getBigDecimal(col); } catch (SQLException e) { return java.math.BigDecimal.ZERO; }
    }
}
