# Marketing Analysis Dashboard

A JDBC-backed decision-support tool for a marketing team to plan campaigns,
manage ads and product pricing, and pull ROI/ROAS analytics — with all
business rules (budget caps, selling-price computation, metric recalculation)
enforced at the database layer, not in application code.

Two front ends ship against the same DAO/database layer:

| Front end | File | Use case |
|---|---|---|
| Console (CLI) | `MarketingDashboard.java` | scripting, headless environments, fast keyboard-driven use |
| Desktop GUI (Swing) | `MarketingDashboardGUI.java` | day-to-day use by non-technical marketing staff |

---

## 1. Why this exists

Marketing teams routinely need answers to three questions that span multiple
tables and require aggregation the application layer shouldn't be trusted to
get right on its own:

1. **Are we about to blow a campaign's budget?** — budget consumption has to
   be validated *at the moment of spend*, not after the fact in a report.
2. **Which ads and campaigns are actually paying for themselves?** — ROAS,
   ROI, CTR, and conversion rate need to be computed consistently every time,
   not recalculated ad hoc in five different places.
3. **Which customer segments respond on which platform?** — targeting
   decisions need cross-table joins that shouldn't be handwritten in the UI.

This project pushes those three concerns into the database itself (triggers
for validation, views for computed metrics, a stored procedure for
recalculation) and keeps the Java layer thin: DAOs issue SQL and shape
results, the UI displays them. That split is deliberate — it's the same
reason the budget check can't be bypassed by any client, present or future.

---

## 2. Architecture

<img width="2720" height="1936" alt="marketing_dashboard_architecture" src="https://github.com/user-attachments/assets/211ed32c-aae3-4afb-b3fc-43b51439c15c" />



---

## 3. Tech stack

| Layer | Technology |
|---|---|
| Language | Java |
| Desktop UI | Swing |
| Database | MySQL |
| Auth | SHA-256 |

---

## 4. Module reference

### 4.1 `DBConnection.java` — connection factory
Single point of contact with the database. Every DAO opens a connection via
`DBConnection.getConnection()` and closes it in a try-with-resources block —
no connection is ever held open across menu operations. The URL, user, and
password are currently constants at the top of the file; see §9 for why that
should change before this goes anywhere near production.

### 4.2 `LoginDAO.java` — authentication gate
Restricts the entire application to users with `Role = 'MARKETING'` in the
`MarketingUser` table. Passwords are never stored or compared as plaintext —
see §6 for the full SHA-256 walkthrough. `login()` returns a boolean; a
missing username and a wrong password are indistinguishable to the caller
(intentional — it avoids leaking which usernames exist).

### 4.3 `CampaignDAO.java` — campaign lifecycle
- `createCampaign()` — inserts a new campaign with `CurrentBudget` forced to
  `0` at creation (spend always starts from zero, never inherited).
- `updateCampaignBudget()` — raises or lowers the budget ceiling.
- `listCampaigns()` — raw campaign listing (ID, dates, budget vs. spend).
- `getCampaignPerformance()` — reads `vw_CampaignPerformance` for
  spend, budget utilization %, revenue, ROI %, and ad count in one query.

### 4.4 `AdDAO.java` — ad creation and performance
- `createAd()` — inserts an ad with `ConversionRate` and `AttributedRevenue`
  initialized to `0`. If the insert would push the parent campaign over
  budget, `trg_CheckCampaignBudget_Insert` raises SQLSTATE `45000`; the DAO
  catches that specific state, surfaces the trigger's message, and returns
  `-1` instead of throwing — the caller gets a clean "why" instead of a raw
  `SQLException`.
- `updateAdMetrics()` — updates impressions/clicks, then calls the
  `sp_RecalcAdMetrics` stored procedure so `ConversionRate` and
  `AttributedRevenue` are recomputed **inside the database**, guaranteeing
  the same recalculation logic runs whether the update came from the CLI,
  the GUI, or a future batch import job.
- `getAdPerformance()` — reads `vw_AdPerformance` (CTR, conversion rate,
  cost-per-click, ROAS, revenue), optionally filtered to one campaign,
  ranked by ROAS descending.

### 4.5 `CampaignProductDAO.java` — pricing
- `addProductToCampaign()` — links a product to a campaign with a discount
  percentage; `SellingPrice` is inserted as `NULL` and computed by
  `trg_CampaignProduct_SellingPrice_Insert` from `MRP` and `Discount` — the
  DAO never calculates a price itself, so pricing logic has exactly one home.
- `updateDiscount()` — changes the discount for an existing campaign/product
  pair (the trigger's insert-time computation means this path currently
  updates `Discount` only; see §9 for the one caveat worth knowing about).
- `listCampaignProducts()` — joined listing of product name, MRP, discount,
  and computed selling price for a campaign.

### 4.6 `ReportDAO.java` — analytics and alerting
Ten read-only reports, each backed by a view or a small aggregation query:

| Method | What it answers |
|---|---|
| `regionPerformance()` | Revenue/orders/ROI by region |
| `segmentPerformance()` | Revenue/orders/ROI by customer segment |
| `platformPerformance()` | Revenue/orders/ROI by ad platform |
| `productPerformance()` | Revenue generated per product |
| `campaignBySegment(id)` | Which segments bought from one specific campaign |
| `budgetAlerts(pct)` | Campaigns at or above a budget-utilization threshold |
| `campaignsEndingSoon(days)` | Campaigns ending within N days, soonest first |
| `topAdsByROAS(limit)` | Best-performing ads by return on ad spend |
| `underperformingAds()` | Ads whose CTR is under half their own campaign's average CTR |
| `segmentPlatformAffinity()` | Which segment converts best on which platform |

Two private helpers, `runSimple(sql)` and `runSimple(sql, RowFormatter)`,
remove the boilerplate of looping a `ResultSet` into `List<String>` — the
first auto-formats every column generically (used for straightforward view
reads), the second takes a formatter lambda for reports that need custom
column labels or derived values (e.g. `underperformingAds()`, which prints
both an ad's CTR and its campaign's average CTR side by side).

### 4.7 `MarketingDashboard.java` — console front end
Gates on `LoginDAO.login()` before showing anything. A `Scanner`-driven,
numbered-menu loop (`mainMenu()` → `campaignMenu()` / `adMenu()` /
`campaignProductMenu()` / `reportsMenu()`) that parses user input, calls the
matching DAO method, and prints results with `printAll()`. Every menu
operation is wrapped so a `SQLException` prints a message and returns to the
menu instead of crashing the process.

### 4.8 `MarketingDashboardGUI.java` — Swing front end
Same DAO layer, a graphical shell:
- `LoginFrame` — username/password form gating entry, mirrors `LoginDAO`
  exactly (same trigger/exception handling path as the CLI).
- `MainFrame` — a `JTabbedPane` with **Campaigns**, **Ads**, **Campaign
  Products**, and **Reports** tabs.
- `CampaignPanel` / `AdPanel` / `CampaignProductPanel` — each pairs a
  `GridBagLayout` form for writes with a `JTable` for the corresponding
  listing/performance read, so an action's result is visible immediately.
- `ReportsPanel` — a `JComboBox` of all ten reports from `ReportDAO`, with a
  parameter field that shows/hides itself depending on which report needs
  one (campaign ID, threshold %, day count, or top-N).
- Shared helpers: `populateTable()` parses the DAOs' `"Key=Value | Key=Value"`
  string format into `JTable` columns generically (no per-report table
  model), and `error()`/`info()` wrap `JOptionPane` dialogs.

---

## 5. Database-enforced business rules

These are not optional application checks — they run inside MySQL and hold
regardless of which client issues the write:

| Rule | Enforced by | Effect |
|---|---|---|
| An ad's cost cannot push its campaign's spend past its budget | `trg_CheckCampaignBudget_Insert` (trigger) | Insert is rejected with `SIGNAL SQLSTATE '45000'`; `AdDAO.createAd()` catches this and returns `-1` with the trigger's message |
| A campaign product's selling price must equal MRP net of its discount | `trg_CampaignProduct_SellingPrice_Insert` (trigger) | `SellingPrice` is always insert-time computed, never client-supplied |
| An ad's conversion rate and attributed revenue must reflect its current impressions/clicks | `sp_RecalcAdMetrics` (stored procedure) | Called explicitly by `AdDAO.updateAdMetrics()` after every metrics update |

---

## 6. Authentication: SHA-256 password hashing

`LoginDAO` never stores or compares a plaintext password. It stores a
**SHA-256 digest** and compares digests instead.

**At a glance:** SHA-256 takes an input of any length and deterministically
produces a fixed 256-bit (32-byte) output, rendered here as 64 hex
characters. It is a member of the SHA-2 family, specified in FIPS 180-4, and
built on the **Merkle–Damgård construction**:

1. **Pad** — the password (as UTF-8 bytes) is padded with a `1` bit, then
   zero bits, then a 64-bit field encoding the original message length, until
   the total length is a multiple of 512 bits.
2. **Split** — the padded message is divided into 512-bit blocks.
3. **Compress** — each block runs through 64 rounds of bitwise rotations,
   shifts, and modular additions (mixing in 64 fixed constants derived from
   the fractional parts of the cube roots of the first 64 primes) that
   update eight 32-bit working variables, seeded initially from the
   fractional parts of the square roots of the first 8 primes.
4. **Chain** — the working variables' state after one block feeds into the
   next block, so the final digest depends on the entire input, not just the
   last block processed.
5. **Output** — after the last block, the eight 32-bit working variables are
   concatenated into the 256-bit digest and hex-encoded.

```java
MessageDigest digest = MessageDigest.getInstance("SHA-256");
byte[] bytes = digest.digest(input.getBytes("UTF-8"));
// bytes.length == 32; formatted as 64 lowercase hex chars below
```

### 6.1 The mathematics of the compression function


**Initial hash values** `H0..H7` — the first 32 bits of the fractional parts
of the square roots of the first 8 primes (2, 3, 5, 7, 11, 13, 17, 19):

```
H0 = 6a09e667   H1 = bb67ae85   H2 = 3c6ef372   H3 = a54ff53a
H4 = 510e527f   H5 = 9b05688c   H6 = 1f83d9ab   H7 = 5be0cd19
```

**Round constants** `K0..K63` — the first 32 bits of the fractional parts of
the cube roots of the first 64 primes (2 .. 311). These are mixed into every
one of the 64 rounds per block, e.g.:

```
K0 = 428a2f98   K1 = 71374491   K2 = b5c0fbcf   K3 = e9b5dba5   ...   K63 = c67178f2
```

**Message schedule.** Each 512-bit block is split into sixteen 32-bit words
`W0..W15`. The schedule is then extended to 64 words:

```
W_t = σ1(W_{t-2}) + W_{t-7} + σ0(W_{t-15}) + W_{t-16}      for t = 16 .. 63

where
σ0(x) = ROTR^7(x)  ⊕ ROTR^18(x) ⊕ SHR^3(x)
σ1(x) = ROTR^17(x) ⊕ ROTR^19(x) ⊕ SHR^10(x)
```

**Compression — one round `t` (0 ≤ t ≤ 63), working variables `a..h`:**

```
Σ0(a) = ROTR^2(a)  ⊕ ROTR^13(a) ⊕ ROTR^22(a)
Σ1(e) = ROTR^6(e)  ⊕ ROTR^11(e) ⊕ ROTR^25(e)

Ch(e,f,g)  = (e ∧ f) ⊕ (¬e ∧ g)          # "choose": e picks bits from f or g
Maj(a,b,c) = (a ∧ b) ⊕ (a ∧ c) ⊕ (b ∧ c)  # "majority" bit of a, b, c

T1 = h + Σ1(e) + Ch(e,f,g) + K_t + W_t
T2 = Σ0(a) + Maj(a,b,c)

h = g;  g = f;  f = e;  e = d + T1
d = c;  c = b;  b = a;  a = T1 + T2
```

`a..h` are seeded from `H0..H7` (or the previous block's output) before
round 0, and run through all 64 rounds per 512-bit block.

**Feed-forward / block output.** After round 63, each working variable is
added back into the hash state carried in from the previous block:

```
H0 = H0 + a   H1 = H1 + b   H2 = H2 + c   H3 = H3 + d
H4 = H4 + e   H5 = H5 + f   H6 = H6 + g   H7 = H7 + h
```

**Final digest.** After the last block, concatenate the eight 32-bit words:

```
digest = H0 ‖ H1 ‖ H2 ‖ H3 ‖ H4 ‖ H5 ‖ H6 ‖ H7        (256 bits total)
```

That 256-bit value, hex-encoded, is exactly what `sha256()` returns and what
`PasswordHash` stores.

**Why this specific mix of operations matters for security:** `Ch` and `Maj`
are non-linear (they mix bits based on the *values* of other bits, not just
fixed positions), so the transformation can't be reduced to simple linear
algebra over the input; `Σ0`/`Σ1`/`σ0`/`σ1` diffuse each bit across many
positions through rotation (never a straight shift alone, which would lose
information); and the modular additions combine the linear and non-linear
parts so that no single operation dominates the mixing. Together with 64
rounds and full block chaining, this is what produces the avalanche effect
described below — there is no known way to run these steps backward from a
digest to the message that produced it.

**Why this is the right primitive for the comparison itself:**

- **Deterministic** — the same password always yields the same hash, so
  `storedHash.equals(hash)` is a valid credential check.
- **One-way** — there's no known feasible way to invert SHA-256 and recover
  the password from the digest; a leaked `PasswordHash` column doesn't hand
  over credentials directly.
- **Avalanche effect** — changing one character in the password flips
  roughly half the output bits, so no partial-match attack is possible.
- **Fixed-width output** — every password maps to exactly 32 bytes / 64 hex
  characters, so `PasswordHash` is a predictable, fixed-width column.

**Where plain SHA-256 falls short of a production password store** (stated
here deliberately rather than left implicit): SHA-256 is *fast* by design —
it was built for data-integrity checksums, not credential storage — which
means an attacker who obtains the `MarketingUser` table can brute-force or
dictionary-attack it at billions of hashes/second on commodity GPU hardware.
It also has no per-user salt, so two users with the same password produce
identical hashes and are vulnerable to precomputed rainbow-table lookups.
For a project scoped as an internal, access-gated dashboard (not a
public-facing credential store), this trade-off is acceptable; the
recommended production upgrade — noted here explicitly rather than assumed —
is a slow, salted algorithm such as **bcrypt**, **scrypt**, or **Argon2**,
each of which adds a random per-user salt and a tunable computational cost
factor that scales with hardware improvements.

---

## 7. Setup

### Prerequisites
- JDK 8 or later
- MySQL 8.x (or compatible) with the `marketing_db` schema, its tables
  (`MarketingUser`, `Campaign`, `Ad`, `CampaignProduct`, `Product`, `Orders`,
  `Customer`, `CustomerSegment`, `Platform`, …), the two triggers, the
  `sp_RecalcAdMetrics` procedure, and the seven `vw_*` views already created
- MySQL Connector/J (`mysql-connector-j`) on the classpath

### Configure the connection
Edit `DBConnection.java`:
```java
private static final String URL =
        "jdbc:mysql://localhost:3306/marketing_db?useSSL=false&serverTimezone=UTC";
private static final String USER = "your_db_user";
private static final String PASSWORD = "your_db_password";
```
(See §9 for why these should move out of source before any shared deployment.)

### Compile and run — console version
```bash
javac -cp .:mysql-connector-j-<version>.jar *.java
java  -cp .:mysql-connector-j-<version>.jar MarketingDashboard
```

### Compile and run — GUI version
```bash
javac -cp .:mysql-connector-j-<version>.jar *.java
java  -cp .:mysql-connector-j-<version>.jar MarketingDashboardGUI
```
*(On Windows, replace `:` with `;` in the classpath.)*

### Seed a marketing-team login
Insert a row into `MarketingUser` with `Role = 'MARKETING'` and a
`PasswordHash` equal to the SHA-256 digest of the chosen password (e.g.
generated with the `sha256()` method in `LoginDAO`, or `echo -n "password" |
sha256sum` for a quick manual seed).

---

## 8. Usage overview

| Menu | Capabilities |
|---|---|
| Campaign management | create campaign, list campaigns, view ROI/spend/CTR performance, update budget |
| Ad management | create ad (budget-checked), update impressions/clicks (auto-recalculates conversion rate & revenue), view ad performance overall or by campaign |
| Campaign products & pricing | attach a product to a campaign with a discount (selling price auto-computed), update discount, list a campaign's priced products |
| Reports & analytics | region / segment / platform / product performance, campaign-by-segment breakdown, budget alerts, campaigns ending soon, top ads by ROAS, underperforming ads, segment–platform affinity |

---


## 9. Project structure

```
.
├── DBConnection.java           # JDBC connection factory
├── LoginDAO.java                # authentication (SHA-256 hash comparison)
├── CampaignDAO.java             # campaign CRUD + performance view reads
├── AdDAO.java                   # ad CRUD, budget-trigger handling, recalculation
├── CampaignProductDAO.java      # product-to-campaign pricing (trigger-computed)
├── ReportDAO.java               # 10 analytics/reporting queries
├── MarketingDashboard.java      # console front end
├── MarketingDashboardGUI.java   # Swing desktop front end
└── README.md
```


