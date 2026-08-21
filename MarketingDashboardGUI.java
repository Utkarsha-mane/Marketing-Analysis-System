import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/*
 Frontend for the existing JDBC marketing dashboard.
 DAO classes : (CampaignDAO, AdDAO, CampaignProductDAO, ReportDAO, LoginDAO) 
*/
public class MarketingDashboardGUI {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { /* fall back to default L&F */ }
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }


    // Login
    static class LoginFrame extends JFrame {
        LoginFrame() {
            super("Marketing Dashboard - Login");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setResizable(false);

            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 6, 6, 6);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JLabel title = new JLabel("Marketing Team Login");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
            panel.add(title, gbc);
            gbc.gridwidth = 1;

            gbc.gridx = 0; gbc.gridy = 1;
            panel.add(new JLabel("Username:"), gbc);
            JTextField userField = new JTextField(16);
            gbc.gridx = 1;
            panel.add(userField, gbc);

            gbc.gridx = 0; gbc.gridy = 2;
            panel.add(new JLabel("Password:"), gbc);
            JPasswordField passField = new JPasswordField(16);
            gbc.gridx = 1;
            panel.add(passField, gbc);

            JLabel status = new JLabel(" ");
            status.setForeground(Color.RED);
            gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
            panel.add(status, gbc);

            JButton loginBtn = new JButton("Login");
            gbc.gridy = 4;
            panel.add(loginBtn, gbc);

            Runnable attemptLogin = () -> {
                String user = userField.getText().trim();
                String pass = new String(passField.getPassword());
                if (user.isEmpty() || pass.isEmpty()) {
                    status.setText("Enter username and password.");
                    return;
                }
                try {
                    boolean ok = new LoginDAO().login(user, pass);
                    if (ok) {
                        dispose();
                        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
                    } else {
                        status.setText("Access denied. Marketing team only.");
                        passField.setText("");
                    }
                } catch (SQLException ex) {
                    status.setText("DB error: " + ex.getMessage());
                }
            };
            loginBtn.addActionListener(e -> attemptLogin.run());
            passField.addActionListener(e -> attemptLogin.run());

            getContentPane().add(panel);
            pack();
            setLocationRelativeTo(null);
        }
    }

    // Main window
    static class MainFrame extends JFrame {
        MainFrame() {
            super("Marketing Dashboard");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(1000, 650);
            setLocationRelativeTo(null);

            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Campaigns", new CampaignPanel());
            tabs.addTab("Ads", new AdPanel());
            tabs.addTab("Campaign Products", new CampaignProductPanel());
            tabs.addTab("Reports", new ReportsPanel());

            getContentPane().add(tabs);
        }
    }


    // Shared helpers

    // Adds a right-aligned label + text field pair to a GridBagLayout form and returns the field. 
    private static JTextField addField(JPanel panel, GridBagConstraints gbc, int row, String label) {
        gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.EAST; gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);
        JTextField tf = new JTextField(14);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; gbc.weightx = 1;
        panel.add(tf, gbc);
        return tf;
    }

    // Renders "Key=Value | Key=Value | ..." formatted rows (as produced by the DAOs) into a JTable. 
    private static void populateTable(JTable table, List<String> rows) {
        if (rows == null || rows.isEmpty()) {
            DefaultTableModel model = new DefaultTableModel(new Object[]{"Result"}, 0);
            model.addRow(new Object[]{"(no rows)"});
            table.setModel(model);
            return;
        }
        String[] headerParts = rows.get(0).split("\\|");
        String[] headers = new String[headerParts.length];
        for (int i = 0; i < headerParts.length; i++) {
            String p = headerParts[i].trim();
            int eq = p.indexOf('=');
            headers[i] = eq >= 0 ? p.substring(0, eq).trim() : ("Col" + (i + 1));
        }
        DefaultTableModel model = new DefaultTableModel(headers, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (String row : rows) {
            String[] parts = row.split("\\|");
            Object[] values = new Object[headers.length];
            for (int i = 0; i < values.length; i++) {
                if (i < parts.length) {
                    String p = parts[i].trim();
                    int eq = p.indexOf('=');
                    values[i] = eq >= 0 ? p.substring(eq + 1).trim() : p;
                } else {
                    values[i] = "";
                }
            }
            model.addRow(values);
        }
        table.setModel(model);
    }

    private static void error(Component parent, Exception ex) {
        JOptionPane.showMessageDialog(parent, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static void info(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Done", JOptionPane.INFORMATION_MESSAGE);
    }


    // Campaigns tab
    static class CampaignPanel extends JPanel {
        private final CampaignDAO campaignDAO = new CampaignDAO();
        private final JTable table = new JTable();

        CampaignPanel() {
            setLayout(new BorderLayout(8, 8));
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 4, 4, 4);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JLabel createTitle = new JLabel("Create Campaign");
            createTitle.setFont(createTitle.getFont().deriveFont(Font.BOLD));
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
            form.add(createTitle, gbc);
            gbc.gridwidth = 1;

            JTextField platformId = addField(form, gbc, 1, "Platform ID:");
            JTextField startDate = addField(form, gbc, 2, "Start date (YYYY-MM-DD):");
            JTextField endDate = addField(form, gbc, 3, "End date (YYYY-MM-DD):");
            JTextField budget = addField(form, gbc, 4, "Budget:");
            JTextField objective = addField(form, gbc, 5, "Objective:");

            JButton createBtn = new JButton("Create Campaign");
            gbc.gridx = 1; gbc.gridy = 6; gbc.anchor = GridBagConstraints.EAST;
            form.add(createBtn, gbc);

            createBtn.addActionListener((ActionEvent e) -> {
                try {
                    int pId = Integer.parseInt(platformId.getText().trim());
                    LocalDate s = LocalDate.parse(startDate.getText().trim());
                    LocalDate en = LocalDate.parse(endDate.getText().trim());
                    BigDecimal b = new BigDecimal(budget.getText().trim());
                    int newId = campaignDAO.createCampaign(pId, s, en, b, objective.getText().trim());
                    info(this, "Created CampaignID = " + newId);
                    populateTable(table, campaignDAO.listCampaigns());
                } catch (Exception ex) {
                    error(this, ex);
                }
            });

            JSeparator sep1 = new JSeparator();
            gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
            form.add(sep1, gbc);
            gbc.gridwidth = 1;

            JLabel updateTitle = new JLabel("Update Campaign Budget");
            updateTitle.setFont(updateTitle.getFont().deriveFont(Font.BOLD));
            gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2;
            form.add(updateTitle, gbc);
            gbc.gridwidth = 1;

            JTextField updId = addField(form, gbc, 9, "Campaign ID:");
            JTextField updBudget = addField(form, gbc, 10, "New budget:");

            JButton updateBtn = new JButton("Update Budget");
            gbc.gridx = 1; gbc.gridy = 11; gbc.anchor = GridBagConstraints.EAST;
            form.add(updateBtn, gbc);

            updateBtn.addActionListener(e -> {
                try {
                    int id = Integer.parseInt(updId.getText().trim());
                    BigDecimal b = new BigDecimal(updBudget.getText().trim());
                    campaignDAO.updateCampaignBudget(id, b);
                    info(this, "Budget updated.");
                    populateTable(table, campaignDAO.listCampaigns());
                } catch (Exception ex) {
                    error(this, ex);
                }
            });

            JPanel buttonsRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton listBtn = new JButton("List Campaigns");
            JButton perfBtn = new JButton("Campaign Performance (ROI/Spend/CTR)");
            listBtn.addActionListener(e -> {
                try { populateTable(table, campaignDAO.listCampaigns()); }
                catch (Exception ex) { error(this, ex); }
            });
            perfBtn.addActionListener(e -> {
                try { populateTable(table, campaignDAO.getCampaignPerformance()); }
                catch (Exception ex) { error(this, ex); }
            });
            buttonsRow.add(listBtn);
            buttonsRow.add(perfBtn);

            JPanel top = new JPanel(new BorderLayout());
            top.add(form, BorderLayout.NORTH);
            top.add(buttonsRow, BorderLayout.SOUTH);

            add(top, BorderLayout.NORTH);
            add(new JScrollPane(table), BorderLayout.CENTER);
        }
    }


    // Ads tab

    static class AdPanel extends JPanel {
        private final AdDAO adDAO = new AdDAO();
        private final JTable table = new JTable();

        AdPanel() {
            setLayout(new BorderLayout(8, 8));
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 4, 4, 4);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JLabel createTitle = new JLabel("Create Ad");
            createTitle.setFont(createTitle.getFont().deriveFont(Font.BOLD));
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
            form.add(createTitle, gbc);
            gbc.gridwidth = 1;

            JTextField campaignId = addField(form, gbc, 1, "Campaign ID:");
            JTextField adName = addField(form, gbc, 2, "Ad name:");
            JTextField impressions = addField(form, gbc, 3, "Impressions:");
            JTextField clicks = addField(form, gbc, 4, "Clicks:");
            JTextField cost = addField(form, gbc, 5, "Cost per ad:");

            JButton createBtn = new JButton("Create Ad");
            gbc.gridx = 1; gbc.gridy = 6; gbc.anchor = GridBagConstraints.EAST;
            form.add(createBtn, gbc);

            createBtn.addActionListener(e -> {
                try {
                    int cId = Integer.parseInt(campaignId.getText().trim());
                    int imp = Integer.parseInt(impressions.getText().trim());
                    int clk = Integer.parseInt(clicks.getText().trim());
                    BigDecimal cst = new BigDecimal(cost.getText().trim());
                    String[] reason = new String[1];
                    int newId = adDAO.createAd(cId, adName.getText().trim(), imp, clk, cst, reason);
                    if (newId == -1) {
                        JOptionPane.showMessageDialog(this, "Could not create ad: " + reason[0],
                                "Business rule", JOptionPane.WARNING_MESSAGE);
                    } else {
                        info(this, "Created AdID = " + newId);
                        populateTable(table, adDAO.getAdPerformance(null));
                    }
                } catch (Exception ex) {
                    error(this, ex);
                }
            });

            JSeparator sep1 = new JSeparator();
            gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
            form.add(sep1, gbc);
            gbc.gridwidth = 1;

            JLabel updTitle = new JLabel("Update Ad Impressions/Clicks");
            updTitle.setFont(updTitle.getFont().deriveFont(Font.BOLD));
            gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2;
            form.add(updTitle, gbc);
            gbc.gridwidth = 1;

            JTextField updAdId = addField(form, gbc, 9, "Ad ID:");
            JTextField updImp = addField(form, gbc, 10, "New impressions:");
            JTextField updClk = addField(form, gbc, 11, "New clicks:");

            JButton updateBtn = new JButton("Update Metrics");
            gbc.gridx = 1; gbc.gridy = 12; gbc.anchor = GridBagConstraints.EAST;
            form.add(updateBtn, gbc);

            updateBtn.addActionListener(e -> {
                try {
                    int adId = Integer.parseInt(updAdId.getText().trim());
                    int imp = Integer.parseInt(updImp.getText().trim());
                    int clk = Integer.parseInt(updClk.getText().trim());
                    adDAO.updateAdMetrics(adId, imp, clk);
                    info(this, "Ad metrics updated and recalculated.");
                    populateTable(table, adDAO.getAdPerformance(null));
                } catch (Exception ex) {
                    error(this, ex);
                }
            });

            JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
            filterRow.add(new JLabel("Filter by Campaign ID (blank = all):"));
            JTextField filterField = new JTextField(6);
            filterRow.add(filterField);
            JButton perfBtn = new JButton("Ad Performance");
            filterRow.add(perfBtn);

            perfBtn.addActionListener(e -> {
                try {
                    String input = filterField.getText().trim();
                    Integer cId = input.isEmpty() ? null : Integer.parseInt(input);
                    populateTable(table, adDAO.getAdPerformance(cId));
                } catch (Exception ex) {
                    error(this, ex);
                }
            });

            JPanel top = new JPanel(new BorderLayout());
            top.add(form, BorderLayout.NORTH);
            top.add(filterRow, BorderLayout.SOUTH);

            add(top, BorderLayout.NORTH);
            add(new JScrollPane(table), BorderLayout.CENTER);
        }
    }


    // Campaign products tab
    static class CampaignProductPanel extends JPanel {
        private final CampaignProductDAO cpDAO = new CampaignProductDAO();
        private final JTable table = new JTable();

        CampaignProductPanel() {
            setLayout(new BorderLayout(8, 8));
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 4, 4, 4);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JLabel addTitle = new JLabel("Add Product to Campaign");
            addTitle.setFont(addTitle.getFont().deriveFont(Font.BOLD));
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
            form.add(addTitle, gbc);
            gbc.gridwidth = 1;

            JTextField addCampaignId = addField(form, gbc, 1, "Campaign ID:");
            JTextField addProductId = addField(form, gbc, 2, "Product ID:");
            JTextField addDiscount = addField(form, gbc, 3, "Discount %:");

            JButton addBtn = new JButton("Add Product");
            gbc.gridx = 1; gbc.gridy = 4; gbc.anchor = GridBagConstraints.EAST;
            form.add(addBtn, gbc);

            addBtn.addActionListener(e -> {
                try {
                    int cId = Integer.parseInt(addCampaignId.getText().trim());
                    int pId = Integer.parseInt(addProductId.getText().trim());
                    BigDecimal disc = new BigDecimal(addDiscount.getText().trim());
                    cpDAO.addProductToCampaign(cId, pId, disc);
                    info(this, "Product added (selling price computed by trigger).");
                    populateTable(table, cpDAO.listCampaignProducts(cId));
                } catch (Exception ex) {
                    error(this, ex);
                }
            });

            JSeparator sep1 = new JSeparator();
            gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
            form.add(sep1, gbc);
            gbc.gridwidth = 1;

            JLabel updTitle = new JLabel("Update Discount");
            updTitle.setFont(updTitle.getFont().deriveFont(Font.BOLD));
            gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
            form.add(updTitle, gbc);
            gbc.gridwidth = 1;

            JTextField updCampaignId = addField(form, gbc, 7, "Campaign ID:");
            JTextField updProductId = addField(form, gbc, 8, "Product ID:");
            JTextField updDiscount = addField(form, gbc, 9, "New discount %:");

            JButton updBtn = new JButton("Update Discount");
            gbc.gridx = 1; gbc.gridy = 10; gbc.anchor = GridBagConstraints.EAST;
            form.add(updBtn, gbc);

            updBtn.addActionListener(e -> {
                try {
                    int cId = Integer.parseInt(updCampaignId.getText().trim());
                    int pId = Integer.parseInt(updProductId.getText().trim());
                    BigDecimal disc = new BigDecimal(updDiscount.getText().trim());
                    cpDAO.updateDiscount(cId, pId, disc);
                    info(this, "Discount updated.");
                    populateTable(table, cpDAO.listCampaignProducts(cId));
                } catch (Exception ex) {
                    error(this, ex);
                }
            });

            JPanel listRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
            listRow.add(new JLabel("Campaign ID:"));
            JTextField listCampaignId = new JTextField(6);
            listRow.add(listCampaignId);
            JButton listBtn = new JButton("List Products in Campaign");
            listRow.add(listBtn);

            listBtn.addActionListener(e -> {
                try {
                    int cId = Integer.parseInt(listCampaignId.getText().trim());
                    populateTable(table, cpDAO.listCampaignProducts(cId));
                } catch (Exception ex) {
                    error(this, ex);
                }
            });

            JPanel top = new JPanel(new BorderLayout());
            top.add(form, BorderLayout.NORTH);
            top.add(listRow, BorderLayout.SOUTH);

            add(top, BorderLayout.NORTH);
            add(new JScrollPane(table), BorderLayout.CENTER);
        }
    }


    // Reports tab
    static class ReportsPanel extends JPanel {
        private final ReportDAO reportDAO = new ReportDAO();
        private final JTable table = new JTable();

        private static final String[] REPORTS = {
                "Region Performance",
                "Segment Performance",
                "Platform Performance",
                "Product Performance",
                "Campaign-by-Segment Breakdown (needs Campaign ID)",
                "Budget Alerts (needs Threshold %)",
                "Campaigns Ending Soon (needs Days)",
                "Top Ads by ROAS (needs N)",
                "Underperforming Ads (low CTR)",
                "Segment-Platform Affinity"
        };

        ReportsPanel() {
            setLayout(new BorderLayout(8, 8));
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JComboBox<String> reportCombo = new JComboBox<>(REPORTS);
            JLabel paramLabel = new JLabel("Parameter:");
            JTextField paramField = new JTextField(10);
            JButton runBtn = new JButton("Run Report");

            form.add(new JLabel("Report:"));
            form.add(reportCombo);
            form.add(paramLabel);
            form.add(paramField);
            form.add(runBtn);

            Runnable updateParamVisibility = () -> {
                int idx = reportCombo.getSelectedIndex();
                boolean needsParam = idx == 4 || idx == 5 || idx == 6 || idx == 7;
                paramLabel.setVisible(needsParam);
                paramField.setVisible(needsParam);
                switch (idx) {
                    case 4: paramLabel.setText("Campaign ID:"); break;
                    case 5: paramLabel.setText("Threshold %:"); break;
                    case 6: paramLabel.setText("Within days:"); break;
                    case 7: paramLabel.setText("Top N:"); break;
                    default: break;
                }
                form.revalidate();
                form.repaint();
            };
            reportCombo.addActionListener(e -> updateParamVisibility.run());
            updateParamVisibility.run();

            runBtn.addActionListener(e -> {
                try {
                    int idx = reportCombo.getSelectedIndex();
                    String param = paramField.getText().trim();
                    switch (idx) {
                        case 0: populateTable(table, reportDAO.regionPerformance()); break;
                        case 1: populateTable(table, reportDAO.segmentPerformance()); break;
                        case 2: populateTable(table, reportDAO.platformPerformance()); break;
                        case 3: populateTable(table, reportDAO.productPerformance()); break;
                        case 4: populateTable(table, reportDAO.campaignBySegment(Integer.parseInt(param))); break;
                        case 5: populateTable(table, reportDAO.budgetAlerts(Double.parseDouble(param))); break;
                        case 6: populateTable(table, reportDAO.campaignsEndingSoon(Integer.parseInt(param))); break;
                        case 7: populateTable(table, reportDAO.topAdsByROAS(Integer.parseInt(param))); break;
                        case 8: populateTable(table, reportDAO.underperformingAds()); break;
                        case 9: populateTable(table, reportDAO.segmentPlatformAffinity()); break;
                        default: break;
                    }
                } catch (Exception ex) {
                    error(this, ex);
                }
            });

            add(form, BorderLayout.NORTH);
            add(new JScrollPane(table), BorderLayout.CENTER);
        }
    }
}
