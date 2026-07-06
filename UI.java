import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class JewelryManagementUI extends JFrame {

    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost:3306/Vulcynyx?useSSL=false&allowPublicKeyRetrieval=true";
    static final String USER = "root";
    static final String PASS = "root";

    private JPanel mainPanel;
    private CardLayout cardLayout;

    // Color scheme
    private static final Color PRIMARY_COLOR = new Color(44, 62, 80);
    private static final Color SECONDARY_COLOR = new Color(52, 152, 219);
    private static final Color ACCENT_COLOR = new Color(231, 76, 60);
    private static final Color SUCCESS_COLOR = new Color(46, 204, 113);
    private static final Color BG_COLOR = new Color(236, 240, 241);
    private static final Color CARD_COLOR = Color.WHITE;

    public JewelryManagementUI() {
        setTitle("Jewelry Business Management System");
        setSize(1400, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Test connection first
        if (!testConnection()) {
            JOptionPane.showMessageDialog(this,
                    "Database connection failed!\nPlease check your database configuration.",
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        initComponents();
        setVisible(true);
    }

    private boolean testConnection() {
        try {
            Class.forName(JDBC_DRIVER);
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
            conn.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void initComponents() {
        // Main layout
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Create panels
        mainPanel.add(createDashboardPanel(), "DASHBOARD");
        mainPanel.add(createProductPanel(), "PRODUCTS");
        mainPanel.add(createCustomerPanel(), "CUSTOMERS");
        mainPanel.add(createCampaignPanel(), "CAMPAIGNS");
        mainPanel.add(createBusinessPanel(), "BUSINESS");
        mainPanel.add(createAnalyticsPanel(), "ANALYTICS");
        mainPanel.add(createAdPanel(), "ADS");

        // Create sidebar
        JPanel sidebar = createSidebar();

        // Layout
        setLayout(new BorderLayout());
        add(sidebar, BorderLayout.WEST);
        add(mainPanel, BorderLayout.CENTER);

        getContentPane().setBackground(BG_COLOR);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(PRIMARY_COLOR);
        sidebar.setPreferredSize(new Dimension(250, getHeight()));

        // Logo/Title
        JLabel title = new JLabel("JEWELRY BMS");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setBorder(BorderFactory.createEmptyBorder(30, 20, 30, 20));
        sidebar.add(title);

        // Menu buttons
        String[] menuItems = { "Dashboard", "Products", "Customers", "Campaigns", "Business", "Analytics", "Ads" };
        String[] menuKeys = { "DASHBOARD", "PRODUCTS", "CUSTOMERS", "CAMPAIGNS", "BUSINESS", "ANALYTICS", "ADS" };

        for (int i = 0; i < menuItems.length; i++) {
            final String key = menuKeys[i];
            JButton btn = createMenuButton(menuItems[i]);
            btn.addActionListener(e -> cardLayout.show(mainPanel, key));
            sidebar.add(btn);
            sidebar.add(Box.createRigidArea(new Dimension(0, 5)));
        }

        sidebar.add(Box.createVerticalGlue());

        // Exit button
        JButton exitBtn = createMenuButton("Exit");
        exitBtn.setBackground(ACCENT_COLOR);
        exitBtn.addActionListener(e -> System.exit(0));
        sidebar.add(exitBtn);
        sidebar.add(Box.createRigidArea(new Dimension(0, 20)));

        return sidebar;
    }

    private JButton createMenuButton(String text) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(new Dimension(230, 50));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFont(new Font("Arial", Font.PLAIN, 16));
        btn.setForeground(Color.WHITE);
        btn.setBackground(SECONDARY_COLOR);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(btn.getBackground().brighter());
            }

            public void mouseExited(MouseEvent e) {
                if (!text.equals("Exit")) {
                    btn.setBackground(SECONDARY_COLOR);
                } else {
                    btn.setBackground(ACCENT_COLOR);
                }
            }
        });

        return btn;
    }

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_COLOR);

        JLabel header = new JLabel("Dashboard");
        header.setFont(new Font("Arial", Font.BOLD, 32));
        header.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.add(header, BorderLayout.NORTH);

        JPanel statsPanel = new JPanel(new GridLayout(2, 3, 20, 20));
        statsPanel.setBackground(BG_COLOR);
        statsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Create stat cards
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            // Total Products
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM Product");
            rs.next();
            statsPanel.add(createStatCard("Total Products", String.valueOf(rs.getInt("cnt")), SECONDARY_COLOR));

            // Total Customers
            rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM Customer");
            rs.next();
            statsPanel.add(createStatCard("Total Customers", String.valueOf(rs.getInt("cnt")), SUCCESS_COLOR));

            // Total Revenue
            rs = stmt.executeQuery(
                    "SELECT SUM(b.Qty * p.Price) as total FROM Business b JOIN Product p ON b.ProductID = p.ProductID");
            rs.next();
            statsPanel.add(createStatCard("Total Revenue", "₹" + String.format("%.2f", rs.getDouble("total")),
                    new Color(155, 89, 182)));

            // Low Stock Items
            rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM Product WHERE Stock <= 10");
            rs.next();
            statsPanel.add(createStatCard("Low Stock Items", String.valueOf(rs.getInt("cnt")), ACCENT_COLOR));

            // Active Campaigns
            rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM Campaign");
            rs.next();
            statsPanel
                    .add(createStatCard("Active Campaigns", String.valueOf(rs.getInt("cnt")), new Color(230, 126, 34)));

            // Total Transactions
            rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM Business");
            rs.next();
            statsPanel.add(
                    createStatCard("Total Transactions", String.valueOf(rs.getInt("cnt")), new Color(26, 188, 156)));

        } catch (SQLException e) {
            e.printStackTrace();
        }

        panel.add(statsPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStatCard(String title, String value, Color color) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_COLOR);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 3),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        titleLabel.setForeground(Color.GRAY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 36));
        valueLabel.setForeground(color);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(Box.createVerticalGlue());
        card.add(titleLabel);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(valueLabel);
        card.add(Box.createVerticalGlue());

        return card;
    }

    private JPanel createProductPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_COLOR);

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(BG_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        JLabel header = new JLabel("Product Management");
        header.setFont(new Font("Arial", Font.BOLD, 32));
        headerPanel.add(header, BorderLayout.WEST);

        // Action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(BG_COLOR);

        JButton addBtn = createActionButton("Add Product", SUCCESS_COLOR);
        JButton searchBtn = createActionButton("Search", SECONDARY_COLOR);
        JButton lowStockBtn = createActionButton("Low Stock", ACCENT_COLOR);
        JButton refreshBtn = createActionButton("Refresh", PRIMARY_COLOR);

        buttonPanel.add(addBtn);
        buttonPanel.add(searchBtn);
        buttonPanel.add(lowStockBtn);
        buttonPanel.add(refreshBtn);

        headerPanel.add(buttonPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);

        // Table
        String[] columns = { "ID", "Name", "Category", "Price", "Stock" };
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        styleTable(table);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Load data
        loadProductData(model);

        // Button actions
        addBtn.addActionListener(e -> showAddProductDialog(model));
        searchBtn.addActionListener(e -> showSearchProductDialog(model));
        lowStockBtn.addActionListener(e -> showLowStockDialog(model));
        refreshBtn.addActionListener(e -> loadProductData(model));

        // Double click to edit
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row != -1) {
                        showEditProductDialog(model, row);
                    }
                }
            }
        });

        return panel;
    }

    private JButton createActionButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(color);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(120, 40));
        return btn;
    }

    private void styleTable(JTable table) {
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.setRowHeight(30);
        table.setSelectionBackground(SECONDARY_COLOR);
        table.setSelectionForeground(Color.WHITE);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 5));

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Arial", Font.BOLD, 14));
        header.setBackground(PRIMARY_COLOR);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(header.getWidth(), 40));
    }

    private void loadProductData(DefaultTableModel model) {
        model.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM Product ORDER BY ProductID")) {

            while (rs.next()) {
                model.addRow(new Object[] {
                        rs.getInt("ProductID"),
                        rs.getString("Name"),
                        rs.getString("Category"),
                        "₹" + String.format("%.2f", rs.getDouble("Price")),
                        rs.getInt("Stock")
                });
            }
        } catch (SQLException e) {
            showError("Error loading products: " + e.getMessage());
        }
    }

    private void showAddProductDialog(DefaultTableModel model) {
        JDialog dialog = new JDialog(this, "Add Product", true);
        dialog.setLayout(new GridLayout(6, 2, 10, 10));
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        JTextField idField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField categoryField = new JTextField();
        JTextField priceField = new JTextField();
        JTextField stockField = new JTextField();

        dialog.add(new JLabel("Product ID:"));
        dialog.add(idField);
        dialog.add(new JLabel("Name:"));
        dialog.add(nameField);
        dialog.add(new JLabel("Category:"));
        dialog.add(categoryField);
        dialog.add(new JLabel("Price:"));
        dialog.add(priceField);
        dialog.add(new JLabel("Stock:"));
        dialog.add(stockField);

        JButton saveBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");

        saveBtn.addActionListener(e -> {
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                    PreparedStatement pstmt = conn.prepareStatement(
                            "INSERT INTO Product (ProductID, Name, Category, Price, Stock) VALUES (?, ?, ?, ?, ?)")) {

                pstmt.setInt(1, Integer.parseInt(idField.getText()));
                pstmt.setString(2, nameField.getText());
                pstmt.setString(3, categoryField.getText());
                pstmt.setDouble(4, Double.parseDouble(priceField.getText()));
                pstmt.setInt(5, Integer.parseInt(stockField.getText()));

                pstmt.executeUpdate();
                showSuccess("Product added successfully!");
                loadProductData(model);
                dialog.dispose();
            } catch (SQLException ex) {
                showError("Error adding product: " + ex.getMessage());
            } catch (NumberFormatException ex) {
                showError("Please enter valid numbers for ID, Price, and Stock");
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.add(saveBtn);
        dialog.add(cancelBtn);
        dialog.setVisible(true);
    }

    private void showEditProductDialog(DefaultTableModel model, int row) {
        String idStr = model.getValueAt(row, 0).toString();
        int productID = Integer.parseInt(idStr);

        String[] options = { "Name", "Category", "Price", "Stock" };
        String choice = (String) JOptionPane.showInputDialog(this,
                "Select field to update:",
                "Update Product",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (choice != null) {
            String newValue = JOptionPane.showInputDialog(this, "Enter new " + choice + ":");
            if (newValue != null && !newValue.trim().isEmpty()) {
                updateProduct(productID, choice, newValue, model);
            }
        }
    }

    private void updateProduct(int productID, String field, String value, DefaultTableModel model) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                PreparedStatement pstmt = conn.prepareStatement(
                        "UPDATE Product SET " + field + " = ? WHERE ProductID = ?")) {

            if (field.equals("Price")) {
                pstmt.setDouble(1, Double.parseDouble(value));
            } else if (field.equals("Stock")) {
                pstmt.setInt(1, Integer.parseInt(value));
            } else {
                pstmt.setString(1, value);
            }
            pstmt.setInt(2, productID);

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                showSuccess("Product updated successfully!");
                loadProductData(model);
            } else {
                showError("Product not found!");
            }
        } catch (SQLException e) {
            showError("Error updating product: " + e.getMessage());
        }
    }

    private void showSearchProductDialog(DefaultTableModel model) {
        String[] options = { "By Category", "By Price Range" };
        int choice = JOptionPane.showOptionDialog(this,
                "Choose search type:",
                "Search Products",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (choice == 0) {
            String category = JOptionPane.showInputDialog(this, "Enter category:");
            if (category != null)
                searchByCategory(model, category);
        } else if (choice == 1) {
            searchByPriceRange(model);
        }
    }

    private void searchByCategory(DefaultTableModel model, String category) {
        model.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                PreparedStatement pstmt = conn.prepareStatement(
                        "SELECT * FROM Product WHERE Category LIKE ? ORDER BY Price")) {

            pstmt.setString(1, "%" + category + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                model.addRow(new Object[] {
                        rs.getInt("ProductID"),
                        rs.getString("Name"),
                        rs.getString("Category"),
                        "₹" + String.format("%.2f", rs.getDouble("Price")),
                        rs.getInt("Stock")
                });
            }
        } catch (SQLException e) {
            showError("Error searching products: " + e.getMessage());
        }
    }

    private void searchByPriceRange(DefaultTableModel model) {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        JTextField minField = new JTextField();
        JTextField maxField = new JTextField();
        panel.add(new JLabel("Min Price:"));
        panel.add(minField);
        panel.add(new JLabel("Max Price:"));
        panel.add(maxField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Enter Price Range", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                double min = Double.parseDouble(minField.getText());
                double max = Double.parseDouble(maxField.getText());

                model.setRowCount(0);
                try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                        PreparedStatement pstmt = conn.prepareStatement(
                                "SELECT * FROM Product WHERE Price BETWEEN ? AND ? ORDER BY Price")) {

                    pstmt.setDouble(1, min);
                    pstmt.setDouble(2, max);
                    ResultSet rs = pstmt.executeQuery();

                    while (rs.next()) {
                        model.addRow(new Object[] {
                                rs.getInt("ProductID"),
                                rs.getString("Name"),
                                rs.getString("Category"),
                                "₹" + String.format("%.2f", rs.getDouble("Price")),
                                rs.getInt("Stock")
                        });
                    }
                } catch (SQLException e) {
                    showError("Error searching products: " + e.getMessage());
                }
            } catch (NumberFormatException e) {
                showError("Please enter valid numbers");
            }
        }
    }

    private void showLowStockDialog(DefaultTableModel model) {
        String threshold = JOptionPane.showInputDialog(this, "Enter stock threshold:", "10");
        if (threshold != null) {
            try {
                int thresh = Integer.parseInt(threshold);
                model.setRowCount(0);

                try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                        PreparedStatement pstmt = conn.prepareStatement(
                                "SELECT * FROM Product WHERE Stock <= ? ORDER BY Stock ASC")) {

                    pstmt.setInt(1, thresh);
                    ResultSet rs = pstmt.executeQuery();

                    while (rs.next()) {
                        model.addRow(new Object[] {
                                rs.getInt("ProductID"),
                                rs.getString("Name"),
                                rs.getString("Category"),
                                "₹" + String.format("%.2f", rs.getDouble("Price")),
                                rs.getInt("Stock")
                        });
                    }
                } catch (SQLException e) {
                    showError("Error: " + e.getMessage());
                }
            } catch (NumberFormatException e) {
                showError("Please enter a valid number");
            }
        }
    }

    private JPanel createCustomerPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_COLOR);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(BG_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        JLabel header = new JLabel("Customer Management");
        header.setFont(new Font("Arial", Font.BOLD, 32));
        headerPanel.add(header, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(BG_COLOR);

        JButton addBtn = createActionButton("Add Customer", SUCCESS_COLOR);
        JButton searchBtn = createActionButton("Search", SECONDARY_COLOR);
        JButton refreshBtn = createActionButton("Refresh", PRIMARY_COLOR);

        buttonPanel.add(addBtn);
        buttonPanel.add(searchBtn);
        buttonPanel.add(refreshBtn);

        headerPanel.add(buttonPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);

        String[] columns = { "ID", "Name", "Gender", "Age Group", "City" };
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        styleTable(table);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        panel.add(scrollPane, BorderLayout.CENTER);

        loadCustomerData(model);

        addBtn.addActionListener(e -> showAddCustomerDialog(model));
        searchBtn.addActionListener(e -> {
            String city = JOptionPane.showInputDialog(this, "Enter city:");
            if (city != null)
                searchCustomersByCity(model, city);
        });
        refreshBtn.addActionListener(e -> loadCustomerData(model));

        return panel;
    }

    private void loadCustomerData(DefaultTableModel model) {
        model.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM Customer ORDER BY CustomerID")) {

            while (rs.next()) {
                model.addRow(new Object[] {
                        rs.getInt("CustomerID"),
                        rs.getString("Name"),
                        rs.getString("Gender"),
                        rs.getString("AgeGroup"),
                        rs.getString("City")
                });
            }
        } catch (SQLException e) {
            showError("Error loading customers: " + e.getMessage());
        }
    }

    private void showAddCustomerDialog(DefaultTableModel model) {
        JDialog dialog = new JDialog(this, "Add Customer", true);
        dialog.setLayout(new GridLayout(6, 2, 10, 10));
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        JTextField idField = new JTextField();
        JTextField nameField = new JTextField();
        JComboBox<String> genderBox = new JComboBox<>(new String[] { "M", "F", "O" });
        JComboBox<String> ageBox = new JComboBox<>(new String[] { "Teen", "Adult", "Senior" });
        JTextField cityField = new JTextField();

        dialog.add(new JLabel("Customer ID:"));
        dialog.add(idField);
        dialog.add(new JLabel("Name:"));
        dialog.add(nameField);
        dialog.add(new JLabel("Gender:"));
        dialog.add(genderBox);
        dialog.add(new JLabel("Age Group:"));
        dialog.add(ageBox);
        dialog.add(new JLabel("City:"));
        dialog.add(cityField);

        JButton saveBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");

        saveBtn.addActionListener(e -> {
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                    PreparedStatement pstmt = conn.prepareStatement(
                            "INSERT INTO Customer (CustomerID, Name, Gender, AgeGroup, City) VALUES (?, ?, ?, ?, ?)")) {

                pstmt.setInt(1, Integer.parseInt(idField.getText()));
                pstmt.setString(2, nameField.getText());
                pstmt.setString(3, (String) genderBox.getSelectedItem());
                pstmt.setString(4, (String) ageBox.getSelectedItem());
                pstmt.setString(5, cityField.getText());

                pstmt.executeUpdate();
                showSuccess("Customer added successfully!");
                loadCustomerData(model);
                dialog.dispose();
            } catch (SQLException ex) {
                showError("Error adding customer: " + ex.getMessage());
            } catch (NumberFormatException ex) {
                showError("Please enter a valid ID");
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.add(saveBtn);
        dialog.add(cancelBtn);
        dialog.setVisible(true);
    }

    private void searchCustomersByCity(DefaultTableModel model, String city) {
        model.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                PreparedStatement pstmt = conn.prepareStatement(
                        "SELECT * FROM Customer WHERE City = ?")) {

            pstmt.setString(1, city);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                model.addRow(new Object[] {
                        rs.getInt("CustomerID"),
                        rs.getString("Name"),
                        rs.getString("Gender"),
                        rs.getString("AgeGroup"),
                        rs.getString("City")
                });
            }
        } catch (SQLException e) {
            showError("Error searching customers: " + e.getMessage());
        }
    }

    private JPanel createCampaignPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_COLOR);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(BG_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        JLabel header = new JLabel("Campaign Management");
        header.setFont(new Font("Arial", Font.BOLD, 32));
        headerPanel.add(header, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(BG_COLOR);

        JButton addBtn = createActionButton("Add Campaign", SUCCESS_COLOR);
        JButton refreshBtn = createActionButton("Refresh", PRIMARY_COLOR);

        buttonPanel.add(addBtn);
        buttonPanel.add(refreshBtn);

        headerPanel.add(buttonPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);

        String[] columns = { "ID", "Name", "Type", "Discount %", "Start Date", "End Date" };
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        styleTable(table);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        panel.add(scrollPane, BorderLayout.CENTER);

        loadCampaignData(model);

        addBtn.addActionListener(e -> showAddCampaignDialog(model));
        refreshBtn.addActionListener(e -> loadCampaignData(model));

        return panel;
    }

    private void loadCampaignData(DefaultTableModel model) {
        model.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM Campaign ORDER BY CampaignID")) {

            while (rs.next()) {
                model.addRow(new Object[] {
                        rs.getInt("CampaignID"),
                        rs.getString("Name"),
                        rs.getString("Type"),
                        rs.getDouble("Discount") + "%",
                        rs.getDate("StartDate"),
                        rs.getDate("EndDate")
                });
            }
        } catch (SQLException e) {
            showError("Error loading campaigns: " + e.getMessage());
        }
    }

    private void showAddCampaignDialog(DefaultTableModel model) {
        JDialog dialog = new JDialog(this, "Add Campaign", true);
        dialog.setLayout(new GridLayout(7, 2, 10, 10));
        dialog.setSize(450, 350);
        dialog.setLocationRelativeTo(this);

        JTextField idField = new JTextField();
        JTextField nameField = new JTextField();
        JComboBox<String> typeBox = new JComboBox<>(new String[] { "Seasonal", "Promotional", "Clearance", "Loyalty" });
        JTextField discountField = new JTextField();
        JTextField startField = new JTextField();
        JTextField endField = new JTextField();

        dialog.add(new JLabel("Campaign ID:"));
        dialog.add(idField);
        dialog.add(new JLabel("Name:"));
        dialog.add(nameField);
        dialog.add(new JLabel("Type:"));
        dialog.add(typeBox);
        dialog.add(new JLabel("Discount %:"));
        dialog.add(discountField);
        dialog.add(new JLabel("Start Date (YYYY-MM-DD):"));
        dialog.add(startField);
        dialog.add(new JLabel("End Date (YYYY-MM-DD):"));
        dialog.add(endField);

        JButton saveBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");

        saveBtn.addActionListener(e -> {
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                    PreparedStatement pstmt = conn.prepareStatement(
                            "INSERT INTO Campaign (CampaignID, Name, Type, Discount, StartDate, EndDate) VALUES (?, ?, ?, ?, ?, ?)")) {

                pstmt.setInt(1, Integer.parseInt(idField.getText()));
                pstmt.setString(2, nameField.getText());
                pstmt.setString(3, (String) typeBox.getSelectedItem());
                pstmt.setDouble(4, Double.parseDouble(discountField.getText()));
                pstmt.setDate(5, Date.valueOf(startField.getText()));
                pstmt.setDate(6, Date.valueOf(endField.getText()));

                pstmt.executeUpdate();
                showSuccess("Campaign added successfully!");
                loadCampaignData(model);
                dialog.dispose();
            } catch (SQLException ex) {
                showError("Error adding campaign: " + ex.getMessage());
            } catch (Exception ex) {
                showError("Invalid input: " + ex.getMessage());
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.add(saveBtn);
        dialog.add(cancelBtn);
        dialog.setVisible(true);
    }

    private JPanel createBusinessPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_COLOR);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(BG_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        JLabel header = new JLabel("Business Transactions");
        header.setFont(new Font("Arial", Font.BOLD, 32));
        headerPanel.add(header, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(BG_COLOR);

        JButton addBtn = createActionButton("Add Transaction", SUCCESS_COLOR);
        JButton refreshBtn = createActionButton("Refresh", PRIMARY_COLOR);

        buttonPanel.add(addBtn);
        buttonPanel.add(refreshBtn);

        headerPanel.add(buttonPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);

        String[] columns = { "Product ID", "Product", "Customer ID", "Customer", "Date", "Qty", "Amount" };
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        styleTable(table);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        panel.add(scrollPane, BorderLayout.CENTER);

        loadBusinessData(model);

        addBtn.addActionListener(e -> showAddTransactionDialog(model));
        refreshBtn.addActionListener(e -> loadBusinessData(model));

        return panel;
    }

    private void loadBusinessData(DefaultTableModel model) {
        model.setRowCount(0);
        String query = "SELECT b.ProductID, p.Name, b.CustomerID, c.Name as CustomerName, " +
                "b.PDate, b.Qty, b.PAmount " +
                "FROM Business b " +
                "JOIN Product p ON b.ProductID = p.ProductID " +
                "JOIN Customer c ON b.CustomerID = c.CustomerID " +
                "ORDER BY b.PDate DESC";

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                model.addRow(new Object[] {
                        rs.getInt("ProductID"),
                        rs.getString("Name"),
                        rs.getInt("CustomerID"),
                        rs.getString("CustomerName"),
                        rs.getDate("PDate"),
                        rs.getInt("Qty"),
                        "₹" + String.format("%.2f", rs.getDouble("PAmount"))
                });
            }
        } catch (SQLException e) {
            showError("Error loading transactions: " + e.getMessage());
        }
    }

    private void showAddTransactionDialog(DefaultTableModel model) {
        JDialog dialog = new JDialog(this, "Add Transaction", true);
        dialog.setLayout(new GridLayout(5, 2, 10, 10));
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(this);

        JTextField productField = new JTextField();
        JTextField customerField = new JTextField();
        JTextField dateField = new JTextField();
        JTextField qtyField = new JTextField();

        dialog.add(new JLabel("Product ID:"));
        dialog.add(productField);
        dialog.add(new JLabel("Customer ID:"));
        dialog.add(customerField);
        dialog.add(new JLabel("Date (YYYY-MM-DD):"));
        dialog.add(dateField);
        dialog.add(new JLabel("Quantity:"));
        dialog.add(qtyField);

        JButton saveBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");

        saveBtn.addActionListener(e -> {
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                    PreparedStatement pstmt = conn.prepareStatement(
                            "INSERT INTO Business (ProductID, CustomerID, PDate, Qty) VALUES (?, ?, ?, ?)")) {

                pstmt.setInt(1, Integer.parseInt(productField.getText()));
                pstmt.setInt(2, Integer.parseInt(customerField.getText()));
                pstmt.setDate(3, Date.valueOf(dateField.getText()));
                pstmt.setInt(4, Integer.parseInt(qtyField.getText()));

                pstmt.executeUpdate();
                showSuccess("Transaction added successfully!");
                loadBusinessData(model);
                dialog.dispose();
            } catch (SQLException ex) {
                showError("Error adding transaction: " + ex.getMessage());
            } catch (Exception ex) {
                showError("Invalid input: " + ex.getMessage());
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.add(saveBtn);
        dialog.add(cancelBtn);
        dialog.setVisible(true);
    }

    private JPanel createAnalyticsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_COLOR);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(BG_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        JLabel header = new JLabel("Analytics & Reports");
        header.setFont(new Font("Arial", Font.BOLD, 32));
        headerPanel.add(header, BorderLayout.WEST);
        panel.add(headerPanel, BorderLayout.NORTH);

        // Analytics menu
        JPanel menuPanel = new JPanel(new GridLayout(4, 2, 15, 15));
        menuPanel.setBackground(BG_COLOR);
        menuPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JButton topCustomersBtn = createAnalyticsButton("Top 5 Customers", "By Spending");
        JButton revenueCategoryBtn = createAnalyticsButton("Revenue", "By Category");
        JButton revenuePlatformBtn = createAnalyticsButton("Revenue", "By Platform");
        JButton topSellingBtn = createAnalyticsButton("Top Selling", "By Region");
        JButton campaignROIBtn = createAnalyticsButton("Campaign", "ROI Analysis");
        JButton avgPriceBtn = createAnalyticsButton("Average Price", "Per Category");
        JButton qtySoldBtn = createAnalyticsButton("Quantity Sold", "Per Product");
        JButton topSearchedBtn = createAnalyticsButton("Top Searched", "By Age Group");

        menuPanel.add(topCustomersBtn);
        menuPanel.add(revenueCategoryBtn);
        menuPanel.add(revenuePlatformBtn);
        menuPanel.add(topSellingBtn);
        menuPanel.add(campaignROIBtn);
        menuPanel.add(avgPriceBtn);
        menuPanel.add(qtySoldBtn);
        menuPanel.add(topSearchedBtn);

        panel.add(menuPanel, BorderLayout.CENTER);

        // Button actions
        topCustomersBtn.addActionListener(e -> showTopCustomers());
        revenueCategoryBtn.addActionListener(e -> showRevenueByCategory());
        revenuePlatformBtn.addActionListener(e -> showRevenueByPlatform());
        topSellingBtn.addActionListener(e -> showTopSellingByRegion());
        campaignROIBtn.addActionListener(e -> showCampaignROI());
        avgPriceBtn.addActionListener(e -> showAvgPricePerCategory());
        qtySoldBtn.addActionListener(e -> showQuantitySold());
        topSearchedBtn.addActionListener(e -> showTopSearched());

        return panel;
    }

    private JButton createAnalyticsButton(String title, String subtitle) {
        JButton btn = new JButton(
                "<html><center><b>" + title + "</b><br><small>" + subtitle + "</small></center></html>");
        btn.setFont(new Font("Arial", Font.PLAIN, 16));
        btn.setForeground(Color.WHITE);
        btn.setBackground(SECONDARY_COLOR);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(200, 80));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(SECONDARY_COLOR.brighter());
            }

            public void mouseExited(MouseEvent e) {
                btn.setBackground(SECONDARY_COLOR);
            }
        });

        return btn;
    }

    private void showTopCustomers() {
        String query = "SELECT c.CustomerID, c.Name AS CustomerName, " +
                "SUM(b.Qty * p.Price) AS TotalSpent " +
                "FROM Customer c " +
                "JOIN Business b ON c.CustomerID = b.CustomerID " +
                "JOIN Product p ON b.ProductID = p.ProductID " +
                "GROUP BY c.CustomerID, c.Name " +
                "ORDER BY TotalSpent DESC LIMIT 5";

        showAnalyticsResult(query, new String[] { "Customer ID", "Name", "Total Spent" },
                "Top 5 Customers by Spending");
    }

    private void showRevenueByCategory() {
        String query = "SELECT p.Category, SUM(b.Qty * p.Price) AS TotalRevenue " +
                "FROM Product p " +
                "JOIN Business b ON p.ProductID = b.ProductID " +
                "GROUP BY p.Category " +
                "ORDER BY TotalRevenue DESC";

        showAnalyticsResult(query, new String[] { "Category", "Total Revenue" }, "Revenue by Category");
    }

    private void showRevenueByPlatform() {
        String query = "SELECT Platform, SUM(Revenue) AS Total_Revenue " +
                "FROM Ads " +
                "GROUP BY Platform " +
                "ORDER BY Total_Revenue DESC";

        showAnalyticsResult(query, new String[] { "Platform", "Total Revenue" }, "Revenue by Platform");
    }

    private void showTopSellingByRegion() {
        String query = "SELECT RegionName, ProductName, TotalSold FROM (" +
                "SELECT r.Region AS RegionName, p.Name AS ProductName, " +
                "SUM(b.Qty) AS TotalSold, " +
                "RANK() OVER (PARTITION BY r.Region ORDER BY SUM(b.Qty) DESC) AS rank_in_region " +
                "FROM Regional_info r " +
                "JOIN Customer c ON r.City = c.City " +
                "JOIN Business b ON b.CustomerID = c.CustomerID " +
                "JOIN Product p ON b.ProductID = p.ProductID " +
                "GROUP BY r.Region, p.Name) ranked " +
                "WHERE rank_in_region <= 5";

        showAnalyticsResult(query, new String[] { "Region", "Product", "Total Sold" },
                "Top Selling Products by Region");
    }

    private void showCampaignROI() {
        String query = "SELECT c.CampaignID, c.Name AS CampaignName, " +
                "SUM(a.Revenue) AS TotalRevenue, SUM(a.Cost) AS TotalCost, " +
                "ROUND((SUM(a.Revenue) - SUM(a.Cost)) / NULLIF(SUM(a.Cost), 0) * 100, 2) AS ROI " +
                "FROM Campaign c " +
                "JOIN Ads_Campaign ac ON c.CampaignID = ac.CampaignID " +
                "JOIN Ads a ON a.AdsID = ac.AdsID AND a.Platform = ac.Platform " +
                "GROUP BY c.CampaignID, c.Name " +
                "ORDER BY ROI DESC LIMIT 5";

        showAnalyticsResult(query, new String[] { "ID", "Campaign", "Revenue", "Cost", "ROI %" },
                "Campaign ROI Analysis");
    }

    private void showAvgPricePerCategory() {
        String query = "SELECT Category, ROUND(AVG(Price), 2) AS AvgPrice " +
                "FROM Product " +
                "GROUP BY Category " +
                "ORDER BY AvgPrice DESC";

        showAnalyticsResult(query, new String[] { "Category", "Average Price" }, "Average Price per Category");
    }

    private void showQuantitySold() {
        String query = "SELECT p.ProductID, p.Name AS ProductName, SUM(b.Qty) AS TotalSold " +
                "FROM Product p " +
                "JOIN Business b ON p.ProductID = b.ProductID " +
                "GROUP BY p.ProductID, p.Name " +
                "ORDER BY TotalSold DESC";

        showAnalyticsResult(query, new String[] { "Product ID", "Product Name", "Total Sold" },
                "Total Quantity Sold per Product");
    }

    private void showTopSearched() {
        String query = "SELECT * FROM (" +
                "SELECT a.AgeGroup, p.ProductID, p.Name AS ProductName, " +
                "SUM(a.Impressions) AS Total_Impressions, " +
                "RANK() OVER (PARTITION BY a.AgeGroup ORDER BY SUM(a.Impressions) DESC) AS rnk " +
                "FROM Ads a " +
                "JOIN Products_Ads pa ON a.AdsID = pa.AdsID AND a.Platform = pa.Platform " +
                "JOIN Product p ON pa.ProductID = p.ProductID " +
                "GROUP BY a.AgeGroup, p.ProductID, p.Name) ranked " +
                "WHERE rnk = 1";

        showAnalyticsResult(query, new String[] { "Age Group", "Product ID", "Product Name", "Impressions" },
                "Top Searched Products by Age Group");
    }

    private void showAnalyticsResult(String query, String[] columns, String title) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setSize(800, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        styleTable(table);
        JScrollPane scrollPane = new JScrollPane(table);

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();

            while (rs.next()) {
                Object[] row = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    Object value = rs.getObject(i);
                    if (value instanceof Double) {
                        row[i - 1] = "₹" + String.format("%.2f", (Double) value);
                    } else {
                        row[i - 1] = value;
                    }
                }
                model.addRow(row);
            }
        } catch (SQLException e) {
            showError("Error loading analytics: " + e.getMessage());
        }

        dialog.add(scrollPane, BorderLayout.CENTER);

        JButton closeBtn = createActionButton("Close", PRIMARY_COLOR);
        closeBtn.addActionListener(e -> dialog.dispose());
        JPanel btnPanel = new JPanel();
        btnPanel.add(closeBtn);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private JPanel createAdPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_COLOR);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(BG_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        JLabel header = new JLabel("Advertisement Management");
        header.setFont(new Font("Arial", Font.BOLD, 32));
        headerPanel.add(header, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(BG_COLOR);

        JButton addBtn = createActionButton("Add Ad", SUCCESS_COLOR);
        JButton lossBtn = createActionButton("Ads at Loss", ACCENT_COLOR);
        JButton refreshBtn = createActionButton("Refresh", PRIMARY_COLOR);

        buttonPanel.add(addBtn);
        buttonPanel.add(lossBtn);
        buttonPanel.add(refreshBtn);

        headerPanel.add(buttonPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);

        String[] columns = { "Ad ID", "Platform", "Age Group", "Impressions", "Conversions", "Revenue", "Cost",
                "ROI %" };
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        styleTable(table);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        panel.add(scrollPane, BorderLayout.CENTER);

        loadAdData(model);

        addBtn.addActionListener(e -> showAddAdDialog(model));
        lossBtn.addActionListener(e -> showAdsAtLoss(model));
        refreshBtn.addActionListener(e -> loadAdData(model));

        return panel;
    }

    private void loadAdData(DefaultTableModel model) {
        model.setRowCount(0);
        String query = "SELECT AdsID, Platform, AgeGroup, Impressions, Conversions, Revenue, Cost, " +
                "ROUND((Revenue - Cost) / NULLIF(Cost, 0) * 100, 2) AS ROI " +
                "FROM Ads ORDER BY AdsID";

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                model.addRow(new Object[] {
                        rs.getInt("AdsID"),
                        rs.getString("Platform"),
                        rs.getString("AgeGroup"),
                        rs.getInt("Impressions"),
                        rs.getInt("Conversions"),
                        "₹" + String.format("%.2f", rs.getDouble("Revenue")),
                        "₹" + String.format("%.2f", rs.getDouble("Cost")),
                        String.format("%.2f", rs.getDouble("ROI")) + "%"
                });
            }
        } catch (SQLException e) {
            showError("Error loading ads: " + e.getMessage());
        }
    }

    private void showAddAdDialog(DefaultTableModel model) {
        JDialog dialog = new JDialog(this, "Add Advertisement", true);
        dialog.setLayout(new GridLayout(8, 2, 10, 10));
        dialog.setSize(450, 400);
        dialog.setLocationRelativeTo(this);

        JTextField idField = new JTextField();
        JComboBox<String> platformBox = new JComboBox<>(new String[] { "Instagram", "YouTube", "Google" });
        JComboBox<String> ageBox = new JComboBox<>(new String[] { "Teen", "Adult", "Senior" });
        JTextField impressionsField = new JTextField();
        JTextField conversionsField = new JTextField();
        JTextField revenueField = new JTextField();
        JTextField costField = new JTextField();

        dialog.add(new JLabel("Ad ID:"));
        dialog.add(idField);
        dialog.add(new JLabel("Platform:"));
        dialog.add(platformBox);
        dialog.add(new JLabel("Age Group:"));
        dialog.add(ageBox);
        dialog.add(new JLabel("Impressions:"));
        dialog.add(impressionsField);
        dialog.add(new JLabel("Conversions:"));
        dialog.add(conversionsField);
        dialog.add(new JLabel("Revenue:"));
        dialog.add(revenueField);
        dialog.add(new JLabel("Cost:"));
        dialog.add(costField);

        JButton saveBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");

        saveBtn.addActionListener(e -> {
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                    PreparedStatement pstmt = conn.prepareStatement(
                            "INSERT INTO Ads (AdsID, Platform, AgeGroup, Impressions, Conversions, Revenue, Cost) VALUES (?, ?, ?, ?, ?, ?, ?)")) {

                pstmt.setInt(1, Integer.parseInt(idField.getText()));
                pstmt.setString(2, (String) platformBox.getSelectedItem());
                pstmt.setString(3, (String) ageBox.getSelectedItem());
                pstmt.setInt(4, Integer.parseInt(impressionsField.getText()));
                pstmt.setInt(5, Integer.parseInt(conversionsField.getText()));
                pstmt.setDouble(6, Double.parseDouble(revenueField.getText()));
                pstmt.setDouble(7, Double.parseDouble(costField.getText()));

                pstmt.executeUpdate();
                showSuccess("Advertisement added successfully!");
                loadAdData(model);
                dialog.dispose();
            } catch (SQLException ex) {
                showError("Error adding ad: " + ex.getMessage());
            } catch (NumberFormatException ex) {
                showError("Please enter valid numbers");
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.add(saveBtn);
        dialog.add(cancelBtn);
        dialog.setVisible(true);
    }

    private void showAdsAtLoss(DefaultTableModel model) {
        model.setRowCount(0);
        String query = "SELECT AdsID, Platform, AgeGroup, Impressions, Conversions, Revenue, Cost, " +
                "ROUND((Revenue - Cost) / NULLIF(Cost, 0) * 100, 2) AS ROI " +
                "FROM Ads " +
                "WHERE Revenue < Cost " +
                "ORDER BY ROI ASC";

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                model.addRow(new Object[] {
                        rs.getInt("AdsID"),
                        rs.getString("Platform"),
                        rs.getString("AgeGroup"),
                        rs.getInt("Impressions"),
                        rs.getInt("Conversions"),
                        "₹" + String.format("%.2f", rs.getDouble("Revenue")),
                        "₹" + String.format("%.2f", rs.getDouble("Cost")),
                        String.format("%.2f", rs.getDouble("ROI")) + "%"
                });
            }
        } catch (SQLException e) {
            showError("Error loading ads at loss: " + e.getMessage());
        }
    }

    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> new JewelryManagementUI());
    }
}
