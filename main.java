package mini;

import java.sql.*;
import java.util.Scanner;

public class proj {

	static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
	static final String DB_URL = "jdbc:mysql://localhost:3306/Vulcynyx?useSSL=false&allowPublicKeyRetrieval=true";
	static final String USER = "root";
	static final String PASS = "root";

	public static void main(String[] args) {

		// Test connection first
		if (!testConnection()) {
			System.out.println("Connection test failed. Exiting...");
			return;
		}

		Scanner sc = new Scanner(System.in);
		boolean continueRunning = true;

		while (continueRunning) {
			displayMainMenu();
			int mainChoice = getValidInt(sc, "Enter your choice: ");

			switch (mainChoice) {
			case 1:
				handleProductOperations(sc);
				break;
			case 2:
				handleCustomerOperations(sc);
				break;
			case 3:
				handleCampaignOperations(sc);
				break;
			case 4:
				handleBusinessOperations(sc);
				break;
			case 5:
				handleAnalyticsOperations(sc);
				break;
			case 6:
				handleAdOperations(sc);
				break;
			case 0:
				continueRunning = false;
				System.out.println("Thank you for using Jewelry Business Management System!");
				break;
			default:
				System.out.println("Invalid choice. Please try again.");
			}
		}
		sc.close();
	}

	// ==================== CONNECTION TEST ====================
	private static boolean testConnection() {
		System.out.println("\n=== Testing Database Connection ===\n");

		try {
			Class.forName(JDBC_DRIVER);
			System.out.println("✓ JDBC Driver loaded successfully!");
		} catch (ClassNotFoundException e) {
			System.out.println("✗ JDBC Driver NOT found!");
			System.out.println("Solution: Add mysql-connector-j-8.2.0.jar to classpath");
			return false;
		}

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
			System.out.println("✓ Connection successful!");
			DatabaseMetaData metaData = conn.getMetaData();
			System.out.println("Database: " + metaData.getDatabaseProductName());
			System.out.println("Version: " + metaData.getDatabaseProductVersion());
			System.out.println("=================================\n");
			return true;
		} catch (SQLException e) {
			System.out.println("✗ Connection failed!");
			System.out.println("Error: " + e.getMessage());
			return false;
		}
	}

	// ==================== MENU DISPLAYS ====================
	private static void displayMainMenu() {
		System.out.println("\n╔═══════════════════════════════════════════╗");
		System.out.println("║  JEWELRY BUSINESS MANAGEMENT SYSTEM       ║");
		System.out.println("╠═══════════════════════════════════════════╣");
		System.out.println("║  1. Product Management                    ║");
		System.out.println("║  2. Customer Management                   ║");
		System.out.println("║  3. Campaign Management                   ║");
		System.out.println("║  4. Business Transactions                 ║");
		System.out.println("║  5. Analytics & Reports                   ║");
		System.out.println("║  6. Advertisement Management              ║");
		System.out.println("║  0. Exit                                  ║");
		System.out.println("╚═══════════════════════════════════════════╝");
	}

	private static void displayProductMenu() {
		System.out.println("\n--- Product Management ---");
		System.out.println("1. Add Product");
		System.out.println("2. View All Products");
		System.out.println("3. Search by Category");
		System.out.println("4. Search by Price Range");
		System.out.println("5. Update Product");
		System.out.println("6. Delete Product");
		System.out.println("7. Sort by Price");
		System.out.println("8. Low Stock Alert");
		System.out.println("9. Most Purchased Products in Price Range");
		System.out.println("10. Products in Stock but Never Sold");
		System.out.println("0. Back to Main Menu");
	}

	private static void displayAnalyticsMenu() {
		System.out.println("\n--- Analytics & Reports ---");
		System.out.println("1. Top 5 Customers by Spending");
		System.out.println("2. Revenue by Category");
		System.out.println("3. Revenue by Platform");
		System.out.println("4. Top Selling Products by Region");
		System.out.println("5. Campaign ROI");
		System.out.println("6. Average Price per Category");
		System.out.println("7. Total Quantity Sold per Product");
		System.out.println("8. Top Searched Products by Age Group");
		System.out.println("0. Back to Main Menu");
	}

	// ==================== PRODUCT OPERATIONS ====================
	private static void handleProductOperations(Scanner sc) {
		boolean back = false;
		while (!back) {
			displayProductMenu();
			int choice = getValidInt(sc, "Enter your choice: ");

			switch (choice) {
			case 1:
				insertProduct(sc);
				break;
			case 2:
				displayAllProducts();
				break;
			case 3:
				searchProductByCategory(sc);
				break;
			case 4:
				searchProductByPriceRange(sc);
				break;
			case 5:
				updateProduct(sc);
				break;
			case 6:
				deleteProduct(sc);
				break;
			case 7:
				sortProductsByPrice();
				break;
			case 8:
				lowStockAlert(sc);
				break;
			case 9:
				mostPurchasedProductsInPriceRange(sc);
				break;
			case 10:
				productsInStockNotSold();
				break;
			case 0:
				back = true;
				break;
			default:
				System.out.println("Invalid choice.");
			}
		}
	}

	private static void insertProduct(Scanner sc) {
		String query = "INSERT INTO Product (ProductID, Name, Category, Price, Stock) VALUES (?, ?, ?, ?, ?)";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query)) {

			System.out.println("\n--- Add New Product ---");
			int productID = getValidInt(sc, "Enter Product ID: ");
			sc.nextLine(); // consume newline
			System.out.print("Enter Product Name: ");
			String name = sc.nextLine();
			System.out.print("Enter Category: ");
			String category = sc.nextLine();
			double price = getValidDouble(sc, "Enter Price: ");
			int stock = getValidInt(sc, "Enter Stock Quantity: ");

			pstmt.setInt(1, productID);
			pstmt.setString(2, name);
			pstmt.setString(3, category);
			pstmt.setDouble(4, price);
			pstmt.setInt(5, stock);

			int rowsAffected = pstmt.executeUpdate();
			if (rowsAffected > 0) {
				System.out.println("✓ Product added successfully!");
			}

		} catch (SQLException e) {
			handleSQLException("inserting product", e);
		}
	}

	private static void productsInStockNotSold() {
		String query = """
				SELECT
				    p.ProductID,
				    p.Name,
				    p.Category,
				    p.Price,
				    p.Stock
				FROM Product p
				WHERE p.Stock > 0
				  AND p.ProductID NOT IN (SELECT DISTINCT ProductID FROM Business);
				""";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query);
				ResultSet rs = pstmt.executeQuery()) {

			System.out.println("\n=== Products in Stock but Never Sold ===");
			System.out.printf("%-10s %-30s %-20s %-10s %-10s%n", "ID", "Name", "Category", "Price", "Stock");
			System.out.println("-".repeat(80));

			boolean data = false;
			while (rs.next()) {
				data = true;
				System.out.printf("%-10d %-30s %-20s ₹%-9.2f %-10d%n", rs.getInt("ProductID"), rs.getString("Name"),
						rs.getString("Category"), rs.getDouble("Price"), rs.getInt("Stock"));
			}

			if (!data)
				System.out.println("All products have been sold at least once.");

		} catch (SQLException e) {
			handleSQLException("checking unsold products", e);
		}
	}

	private static void mostPurchasedProductsInPriceRange(Scanner sc) {
		String query = """
				SELECT
				    p.ProductID,
				    p.Name,
				    p.Category,
				    p.Price,
				    SUM(b.Qty) AS TotalSold
				FROM Business b
				JOIN Product p ON b.ProductID = p.ProductID
				WHERE p.Price BETWEEN ? AND ?
				GROUP BY p.ProductID, p.Name, p.Category, p.Price
				ORDER BY TotalSold DESC
				LIMIT 5;
				""";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query)) {

			double min = getValidDouble(sc, "Enter Minimum Price: ");
			double max = getValidDouble(sc, "Enter Maximum Price: ");
			pstmt.setDouble(1, min);
			pstmt.setDouble(2, max);
			ResultSet rs = pstmt.executeQuery();

			System.out.println("\n=== Top 5 Most Purchased Products in ₹" + min + " - ₹" + max + " ===");
			System.out.printf("%-10s %-25s %-20s %-10s %-10s%n", "ID", "Name", "Category", "Price", "Sold");
			System.out.println("-".repeat(75));

			boolean data = false;
			while (rs.next()) {
				data = true;
				System.out.printf("%-10d %-25s %-20s ₹%-9.2f %-10d%n", rs.getInt("ProductID"), rs.getString("Name"),
						rs.getString("Category"), rs.getDouble("Price"), rs.getInt("TotalSold"));
			}

			if (!data)
				System.out.println("No products sold in that range.");

		} catch (SQLException e) {
			handleSQLException("fetching most purchased products", e);
		}
	}

	private static void displayAllProducts() {
		String query = "SELECT ProductID, Name, Category, Price, Stock FROM Product ORDER BY ProductID";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query);
				ResultSet rs = pstmt.executeQuery()) {

			System.out.println("\n" + "=".repeat(100));
			System.out.printf("%-10s %-30s %-20s %-15s %-10s%n", "ID", "Name", "Category", "Price", "Stock");
			System.out.println("=".repeat(100));

			boolean hasData = false;
			while (rs.next()) {
				hasData = true;
				System.out.printf("%-10d %-30s %-20s ₹%-14.2f %-10d%n", rs.getInt("ProductID"), rs.getString("Name"),
						rs.getString("Category"), rs.getDouble("Price"), rs.getInt("Stock"));
			}

			if (!hasData) {
				System.out.println("No products found in database.");
			}
			System.out.println("=".repeat(100));

		} catch (SQLException e) {
			handleSQLException("displaying products", e);
		}
	}

	private static void searchProductByCategory(Scanner sc) {
		String query = "SELECT ProductID, Name, Category, Price, Stock FROM Product WHERE Category LIKE ? ORDER BY Price";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query)) {

			sc.nextLine(); // consume newline
			System.out.print("Enter Category to search: ");
			String category = sc.nextLine();

			pstmt.setString(1, "%" + category + "%");
			ResultSet rs = pstmt.executeQuery();

			System.out.println("\n--- Search Results ---");
			System.out.printf("%-10s %-30s %-20s %-15s %-10s%n", "ID", "Name", "Category", "Price", "Stock");
			System.out.println("-".repeat(90));

			boolean hasData = false;
			while (rs.next()) {
				hasData = true;
				System.out.printf("%-10d %-30s %-20s ₹%-14.2f %-10d%n", rs.getInt("ProductID"), rs.getString("Name"),
						rs.getString("Category"), rs.getDouble("Price"), rs.getInt("Stock"));
			}

			if (!hasData) {
				System.out.println("No products found in category: " + category);
			}
			rs.close();

		} catch (SQLException e) {
			handleSQLException("searching products", e);
		}
	}

	private static void searchProductByPriceRange(Scanner sc) {
		String query = "SELECT ProductID, Name, Category, Price, Stock FROM Product "
				+ "WHERE Price BETWEEN ? AND ? ORDER BY Price";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query)) {

			double minPrice = getValidDouble(sc, "Enter Minimum Price: ");
			double maxPrice = getValidDouble(sc, "Enter Maximum Price: ");

			pstmt.setDouble(1, minPrice);
			pstmt.setDouble(2, maxPrice);
			ResultSet rs = pstmt.executeQuery();

			System.out.println("\n--- Products in Price Range ₹" + minPrice + " - ₹" + maxPrice + " ---");
			System.out.printf("%-10s %-30s %-20s %-15s %-10s%n", "ID", "Name", "Category", "Price", "Stock");
			System.out.println("-".repeat(90));

			boolean hasData = false;
			while (rs.next()) {
				hasData = true;
				System.out.printf("%-10d %-30s %-20s ₹%-14.2f %-10d%n", rs.getInt("ProductID"), rs.getString("Name"),
						rs.getString("Category"), rs.getDouble("Price"), rs.getInt("Stock"));
			}

			if (!hasData) {
				System.out.println("No products found in this price range.");
			}
			rs.close();

		} catch (SQLException e) {
			handleSQLException("searching products by price", e);
		}
	}

	private static void updateProduct(Scanner sc) {
		System.out.println("\n--- Update Product ---");
		int productID = getValidInt(sc, "Enter Product ID to update: ");

		System.out.println("Select field to update:");
		System.out.println("1. Name");
		System.out.println("2. Category");
		System.out.println("3. Price");
		System.out.println("4. Stock");
		int fieldChoice = getValidInt(sc, "Enter choice: ");

		String column;
		Object value;

		switch (fieldChoice) {
		case 1:
			column = "Name";
			sc.nextLine();
			System.out.print("Enter new Name: ");
			value = sc.nextLine();
			break;
		case 2:
			column = "Category";
			sc.nextLine();
			System.out.print("Enter new Category: ");
			value = sc.nextLine();
			break;
		case 3:
			column = "Price";
			value = getValidDouble(sc, "Enter new Price: ");
			break;
		case 4:
			column = "Stock";
			value = getValidInt(sc, "Enter new Stock: ");
			break;
		default:
			System.out.println("Invalid choice.");
			return;
		}

		String query = "UPDATE Product SET " + column + " = ? WHERE ProductID = ?";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query)) {

			if (value instanceof String) {
				pstmt.setString(1, (String) value);
			} else if (value instanceof Double) {
				pstmt.setDouble(1, (Double) value);
			} else if (value instanceof Integer) {
				pstmt.setInt(1, (Integer) value);
			}
			pstmt.setInt(2, productID);

			int rowsAffected = pstmt.executeUpdate();
			if (rowsAffected > 0) {
				System.out.println("✓ Product updated successfully!");
			} else {
				System.out.println("✗ Product ID not found.");
			}

		} catch (SQLException e) {
			handleSQLException("updating product", e);
		}
	}

	private static void deleteProduct(Scanner sc) {
		String query = "DELETE FROM Product WHERE ProductID = ?";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query)) {

			int productID = getValidInt(sc, "Enter Product ID to delete: ");

			System.out.print("Are you sure you want to delete? (yes/no): ");
			String confirm = sc.next();
			if (!confirm.equalsIgnoreCase("yes")) {
				System.out.println("Deletion cancelled.");
				return;
			}

			pstmt.setInt(1, productID);
			int rowsAffected = pstmt.executeUpdate();

			if (rowsAffected > 0) {
				System.out.println("✓ Product deleted successfully!");
			} else {
				System.out.println("✗ Product ID not found.");
			}

		} catch (SQLException e) {
			handleSQLException("deleting product", e);
		}
	}

	private static void sortProductsByPrice() {
		String query = "SELECT ProductID, Name, Category, Price, Stock FROM Product ORDER BY Price ASC";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query);
				ResultSet rs = pstmt.executeQuery()) {

			System.out.println("\n--- Products Sorted by Price (Low to High) ---");
			System.out.printf("%-10s %-30s %-20s %-15s %-10s%n", "ID", "Name", "Category", "Price", "Stock");
			System.out.println("-".repeat(90));

			while (rs.next()) {
				System.out.printf("%-10d %-30s %-20s ₹%-14.2f %-10d%n", rs.getInt("ProductID"), rs.getString("Name"),
						rs.getString("Category"), rs.getDouble("Price"), rs.getInt("Stock"));
			}

		} catch (SQLException e) {
			handleSQLException("sorting products", e);
		}
	}

	private static void lowStockAlert(Scanner sc) {
		String query = "SELECT ProductID, Name, Category, Price, Stock FROM Product "
				+ "WHERE Stock <= ? ORDER BY Stock ASC";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query)) {

			int threshold = getValidInt(sc, "Enter stock threshold: ");
			pstmt.setInt(1, threshold);
			ResultSet rs = pstmt.executeQuery();

			System.out.println("\n⚠ LOW STOCK ALERT (Stock <= " + threshold + ") ⚠");
			System.out.printf("%-10s %-30s %-20s %-15s %-10s%n", "ID", "Name", "Category", "Price", "Stock");
			System.out.println("-".repeat(90));

			boolean hasData = false;
			while (rs.next()) {
				hasData = true;
				System.out.printf("%-10d %-30s %-20s ₹%-14.2f %-10d%n", rs.getInt("ProductID"), rs.getString("Name"),
						rs.getString("Category"), rs.getDouble("Price"), rs.getInt("Stock"));
			}

			if (!hasData) {
				System.out.println("✓ All products have sufficient stock!");
			}
			rs.close();

		} catch (SQLException e) {
			handleSQLException("checking low stock", e);
		}
	}

// ==================== CUSTOMER OPERATIONS ====================
	private static void handleCustomerOperations(Scanner sc) {
		System.out.println("\n--- Customer Management ---");
		System.out.println("1. Add Customer");
		System.out.println("2. View All Customers");
		System.out.println("3. Search by City");
		System.out.println("4. Delete Customer");
		System.out.println("5. Show top 3 customers of the year");
		System.out.println("0. Back");

		boolean back = false;
		while (!back) {
			int choice = getValidInt(sc, "Enter choice: ");

			switch (choice) {
			case 1:
				insertCustomer(sc);
				break;
			case 2:
				displayAllCustomers();
				break;
			case 3:
				searchCustomersByCity(sc);
				break;
			case 4:
				deleteCustomer(sc);
				break;
			case 5:
				showTop3CustomersByYear();
				break;
			case 0:
				back = true;
				break;
			default:
				System.out.println("Invalid choice.");

			}
		}
	}

	private static void insertCustomer(Scanner sc) {
		String query = "INSERT INTO Customer (CustomerID, Name, Gender, AgeGroup, City) VALUES (?, ?, ?, ?, ?)";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query)) {

			System.out.println("\n--- Add New Customer ---");
			int customerID = getValidInt(sc, "Enter Customer ID: ");
			sc.nextLine();
			System.out.print("Enter Name: ");
			String name = sc.nextLine();
			System.out.print("Enter Gender (M/F/O): ");
			String gender = sc.nextLine().toUpperCase();
			System.out.print("Enter Age Group (Teen/Adult/Senior): ");
			String ageGroup = sc.nextLine();
			System.out.print("Enter City: ");
			String city = sc.nextLine();

			pstmt.setInt(1, customerID);
			pstmt.setString(2, name);
			pstmt.setString(3, gender);
			pstmt.setString(4, ageGroup);
			pstmt.setString(5, city);

			int rowsAffected = pstmt.executeUpdate();
			if (rowsAffected > 0) {
				System.out.println("✓ Customer added successfully!");
			}

		} catch (SQLException e) {
			handleSQLException("inserting customer", e);
		}
	}

	private static void displayAllCustomers() {
		String query = "SELECT CustomerID, Name, Gender, AgeGroup, City FROM Customer ORDER BY CustomerID";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query);
				ResultSet rs = pstmt.executeQuery()) {

			System.out.println("\n" + "=".repeat(90));
			System.out.printf("%-10s %-25s %-10s %-15s %-20s%n", "ID", "Name", "Gender", "Age Group", "City");
			System.out.println("=".repeat(90));

			while (rs.next()) {
				System.out.printf("%-10d %-25s %-10s %-15s %-20s%n", rs.getInt("CustomerID"), rs.getString("Name"),
						rs.getString("Gender"), rs.getString("AgeGroup"), rs.getString("City"));
			}
			System.out.println("=".repeat(90));

		} catch (SQLException e) {
			handleSQLException("displaying customers", e);
		}
	}

	private static void searchCustomersByCity(Scanner sc) {
		String query = "SELECT CustomerID, Name, Gender, AgeGroup, City FROM Customer WHERE City = ?";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query)) {

			sc.nextLine();
			System.out.print("Enter City: ");
			String city = sc.nextLine();

			pstmt.setString(1, city);
			ResultSet rs = pstmt.executeQuery();

			System.out.println("\n--- Customers in " + city + " ---");
			System.out.printf("%-10s %-25s %-10s %-15s%n", "ID", "Name", "Gender", "Age Group");
			System.out.println("-".repeat(60));

			boolean hasData = false;
			while (rs.next()) {
				hasData = true;
				System.out.printf("%-10d %-25s %-10s %-15s%n", rs.getInt("CustomerID"), rs.getString("Name"),
						rs.getString("Gender"), rs.getString("AgeGroup"));
			}

			if (!hasData) {
				System.out.println("No customers found in " + city);
			}
			rs.close();

		} catch (SQLException e) {
			handleSQLException("searching customers", e);
		}
	}

	private static void deleteCustomer(Scanner sc) {
		String query = "DELETE FROM Customer WHERE CustomerID = ?";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query)) {

			int customerID = getValidInt(sc, "Enter Customer ID to delete: ");
			pstmt.setInt(1, customerID);

			int rowsAffected = pstmt.executeUpdate();
			if (rowsAffected > 0) {
				System.out.println("✓ Customer deleted successfully!");
			} else {
				System.out.println("✗ Customer ID not found.");
			}

		} catch (SQLException e) {
			handleSQLException("deleting customer", e);
		}
	}

	private static void showTop3CustomersByYear() {
		String query = """
				           SELECT *
				FROM (
				    SELECT

				        YEAR(b.PDate) AS Year,
				        b.CustomerID,
				        c.Name,
				        SUM(b.Qty) AS TotalQty,
				        RANK() OVER (PARTITION BY YEAR(b.PDate) ORDER BY SUM(b.Qty) DESC) AS RankPos
				    FROM Business b
				    JOIN Customer c ON b.CustomerID = c.CustomerID
				    GROUP BY YEAR(b.PDate), b.CustomerID, c.Name
				) ranked
				WHERE RankPos <= 3
				ORDER BY Year, RankPos;

				            """;

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query);
				ResultSet rs = pstmt.executeQuery()) {

			System.out.println("\n=== Top 3 Customers by Year ===");
			System.out.printf("%-6s %-10s %-25s %-10s%n", "Year", "CustID", "Name", "TotalQty");
			System.out.println("-".repeat(60));

			while (rs.next()) {
				System.out.printf("%-6d %-10d %-25s %-10d%n", rs.getInt("Year"), rs.getInt("CustomerID"),
						rs.getString("Name"), rs.getInt("TotalQty"));
			}

		} catch (SQLException e) {
			handleSQLException("fetching top customers", e);
		}
	}

	// ==================== CAMPAIGN OPERATIONS ====================
	private static void handleCampaignOperations(Scanner sc) {
		System.out.println("\n--- Campaign Management ---");
		System.out.println("1. Add Campaign");
		System.out.println("2. View All Campaigns");
		System.out.println("3. Delete Campaign");
		System.out.println("0. Back");
		boolean back = false;
		while (!back) {
			int choice = getValidInt(sc, "Enter choice: ");

			switch (choice) {
			case 1:
				insertCampaign(sc);
				break;
			case 2:
				displayAllCampaigns();
				break;
			case 3:
				deleteCampaign(sc);
				break;
			case 0:
				back = true;
				break;
			default:
				System.out.println("Invalid choice.");
			}
		}
	}

	private static void insertCampaign(Scanner sc) {
		String query = "INSERT INTO Campaign (CampaignID, Name, Type, Discount, StartDate, EndDate) "
				+ "VALUES (?, ?, ?, ?, ?, ?)";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query)) {

			System.out.println("\n--- Add New Campaign ---");
			int campaignID = getValidInt(sc, "Enter Campaign ID: ");
			sc.nextLine();
			System.out.print("Enter Campaign Name: ");
			String name = sc.nextLine();
			System.out.print("Enter Type (Seasonal/Promotional/Clearance/Loyalty): ");
			String type = sc.nextLine();
			double discount = getValidDouble(sc, "Enter Discount %: ");
			System.out.print("Enter Start Date (YYYY-MM-DD): ");
			String startDate = sc.next();
			System.out.print("Enter End Date (YYYY-MM-DD): ");
			String endDate = sc.next();

			pstmt.setInt(1, campaignID);
			pstmt.setString(2, name);
			pstmt.setString(3, type);
			pstmt.setDouble(4, discount);
			pstmt.setDate(5, Date.valueOf(startDate));
			pstmt.setDate(6, Date.valueOf(endDate));

			int rowsAffected = pstmt.executeUpdate();
			if (rowsAffected > 0) {
				System.out.println("✓ Campaign added successfully!");
			}

		} catch (SQLException e) {
			handleSQLException("inserting campaign", e);
		} catch (IllegalArgumentException e) {
			System.out.println("✗ Invalid date format. Use YYYY-MM-DD");
		}
	}

	private static void displayAllCampaigns() {
		String query = "SELECT CampaignID, Name, Type, Discount, StartDate, EndDate FROM Campaign";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query);
				ResultSet rs = pstmt.executeQuery()) {

			System.out.println("\n" + "=".repeat(100));
			System.out.printf("%-10s %-25s %-15s %-10s %-15s %-15s%n", "ID", "Name", "Type", "Discount%", "Start Date",
					"End Date");
			System.out.println("=".repeat(100));

			while (rs.next()) {
				System.out.printf("%-10d %-25s %-15s %-10.2f %-15s %-15s%n", rs.getInt("CampaignID"),
						rs.getString("Name"), rs.getString("Type"), rs.getDouble("Discount"), rs.getDate("StartDate"),
						rs.getDate("EndDate"));
			}
			System.out.println("=".repeat(100));

		} catch (SQLException e) {
			handleSQLException("displaying campaigns", e);
		}
	}

	private static void deleteCampaign(Scanner sc) {
		String query = "DELETE FROM Campaign WHERE CampaignID = ?";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query)) {

			int campaignID = getValidInt(sc, "Enter Campaign ID to delete: ");
			pstmt.setInt(1, campaignID);

			int rowsAffected = pstmt.executeUpdate();
			if (rowsAffected > 0) {
				System.out.println("✓ Campaign deleted successfully!");
			} else {
				System.out.println("✗ Campaign ID not found.");
			}

		} catch (SQLException e) {
			handleSQLException("deleting campaign", e);
		}
	}

	// ==================== BUSINESS OPERATIONS ====================
	private static void handleBusinessOperations(Scanner sc) {
		System.out.println("\n--- Business Transactions ---");
		System.out.println("1. Add Transaction");
		System.out.println("2. View All Transactions");
		System.out.println("3. Sales of Specific Product");
		System.out.println("0. Back");
		boolean back = false;
		while (!back) {
			int choice = getValidInt(sc, "Enter choice: ");

			switch (choice) {
			case 1:
				insertTransaction(sc);
				break;
			case 2:
				displayAllTransactions();
				break;
			case 3:
				salesOfProduct(sc);
				break;
			case 0:
				back = true;
				break;
			default:
				System.out.println("Invalid choice.");
			}
		}
	}

	private static void insertTransaction(Scanner sc) {
		String query = "INSERT INTO Business (ProductID, CustomerID, PDate, Qty) VALUES (?, ?, ?, ?)";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query)) {

			System.out.println("\n--- Add New Transaction ---");
			int productID = getValidInt(sc, "Enter Product ID: ");
			int customerID = getValidInt(sc, "Enter Customer ID: ");
			System.out.print("Enter Date (YYYY-MM-DD): ");
			String date = sc.next();
			int quantity = getValidInt(sc, "Enter Quantity: ");

			pstmt.setInt(1, productID);
			pstmt.setInt(2, customerID);
			pstmt.setDate(3, Date.valueOf(date));
			pstmt.setInt(4, quantity);

			int rowsAffected = pstmt.executeUpdate();
			if (rowsAffected > 0) {
				System.out.println("✓ Transaction added successfully!");
			}

		} catch (SQLException e) {
			handleSQLException("inserting transaction", e);
		} catch (IllegalArgumentException e) {
			System.out.println("✗ Invalid date format. Use YYYY-MM-DD");
		}
	}

	private static void displayAllTransactions() {
		String query = "SELECT b.ProductID, p.Name, b.CustomerID, c.Name as CustomerName, "
				+ "b.PDate, b.Qty, b.PAmount " + "FROM Business b " + "JOIN Product p ON b.ProductID = p.ProductID "
				+ "JOIN Customer c ON b.CustomerID = c.CustomerID " + "ORDER BY b.PDate DESC";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query);
				ResultSet rs = pstmt.executeQuery()) {

			System.out.println("\n" + "=".repeat(110));
			System.out.printf("%-10s %-25s %-12s %-25s %-15s %-10s %-15s%n", "ProdID", "Product", "CustID", "Customer",
					"Date", "Qty", "Amount");
			System.out.println("=".repeat(110));

			while (rs.next()) {
				System.out.printf("%-10d %-25s %-12d %-25s %-15s %-10d ₹%-14.2f%n", rs.getInt("ProductID"),
						rs.getString("Name"), rs.getInt("CustomerID"), rs.getString("CustomerName"),
						rs.getDate("PDate"), rs.getInt("Qty"), rs.getDouble("PAmount"));
			}
			System.out.println("=".repeat(110));

		} catch (SQLException e) {
			handleSQLException("displaying transactions", e);
		}
	}

	private static void salesOfProduct(Scanner sc) {
		String query = "SELECT p.ProductID, p.Name AS ProductName, " + "SUM(b.Qty) AS TotalSold, "
				+ "SUM(b.Qty * p.Price) AS TotalRevenue " + "FROM Product p "
				+ "JOIN Business b ON p.ProductID = b.ProductID " + "WHERE p.ProductID = ? "
				+ "GROUP BY p.ProductID, p.Name";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query)) {

			int productID = getValidInt(sc, "Enter Product ID: ");
			pstmt.setInt(1, productID);
			ResultSet rs = pstmt.executeQuery();

			if (rs.next()) {
				System.out.println("\n--- Sales Report ---");
				System.out.println("Product ID: " + rs.getInt("ProductID"));
				System.out.println("Product Name: " + rs.getString("ProductName"));
				System.out.println("Total Units Sold: " + rs.getInt("TotalSold"));
				System.out.println("Total Revenue: ₹" + String.format("%.2f", rs.getDouble("TotalRevenue")));
			} else {
				System.out.println("No sales data found for this product.");
			}
			rs.close();

		} catch (SQLException e) {
			handleSQLException("fetching product sales", e);
		}
	}

	// ==================== ANALYTICS OPERATIONS ====================
	private static void handleAnalyticsOperations(Scanner sc) {
		boolean back = false;
		while (!back) {
			displayAnalyticsMenu();
			int choice = getValidInt(sc, "Enter your choice: ");

			switch (choice) {
			case 1:
				topCustomersBySpending();
				break;
			case 2:
				revenueByCategory();
				break;
			case 3:
				revenueByPlatform();
				break;
			case 4:
				topSellingByRegion();
				break;
			case 5:
				campaignROI();
				break;
			case 6:
				avgPricePerCategory();
				break;
			case 7:
				totalQuantitySoldPerProduct();
				break;
			case 8:
				topSearchedByAgeGroup();
				break;
			case 0:
				back = true;
				break;
			default:
				System.out.println("Invalid choice.");
			}
		}
	}

	private static void topCustomersBySpending() {
		String query = "SELECT c.CustomerID, c.Name AS CustomerName, " + "SUM(b.Qty * p.Price) AS TotalSpent "
				+ "FROM Customer c " + "JOIN Business b ON c.CustomerID = b.CustomerID "
				+ "JOIN Product p ON b.ProductID = p.ProductID " + "GROUP BY c.CustomerID, c.Name "
				+ "ORDER BY TotalSpent DESC LIMIT 5";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query);
				ResultSet rs = pstmt.executeQuery()) {

			System.out.println("\n=== TOP 5 CUSTOMERS BY SPENDING ===");
			System.out.printf("%-15s %-30s %-20s%n", "Customer ID", "Name", "Total Spent");
			System.out.println("-".repeat(65));

			int rank = 1;
			while (rs.next()) {
				System.out.printf("#%-2d %-12d %-30s ₹%-18.2f%n", rank++, rs.getInt("CustomerID"),
						rs.getString("CustomerName"), rs.getDouble("TotalSpent"));
			}
			System.out.println("=".repeat(65));

		} catch (SQLException e) {
			handleSQLException("fetching top customers", e);
		}
	}

	private static void revenueByCategory() {
		String query = "SELECT p.Category, SUM(b.Qty * p.Price) AS TotalRevenue " + "FROM Product p "
				+ "JOIN Business b ON p.ProductID = b.ProductID " + "GROUP BY p.Category "
				+ "ORDER BY TotalRevenue DESC";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query);
				ResultSet rs = pstmt.executeQuery()) {

			System.out.println("\n=== REVENUE BY CATEGORY ===");
			System.out.printf("%-30s %-20s%n", "Category", "Total Revenue");
			System.out.println("-".repeat(50));

			double grandTotal = 0;
			while (rs.next()) {
				double revenue = rs.getDouble("TotalRevenue");
				grandTotal += revenue;
				System.out.printf("%-30s ₹%-18.2f%n", rs.getString("Category"), revenue);
			}
			System.out.println("-".repeat(50));
			System.out.printf("%-30s ₹%-18.2f%n", "GRAND TOTAL", grandTotal);
			System.out.println("=".repeat(50));

		} catch (SQLException e) {
			handleSQLException("fetching revenue by category", e);
		}
	}

	private static void revenueByPlatform() {
		String query = "SELECT Platform, SUM(Revenue) AS Total_Revenue " + "FROM Ads " + "GROUP BY Platform "
				+ "ORDER BY Total_Revenue DESC";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query);
				ResultSet rs = pstmt.executeQuery()) {

			System.out.println("\n=== REVENUE BY ADVERTISING PLATFORM ===");
			System.out.printf("%-20s %-20s%n", "Platform", "Total Revenue");
			System.out.println("-".repeat(40));

			while (rs.next()) {
				System.out.printf("%-20s ₹%-18.2f%n", rs.getString("Platform"), rs.getDouble("Total_Revenue"));
			}
			System.out.println("=".repeat(40));

		} catch (SQLException e) {
			handleSQLException("fetching revenue by platform", e);
		}
	}

	private static void topSellingByRegion() {
		String query = "SELECT RegionName, ProductName, TotalSold FROM ("
				+ "SELECT r.Region AS RegionName, p.Name AS ProductName, " + "SUM(b.Qty) AS TotalSold, "
				+ "RANK() OVER (PARTITION BY r.Region ORDER BY SUM(b.Qty) DESC) AS rank_in_region "
				+ "FROM Regional_info r " + "JOIN Customer c ON r.City = c.City "
				+ "JOIN Business b ON b.CustomerID = c.CustomerID " + "JOIN Product p ON b.ProductID = p.ProductID "
				+ "GROUP BY r.Region, p.Name) ranked " + "WHERE rank_in_region <= 5";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query);
				ResultSet rs = pstmt.executeQuery()) {

			System.out.println("\n=== TOP SELLING PRODUCTS BY REGION ===");
			System.out.printf("%-20s %-30s %-15s%n", "Region", "Product", "Total Sold");
			System.out.println("-".repeat(65));

			String currentRegion = "";
			while (rs.next()) {
				String region = rs.getString("RegionName");
				if (!region.equals(currentRegion)) {
					if (!currentRegion.isEmpty())
						System.out.println();
					currentRegion = region;
				}
				System.out.printf("%-20s %-30s %-15d%n", region, rs.getString("ProductName"), rs.getInt("TotalSold"));
			}
			System.out.println("=".repeat(65));

		} catch (SQLException e) {
			handleSQLException("fetching top selling products", e);
		}
	}

	private static void campaignROI() {
		String query = "SELECT c.CampaignID, c.Name AS CampaignName, "
				+ "SUM(a.Revenue) AS TotalRevenue, SUM(a.Cost) AS TotalCost, "
				+ "ROUND((SUM(a.Revenue) - SUM(a.Cost)) / NULLIF(SUM(a.Cost), 0) * 100, 2) AS ROI " + "FROM Campaign c "
				+ "JOIN Ads_Campaign ac ON c.CampaignID = ac.CampaignID "
				+ "JOIN Ads a ON a.AdsID = ac.AdsID AND a.Platform = ac.Platform " + "GROUP BY c.CampaignID, c.Name "
				+ "ORDER BY ROI DESC LIMIT 5";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query);
				ResultSet rs = pstmt.executeQuery()) {

			System.out.println("\n=== TOP 5 CAMPAIGNS BY ROI ===");
			System.out.printf("%-10s %-25s %-15s %-15s %-10s%n", "ID", "Campaign", "Revenue", "Cost", "ROI %");
			System.out.println("-".repeat(75));

			while (rs.next()) {
				System.out.printf("%-10d %-25s ₹%-13.2f ₹%-13.2f %-10.2f%%%n", rs.getInt("CampaignID"),
						rs.getString("CampaignName"), rs.getDouble("TotalRevenue"), rs.getDouble("TotalCost"),
						rs.getDouble("ROI"));
			}
			System.out.println("=".repeat(75));

		} catch (SQLException e) {
			handleSQLException("fetching campaign ROI", e);
		}
	}

	private static void avgPricePerCategory() {
		String query = "SELECT Category, ROUND(AVG(Price), 2) AS AvgPrice " + "FROM Product " + "GROUP BY Category "
				+ "ORDER BY AvgPrice DESC";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query);
				ResultSet rs = pstmt.executeQuery()) {

			System.out.println("\n=== AVERAGE PRICE PER CATEGORY ===");
			System.out.printf("%-30s %-20s%n", "Category", "Average Price");
			System.out.println("-".repeat(50));

			while (rs.next()) {
				System.out.printf("%-30s ₹%-18.2f%n", rs.getString("Category"), rs.getDouble("AvgPrice"));
			}
			System.out.println("=".repeat(50));

		} catch (SQLException e) {
			handleSQLException("fetching average prices", e);
		}
	}

	private static void totalQuantitySoldPerProduct() {
		String query = "SELECT p.ProductID, p.Name AS ProductName, SUM(b.Qty) AS TotalSold " + "FROM Product p "
				+ "JOIN Business b ON p.ProductID = b.ProductID " + "GROUP BY p.ProductID, p.Name "
				+ "ORDER BY TotalSold DESC";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query);
				ResultSet rs = pstmt.executeQuery()) {

			System.out.println("\n=== TOTAL QUANTITY SOLD PER PRODUCT ===");
			System.out.printf("%-15s %-35s %-15s%n", "Product ID", "Product Name", "Total Sold");
			System.out.println("-".repeat(65));

			while (rs.next()) {
				System.out.printf("%-15d %-35s %-15d%n", rs.getInt("ProductID"), rs.getString("ProductName"),
						rs.getInt("TotalSold"));
			}
			System.out.println("=".repeat(65));

		} catch (SQLException e) {
			handleSQLException("fetching quantity sold", e);
		}
	}

	private static void topSearchedByAgeGroup() {
		String query = "SELECT * FROM (" + "SELECT a.AgeGroup, p.ProductID, p.Name AS ProductName, "
				+ "SUM(a.Impressions) AS Total_Impressions, "
				+ "RANK() OVER (PARTITION BY a.AgeGroup ORDER BY SUM(a.Impressions) DESC) AS rnk " + "FROM Ads a "
				+ "JOIN Products_Ads pa ON a.AdsID = pa.AdsID AND a.Platform = pa.Platform "
				+ "JOIN Product p ON pa.ProductID = p.ProductID " + "GROUP BY a.AgeGroup, p.ProductID, p.Name) ranked "
				+ "WHERE rnk = 1";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query);
				ResultSet rs = pstmt.executeQuery()) {

			System.out.println("\n=== TOP SEARCHED PRODUCTS BY AGE GROUP ===");
			System.out.printf("%-15s %-35s %-20s%n", "Age Group", "Product Name", "Total Impressions");
			System.out.println("-".repeat(70));

			while (rs.next()) {
				System.out.printf("%-15s %-35s %-20d%n", rs.getString("AgeGroup"), rs.getString("ProductName"),
						rs.getInt("Total_Impressions"));
			}
			System.out.println("=".repeat(70));

		} catch (SQLException e) {
			handleSQLException("fetching top searched products", e);
		}
	}

	// ==================== AD OPERATIONS ====================
	private static void handleAdOperations(Scanner sc) {
		System.out.println("\n--- Advertisement Management ---");
		System.out.println("1. Add Advertisement");
		System.out.println("2. View All Ads");
		System.out.println("3. Ads Running at Loss (ROI < 0)");
		System.out.println("0. Back");
		boolean back = false;
		while (!back) {
			int choice = getValidInt(sc, "Enter choice: ");

			switch (choice) {
			case 1:
				insertAd(sc);
				break;
			case 2:
				displayAllAds();
				break;
			case 3:
				adsAtLoss();
				break;
			case 0:
				back = true;
				break;
			default:
				System.out.println("Invalid choice.");
			}
		}
	}

	private static void insertAd(Scanner sc) {
		String query = "INSERT INTO Ads (AdsID, Platform, AgeGroup, Impressions, Conversions, Revenue, Cost) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?)";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query)) {

			System.out.println("\n--- Add New Advertisement ---");
			int adsID = getValidInt(sc, "Enter Ad ID: ");
			sc.nextLine();
			System.out.print("Enter Platform (Instagram/YouTube/Google): ");
			String platform = sc.nextLine();
			System.out.print("Enter Age Group (Teen/Adult/Senior): ");
			String ageGroup = sc.nextLine();
			int impressions = getValidInt(sc, "Enter Impressions: ");
			int conversions = getValidInt(sc, "Enter Conversions: ");
			double revenue = getValidDouble(sc, "Enter Revenue: ");
			double cost = getValidDouble(sc, "Enter Cost: ");

			pstmt.setInt(1, adsID);
			pstmt.setString(2, platform);
			pstmt.setString(3, ageGroup);
			pstmt.setInt(4, impressions);
			pstmt.setInt(5, conversions);
			pstmt.setDouble(6, revenue);
			pstmt.setDouble(7, cost);

			int rowsAffected = pstmt.executeUpdate();
			if (rowsAffected > 0) {
				System.out.println("✓ Advertisement added successfully!");
			}

		} catch (SQLException e) {
			handleSQLException("inserting ad", e);
		}
	}

	private static void displayAllAds() {
		String query = "SELECT AdsID, Platform, AgeGroup, Impressions, Conversions, Revenue, Cost, "
				+ "ROUND((Revenue - Cost) / NULLIF(Cost, 0) * 100, 2) AS ROI " + "FROM Ads ORDER BY AdsID";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query);
				ResultSet rs = pstmt.executeQuery()) {

			System.out.println("\n" + "=".repeat(120));
			System.out.printf("%-8s %-12s %-12s %-12s %-12s %-15s %-15s %-10s%n", "AdID", "Platform", "AgeGroup",
					"Impressions", "Conversions", "Revenue", "Cost", "ROI %");
			System.out.println("=".repeat(120));

			while (rs.next()) {
				System.out.printf("%-8d %-12s %-12s %-12d %-12d ₹%-13.2f ₹%-13.2f %-10.2f%n", rs.getInt("AdsID"),
						rs.getString("Platform"), rs.getString("AgeGroup"), rs.getInt("Impressions"),
						rs.getInt("Conversions"), rs.getDouble("Revenue"), rs.getDouble("Cost"), rs.getDouble("ROI"));
			}
			System.out.println("=".repeat(120));

		} catch (SQLException e) {
			handleSQLException("displaying ads", e);
		}
	}

	private static void adsAtLoss() {
		String query = "SELECT AdsID, Platform, Revenue, Cost, "
				+ "ROUND((Revenue - Cost) / NULLIF(Cost, 0) * 100, 2) AS ROI " + "FROM Ads " + "WHERE Revenue < Cost "
				+ "ORDER BY ROI ASC";

		try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
				PreparedStatement pstmt = conn.prepareStatement(query);
				ResultSet rs = pstmt.executeQuery()) {

			System.out.println("\n⚠ ADS RUNNING AT LOSS (ROI < 0) ⚠");
			System.out.printf("%-10s %-15s %-15s %-15s %-10s%n", "Ad ID", "Platform", "Revenue", "Cost", "ROI %");
			System.out.println("-".repeat(65));

			boolean hasData = false;
			while (rs.next()) {
				hasData = true;
				System.out.printf("%-10d %-15s ₹%-13.2f ₹%-13.2f %-10.2f%n", rs.getInt("AdsID"),
						rs.getString("Platform"), rs.getDouble("Revenue"), rs.getDouble("Cost"), rs.getDouble("ROI"));
			}

			if (!hasData) {
				System.out.println("✓ No ads running at loss!");
			}
			System.out.println("=".repeat(65));

		} catch (SQLException e) {
			handleSQLException("fetching loss-making ads", e);
		}
	}

	// ==================== UTILITY METHODS ====================
	private static int getValidInt(Scanner sc, String prompt) {
		while (true) {
			try {
				System.out.print(prompt);
				return sc.nextInt();
			} catch (Exception e) {
				System.out.println("✗ Invalid input. Please enter a valid number.");
				sc.nextLine(); // clear buffer
			}
		}
	}

	private static double getValidDouble(Scanner sc, String prompt) {
		while (true) {
			try {
				System.out.print(prompt);
				return sc.nextDouble();
			} catch (Exception e) {
				System.out.println("✗ Invalid input. Please enter a valid number.");
				sc.nextLine(); // clear buffer
			}
		}
	}

	private static void handleSQLException(String operation, SQLException e) {
		System.out.println("\n✗ Error while " + operation + ":");
		System.out.println("SQL State: " + e.getSQLState());
		System.out.println("Error Code: " + e.getErrorCode());
		System.out.println("Message: " + e.getMessage());

		// Provide helpful hints
		if (e.getMessage().contains("Duplicate entry")) {
			System.out.println("Hint: Record with this ID already exists.");
		} else if (e.getMessage().contains("foreign key constraint")) {
			System.out.println("Hint: Referenced ID does not exist in the related table.");
		} else if (e.getMessage().contains("doesn't exist")) {
			System.out.println("Hint: Table might not exist. Check database schema.");
		}
		System.out.println();
	}
}
