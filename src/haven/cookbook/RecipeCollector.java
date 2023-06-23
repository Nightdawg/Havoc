package haven.cookbook;

import haven.ItemInfo;
import haven.res.ui.tt.q.qbuff.QBuff;
import haven.resutil.FoodInfo;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RecipeCollector implements Runnable {
    private static final String DATABASE = "jdbc:sqlite:food_recipes.db";
    private static Set<ParsedFoodInfo> queuedFood = ConcurrentHashMap.newKeySet();
    private final boolean run;

    static {
        try {
            createDatabaseIfNotExist();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public RecipeCollector() {
        this.run = true;
    }

    @Override
    public void run() {
        while (run) {
            checkAndInsertFood();
            queuedFood.clear();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public static void addFood(List<ItemInfo> infoList, String resName) {
        try {
            FoodInfo foodInfo = ItemInfo.find(FoodInfo.class, infoList);
            if (foodInfo != null) {
                QBuff qBuff = ItemInfo.find(QBuff.class, infoList);
                double quality = qBuff != null ? qBuff.q : 10.0;
                double multiplier = Math.sqrt(quality / 10.0);

                ParsedFoodInfo parsedFoodInfo = new ParsedFoodInfo();
                parsedFoodInfo.resourceName = resName;
                parsedFoodInfo.energy = (int) (Math.round(foodInfo.end * 100));
                parsedFoodInfo.hunger = round2Dig(foodInfo.glut * 100);

                for (int i = 0; i < foodInfo.evs.length; i++) {
                    parsedFoodInfo.feps.add(new ParsedFoodInfo.FoodFEP(foodInfo.evs[i].ev.nm, round2Dig(foodInfo.evs[i].a / multiplier)));
                }

                for (ItemInfo info : infoList) {
                    if (info instanceof ItemInfo.AdHoc) {
                        String text = ((ItemInfo.AdHoc) info).str.text;
                        if (text.equals("White-truffled")
                                || text.equals("Black-truffled")
                                || text.equals("Peppered")) {
                            return;
                        }
                    }
                    if (info instanceof ItemInfo.Name) {
                        parsedFoodInfo.itemName = ((ItemInfo.Name) info).str.text;
                    }
                    if (info.getClass().getName().equals("Ingredient")) {
                        String name = (String) info.getClass().getField("name").get(info);
                        Double value = (Double) info.getClass().getField("val").get(info);
                        parsedFoodInfo.ingredients.add(new ParsedFoodInfo.FoodIngredient(name, (int) (value * 100)));
                    } else if (info.getClass().getName().equals("Smoke")) {
                        String name = (String) info.getClass().getField("name").get(info);
                        Double value = (Double) info.getClass().getField("val").get(info);
                        parsedFoodInfo.ingredients.add(new ParsedFoodInfo.FoodIngredient(name, (int) (value * 100)));
                    }
                }
                queuedFood.add(parsedFoodInfo);
            }
        } catch (Exception ex) {
            System.out.println("Cannot create food info: " + ex.getMessage());
        }
    }

    private void checkAndInsertFood() {
        Set<ParsedFoodInfo> currentQueue = new HashSet<>(queuedFood);
        queuedFood.clear();
        try (Connection conn = DriverManager.getConnection(DATABASE)) {
            for (ParsedFoodInfo food : currentQueue) {
                String hash = createHash(food);
                String sql = "SELECT count(*) FROM food WHERE hash = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, hash);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.getInt(1) == 0) {
                            int foodId = insertFood(conn, food, hash);  // Get the returned food id
                            for (ParsedFoodInfo.FoodIngredient foodIngredient : food.getIngredients()) {
                                int ingredientId = getOrInsertIngredient(conn, foodIngredient.getName());
                                insertFoodIngredient(conn, foodId, ingredientId, foodIngredient.getPercentage());
                            }
                        }
                    }
                }
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    private int insertFood(Connection conn, ParsedFoodInfo food, String hash) throws SQLException {
        double totalFep = 0.0;
        Map<String, String> fepMap = new HashMap<>();
        fepMap.put("Strength", "str");
        fepMap.put("Agility", "agi");
        fepMap.put("Intelligence", "int");
        fepMap.put("Constitution", "con");
        fepMap.put("Perception", "per");
        fepMap.put("Charisma", "cha");
        fepMap.put("Dexterity", "dex");
        fepMap.put("Will", "wil");
        fepMap.put("Psyche", "psy");

        Map<String, Double> valuesMap = new HashMap<>();
        for (String fep : fepMap.values()) {
            valuesMap.put(fep + "1", 0.0);
            valuesMap.put(fep + "2", 0.0);
        }

        for (ParsedFoodInfo.FoodFEP fep : food.getFeps()) {
            totalFep += fep.getValue();
            String[] splitName = fep.getName().split(" ");
            String fepName = fepMap.get(splitName[0]) + splitName[1].replace("+", "");
            valuesMap.put(fepName, fep.getValue());
        }

        String fieldNames = String.join(", ", valuesMap.keySet());
        String fieldValues = valuesMap.values().stream().map(Object::toString).collect(Collectors.joining(", "));

        String insertSql = "INSERT INTO food(hash, name, resource, energy, hunger, totalFep, " + fieldNames + ") " +
                "VALUES (?, ?, ?, ?, ?, ?, " + fieldValues + ")";

        try (PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, hash);
            pstmt.setString(2, food.getItemName());
            pstmt.setString(3, food.getResourceName());
            pstmt.setInt(4, food.getEnergy());
            pstmt.setDouble(5, food.getHunger());
            pstmt.setDouble(6, Math.round(totalFep * 100.0) / 100.0);
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);  // Returning the generated id
            } else {
                throw new SQLException("Creating food failed, no ID obtained.");
            }
        }
    }
    private int getOrInsertIngredient(Connection conn, String ingredientName) throws SQLException {
        String checkSql = "SELECT id FROM ingredient WHERE name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
            pstmt.setString(1, ingredientName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        String insertSql = "INSERT INTO ingredient(name) VALUES (?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, ingredientName);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating ingredient failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating ingredient failed, no ID obtained.");
                }
            }
        }
    }
    private void insertFoodIngredient(Connection conn, int foodId, int ingredientId, int percentage) throws SQLException {
        String insertSql = "INSERT INTO food_ingredient(food_id, ingredient_id, percentage) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            pstmt.setInt(1, foodId);
            pstmt.setInt(2, ingredientId);
            pstmt.setInt(3, percentage);
            pstmt.executeUpdate();
        }
    }
    private String createHash(ParsedFoodInfo foodInfo) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String inputData = foodInfo.getItemName() + foodInfo.getResourceName() + foodInfo.getIngredients().toString();
        byte[] hash = digest.digest(inputData.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    private static double round2Dig(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public static void createDatabaseIfNotExist() throws SQLException {
        File dbFile = new File("food_recipes.db");
        boolean dbExists = dbFile.exists();
        try (Connection conn = DriverManager.getConnection(DATABASE)) {
            if (!dbExists) {
                System.out.println("A new database is being created.");
            }

            createSchemaElementIfNotExist(conn, dbExists, "food",
                    "id INTEGER PRIMARY KEY, " +
                            "hash VARCHAR(255) UNIQUE NOT NULL, " +
                            "name VARCHAR(255) NOT NULL, " +
                            "resource VARCHAR(255) NOT NULL, " +
                            "energy INTEGER, " +
                            "hunger REAL, " +
                            "totalFep REAL, " +
                            "str1 REAL, " +
                            "str2 REAL, " +
                            "agi1 REAL, " +
                            "agi2 REAL, " +
                            "int1 REAL, " +
                            "int2 REAL, " +
                            "con1 REAL, " +
                            "con2 REAL, " +
                            "per1 REAL, " +
                            "per2 REAL, " +
                            "cha1 REAL, " +
                            "cha2 REAL, " +
                            "dex1 REAL, " +
                            "dex2 REAL, " +
                            "wil1 REAL, " +
                            "wil2 REAL, " +
                            "psy1 REAL, " +
                            "psy2 REAL", "table");

            createSchemaElementIfNotExist(conn, dbExists, "ingredient",
                    "id INTEGER PRIMARY KEY, " +
                            "name VARCHAR(255) UNIQUE NOT NULL", "table");

            createSchemaElementIfNotExist(conn, dbExists, "food_ingredient",
                    "food_id INTEGER, " +
                            "ingredient_id INTEGER, " +
                            "percentage REAL, " +
                            "FOREIGN KEY(food_id) REFERENCES food(id), " +
                            "FOREIGN KEY(ingredient_id) REFERENCES ingredient(id)", "table");

            createSchemaElementIfNotExist(conn, dbExists, "idx_ingredients_name", "ingredient(name)", "index");
            createSchemaElementIfNotExist(conn, dbExists, "idx_food_ingredients_food_id_ingredient_id", "food_ingredient(food_id, ingredient_id)", "index");
        }
    }
    private static void createSchemaElementIfNotExist(Connection conn, boolean dbExists, String name, String definitions, String type) throws SQLException {
        if (!schemaElementExists(conn, name, type)) {
            String sql = type.equals("table") ? "CREATE TABLE " + name + " (\n" + definitions + "\n);" : "CREATE INDEX " + name + " ON " + definitions + ";";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
            if (dbExists) {
                System.out.println("A new " + type + " (" + name + ") has been created in the existing database.");
            }
        }
    }
    private static boolean schemaElementExists(Connection conn, String name, String type) throws SQLException {
        String checkExistsQuery = "SELECT name FROM sqlite_master WHERE type='" + type + "' AND name='" + name + "';";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkExistsQuery)) {
            return rs.next();
        }
    }
}
