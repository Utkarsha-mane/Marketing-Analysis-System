import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;


//  Console-based interactive marketing dashboard.
//  Gate: marketing-team login required before any menu is shown.

public class MarketingDashboard {

    private static final Scanner sc = new Scanner(System.in);
    private static final CampaignDAO campaignDAO = new CampaignDAO();
    private static final AdDAO adDAO = new AdDAO();
    private static final CampaignProductDAO cpDAO = new CampaignProductDAO();
    private static final ReportDAO reportDAO = new ReportDAO();

    public static void main(String[] args) {
        if (!authenticate()) {
            System.out.println("Access denied. This dashboard is restricted to the marketing team.");
            return;
        }
        mainMenu();
    }

    private static boolean authenticate() {
        System.out.print("Username: ");
        String user = sc.nextLine().trim();
        System.out.print("Password: ");
        String pass = sc.nextLine().trim();
        try {
            return new LoginDAO().login(user, pass);
        } catch (SQLException e) {
            System.out.println("Login error: " + e.getMessage());
            return false;
        }
    }

    private static void mainMenu() {
        while (true) {
            System.out.println("\n===== MARKETING DASHBOARD =====");
            System.out.println("1. Campaign management");
            System.out.println("2. Ad management");
            System.out.println("3. Campaign products & pricing");
            System.out.println("4. Reports & analytics");
            System.out.println("0. Exit");
            System.out.print("Choose: ");
            String choice = sc.nextLine().trim();
            try {
                switch (choice) {
                    case "1": campaignMenu(); break;
                    case "2": adMenu(); break;
                    case "3": campaignProductMenu(); break;
                    case "4": reportsMenu(); break;
                    case "0": System.out.println("Goodbye."); return;
                    default: System.out.println("Invalid option.");
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }
    }

    // Campaign Menu 

    private static void campaignMenu() throws SQLException {
        System.out.println("\n-- Campaigns --");
        System.out.println("1. Create new campaign");
        System.out.println("2. List campaigns");
        System.out.println("3. Campaign performance (ROI/spend/CTR)");
        System.out.println("4. Update campaign budget");
        System.out.print("Choose: ");
        String choice = sc.nextLine().trim();

        switch (choice) {
            case "1": {
                System.out.print("PlatformID: ");
                int platformId = Integer.parseInt(sc.nextLine().trim());
                System.out.print("Start date (YYYY-MM-DD): ");
                LocalDate start = LocalDate.parse(sc.nextLine().trim());
                System.out.print("End date (YYYY-MM-DD): ");
                LocalDate end = LocalDate.parse(sc.nextLine().trim());
                System.out.print("Budget: ");
                BigDecimal budget = new BigDecimal(sc.nextLine().trim());
                System.out.print("Objective: ");
                String objective = sc.nextLine().trim();

                int newId = campaignDAO.createCampaign(platformId, start, end, budget, objective);
                System.out.println("Created CampaignID = " + newId);
                break;
            }
            case "2":
                printAll(campaignDAO.listCampaigns());
                break;
            case "3":
                printAll(campaignDAO.getCampaignPerformance());
                break;
            case "4": {
                System.out.print("CampaignID: ");
                int id = Integer.parseInt(sc.nextLine().trim());
                System.out.print("New budget: ");
                BigDecimal budget = new BigDecimal(sc.nextLine().trim());
                campaignDAO.updateCampaignBudget(id, budget);
                System.out.println("Budget updated.");
                break;
            }
            default:
                System.out.println("Invalid option.");
        }
    }

    // Ad Menu

    private static void adMenu() throws SQLException {
        System.out.println("\n-- Ads --");
        System.out.println("1. Create new ad");
        System.out.println("2. Update ad impressions/clicks");
        System.out.println("3. Ad performance (all / by campaign)");
        System.out.print("Choose: ");
        String choice = sc.nextLine().trim();

        switch (choice) {
            case "1": {
                System.out.print("CampaignID: ");
                int campaignId = Integer.parseInt(sc.nextLine().trim());
                System.out.print("Ad name: ");
                String name = sc.nextLine().trim();
                System.out.print("Impressions: ");
                int impressions = Integer.parseInt(sc.nextLine().trim());
                System.out.print("Clicks: ");
                int clicks = Integer.parseInt(sc.nextLine().trim());
                System.out.print("Cost per ad: ");
                BigDecimal cost = new BigDecimal(sc.nextLine().trim());

                String[] reason = new String[1];
                int newId = adDAO.createAd(campaignId, name, impressions, clicks, cost, reason);
                if (newId == -1) {
                    System.out.println("Could not create ad: " + reason[0]);
                } else {
                    System.out.println("Created AdID = " + newId);
                }
                break;
            }
            case "2": {
                System.out.print("AdID: ");
                int adId = Integer.parseInt(sc.nextLine().trim());
                System.out.print("New impressions: ");
                int impressions = Integer.parseInt(sc.nextLine().trim());
                System.out.print("New clicks: ");
                int clicks = Integer.parseInt(sc.nextLine().trim());
                adDAO.updateAdMetrics(adId, impressions, clicks);
                System.out.println("Ad metrics updated and recalculated.");
                break;
            }
            case "3": {
                System.out.print("Filter by CampaignID (blank for all): ");
                String input = sc.nextLine().trim();
                Integer campaignId = input.isEmpty() ? null : Integer.parseInt(input);
                printAll(adDAO.getAdPerformance(campaignId));
                break;
            }
            default:
                System.out.println("Invalid option.");
        }
    }

    // Campaign Product Menu

    private static void campaignProductMenu() throws SQLException {
        System.out.println("\n-- Campaign Products --");
        System.out.println("1. Add product to campaign (with discount)");
        System.out.println("2. Update discount");
        System.out.println("3. List products in a campaign");
        System.out.print("Choose: ");
        String choice = sc.nextLine().trim();

        switch (choice) {
            case "1": {
                System.out.print("CampaignID: ");
                int campaignId = Integer.parseInt(sc.nextLine().trim());
                System.out.print("ProductID: ");
                int productId = Integer.parseInt(sc.nextLine().trim());
                System.out.print("Discount %: ");
                BigDecimal discount = new BigDecimal(sc.nextLine().trim());
                cpDAO.addProductToCampaign(campaignId, productId, discount);
                System.out.println("Product added to campaign (selling price computed by trigger).");
                break;
            }
            case "2": {
                System.out.print("CampaignID: ");
                int campaignId = Integer.parseInt(sc.nextLine().trim());
                System.out.print("ProductID: ");
                int productId = Integer.parseInt(sc.nextLine().trim());
                System.out.print("New discount %: ");
                BigDecimal discount = new BigDecimal(sc.nextLine().trim());
                cpDAO.updateDiscount(campaignId, productId, discount);
                System.out.println("Discount updated.");
                break;
            }
            case "3": {
                System.out.print("CampaignID: ");
                int campaignId = Integer.parseInt(sc.nextLine().trim());
                printAll(cpDAO.listCampaignProducts(campaignId));
                break;
            }
            default:
                System.out.println("Invalid option.");
        }
    }

    // Report Menu

    private static void reportsMenu() throws SQLException {
        System.out.println("\n-- Reports & Analytics --");
        System.out.println("1. Region performance");
        System.out.println("2. Segment performance");
        System.out.println("3. Platform performance");
        System.out.println("4. Product performance");
        System.out.println("5. Campaign-by-segment breakdown");
        System.out.println("6. Budget alerts (campaigns near budget limit)");
        System.out.println("7. Campaigns ending soon");
        System.out.println("8. Top ads by ROAS");
        System.out.println("9. Underperforming ads (low CTR)");
        System.out.println("10. Segment-platform affinity");
        System.out.print("Choose: ");
        String choice = sc.nextLine().trim();

        switch (choice) {
            case "1": printAll(reportDAO.regionPerformance()); break;
            case "2": printAll(reportDAO.segmentPerformance()); break;
            case "3": printAll(reportDAO.platformPerformance()); break;
            case "4": printAll(reportDAO.productPerformance()); break;
            case "5": {
                System.out.print("CampaignID: ");
                int id = Integer.parseInt(sc.nextLine().trim());
                printAll(reportDAO.campaignBySegment(id));
                break;
            }
            case "6": {
                System.out.print("Alert threshold %% (e.g. 85): ");
                double pct = Double.parseDouble(sc.nextLine().trim());
                printAll(reportDAO.budgetAlerts(pct));
                break;
            }
            case "7": {
                System.out.print("Within how many days: ");
                int days = Integer.parseInt(sc.nextLine().trim());
                printAll(reportDAO.campaignsEndingSoon(days));
                break;
            }
            case "8": {
                System.out.print("Top N: ");
                int n = Integer.parseInt(sc.nextLine().trim());
                printAll(reportDAO.topAdsByROAS(n));
                break;
            }
            case "9": printAll(reportDAO.underperformingAds()); break;
            case "10": printAll(reportDAO.segmentPlatformAffinity()); break;
            default: System.out.println("Invalid option.");
        }
    }

    private static void printAll(List<String> rows) {
        if (rows.isEmpty()) {
            System.out.println("(no rows)");
            return;
        }
        rows.forEach(System.out::println);
    }
}
