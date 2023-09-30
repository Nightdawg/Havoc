package haven.automated.cookbook;

import haven.ItemInfo;
import haven.res.ui.tt.q.qbuff.QBuff;
import haven.resutil.FoodInfo;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
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
            insertAndSendFood();
            queuedFood.clear();
            try {
                Thread.sleep(15000);
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
                parsedFoodInfo.hunger = round3Dig(foodInfo.glut * 1000);

                double totalFep = 0.0;
                for (int i = 0; i < foodInfo.evs.length; i++) {
                    double value = round3Dig(foodInfo.evs[i].a / multiplier);
                    totalFep += value;

                    switch (foodInfo.evs[i].ev.nm) {
                        case "Strength +1" -> parsedFoodInfo.str1 = value;
                        case "Strength +2" -> parsedFoodInfo.str2 = value;
                        case "Agility +1" -> parsedFoodInfo.agi1 = value;
                        case "Agility +2" -> parsedFoodInfo.agi2 = value;
                        case "Intelligence +1" -> parsedFoodInfo.int1 = value;
                        case "Intelligence +2" -> parsedFoodInfo.int2 = value;
                        case "Constitution +1" -> parsedFoodInfo.con1 = value;
                        case "Constitution +2" -> parsedFoodInfo.con2 = value;
                        case "Perception +1" -> parsedFoodInfo.per1 = value;
                        case "Perception +2" -> parsedFoodInfo.per2 = value;
                        case "Charisma +1" -> parsedFoodInfo.cha1 = value;
                        case "Charisma +2" -> parsedFoodInfo.cha2 = value;
                        case "Dexterity +1" -> parsedFoodInfo.dex1 = value;
                        case "Dexterity +2" -> parsedFoodInfo.dex2 = value;
                        case "Will +1" -> parsedFoodInfo.wil1 = value;
                        case "Will +2" -> parsedFoodInfo.wil2 = value;
                        case "Psyche +1" -> parsedFoodInfo.psy1 = value;
                        case "Psyche +2" -> parsedFoodInfo.psy2 = value;
                        default -> {
                            return;
                        }
                    }
                }
                parsedFoodInfo.totalFep = round3Dig(totalFep);
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
                parsedFoodInfo.hash = createHash(parsedFoodInfo);
                queuedFood.add(parsedFoodInfo);
            }
        } catch (Exception ignored) {}
    }

    private void insertAndSendFood() {
        Set<ParsedFoodInfo> currentQueue = new HashSet<>(queuedFood);
        queuedFood.clear();

        if(currentQueue.size() > 0){
            sendToHttpServer(currentQueue);

            for(ParsedFoodInfo foodInfo : currentQueue){
                insertIntoDatabase(foodInfo);
            }
        }
    }

    public void sendToHttpServer(Set<ParsedFoodInfo> foodInfos) {
        JSONArray jsonArray = new JSONArray();
        for (ParsedFoodInfo foodInfo : foodInfos) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("hash", foodInfo.hash);
            jsonObject.put("name", foodInfo.itemName);
            jsonObject.put("resource", foodInfo.resourceName);
            jsonObject.put("energy", foodInfo.energy);
            jsonObject.put("hunger", foodInfo.hunger);
            jsonObject.put("totalFep", foodInfo.totalFep);
            jsonObject.put("str1", foodInfo.str1);
            jsonObject.put("str2", foodInfo.str2);
            jsonObject.put("agi1", foodInfo.agi1);
            jsonObject.put("agi2", foodInfo.agi2);
            jsonObject.put("int1", foodInfo.int1);
            jsonObject.put("int2", foodInfo.int2);
            jsonObject.put("con1", foodInfo.con1);
            jsonObject.put("con2", foodInfo.con2);
            jsonObject.put("per1", foodInfo.per1);
            jsonObject.put("per2", foodInfo.per2);
            jsonObject.put("cha1", foodInfo.cha1);
            jsonObject.put("cha2", foodInfo.cha2);
            jsonObject.put("dex1", foodInfo.dex1);
            jsonObject.put("dex2", foodInfo.dex2);
            jsonObject.put("wil1", foodInfo.wil1);
            jsonObject.put("wil2", foodInfo.wil2);
            jsonObject.put("psy1", foodInfo.psy1);
            jsonObject.put("psy2", foodInfo.psy2);
            JSONArray ingredientsArray = new JSONArray();
            for (ParsedFoodInfo.FoodIngredient ingredient : foodInfo.ingredients) {
                JSONObject ingredientObject = new JSONObject();
                ingredientObject.put("name", ingredient.name);
                ingredientObject.put("percentage", ingredient.percentage);
                ingredientsArray.put(ingredientObject);
            }
            jsonObject.put("ingredients", ingredientsArray);
            jsonArray.put(jsonObject);
        }

        try {
            URL apiUrl = new URL("https://logs.havocandhearth.net/food-log/create");
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("x-security-token", "63b510b2f610d4b6a8e6b03c2c");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonArray.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            connection.getResponseCode();
            connection.disconnect();
        } catch (IOException ignored) {}
    }

    private void insertIntoDatabase(ParsedFoodInfo foodInfo){
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(DATABASE);
            connection.setAutoCommit(false);

            String sql = "SELECT id FROM food WHERE hash = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, foodInfo.hash);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return;
                }
            }

            List<Integer> ingredientIds = new ArrayList<>();
            for (ParsedFoodInfo.FoodIngredient ingredient : foodInfo.ingredients) {
                sql = "INSERT OR IGNORE INTO ingredient (name) VALUES (?)";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, ingredient.name);
                    pstmt.executeUpdate();
                }

                sql = "SELECT id FROM ingredient WHERE name = ?";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, ingredient.name);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        ingredientIds.add(rs.getInt("id"));
                    }
                }
            }

            int foodId = -1;
            sql = "INSERT INTO food (hash, name, resource, energy, hunger, totalFep, " +
                    "str1, str2, agi1, agi2, int1, int2, con1, con2, per1, per2, cha1, cha2, dex1, dex2, wil1, wil2, psy1, psy2) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, foodInfo.hash);
                pstmt.setString(2, foodInfo.itemName);
                pstmt.setString(3, foodInfo.resourceName);
                pstmt.setInt(4, foodInfo.energy);
                pstmt.setDouble(5, foodInfo.hunger);
                pstmt.setDouble(6, foodInfo.totalFep);
                pstmt.setDouble(7, foodInfo.str1);
                pstmt.setDouble(8, foodInfo.str2);
                pstmt.setDouble(9, foodInfo.agi1);
                pstmt.setDouble(10, foodInfo.agi2);
                pstmt.setDouble(11, foodInfo.int1);
                pstmt.setDouble(12, foodInfo.int2);
                pstmt.setDouble(13, foodInfo.con1);
                pstmt.setDouble(14, foodInfo.con2);
                pstmt.setDouble(15, foodInfo.per1);
                pstmt.setDouble(16, foodInfo.per2);
                pstmt.setDouble(17, foodInfo.cha1);
                pstmt.setDouble(18, foodInfo.cha2);
                pstmt.setDouble(19, foodInfo.dex1);
                pstmt.setDouble(20, foodInfo.dex2);
                pstmt.setDouble(21, foodInfo.wil1);
                pstmt.setDouble(22, foodInfo.wil2);
                pstmt.setDouble(23, foodInfo.psy1);
                pstmt.setDouble(24, foodInfo.psy2);
                pstmt.executeUpdate();
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    foodId = rs.getInt(1);
                }
            }

            for (int i = 0; i < foodInfo.ingredients.size(); i++) {
                int ingredientId = ingredientIds.get(i);
                double percentage = foodInfo.ingredients.get(i).percentage;

                sql = "INSERT INTO food_ingredient (food_id, ingredient_id, percentage) VALUES (?, ?, ?)";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setInt(1, foodId);
                    pstmt.setInt(2, ingredientId);
                    pstmt.setDouble(3, percentage);
                    pstmt.executeUpdate();
                }
            }
            connection.commit();
        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    private static String createHash(ParsedFoodInfo foodInfo) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String inputData = foodInfo.itemName + "|" +
                foodInfo.resourceName + "|" +
                foodInfo.energy + "|" +
                foodInfo.hunger + "|" +
                foodInfo.totalFep + "|" +
                foodInfo.ingredients.stream()
                        .map(ParsedFoodInfo.FoodIngredient::toString)
                        .collect(Collectors.joining(","));
        byte[] hash = digest.digest(inputData.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    private static double round3Dig(double value) {
        return Math.round(value * 1000.0) / 1000.0;
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
                            "created DATETIME DEFAULT CURRENT_TIMESTAMP, " +
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
