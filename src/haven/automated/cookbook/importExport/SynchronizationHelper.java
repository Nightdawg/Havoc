package haven.automated.cookbook.importExport;

import haven.GameUI;
import haven.HackThread;
import haven.automated.cookbook.ParsedFoodInfo;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class SynchronizationHelper {
    private static final String DATABASE = "jdbc:sqlite:food_recipes.db";

    public static List<ParsedFoodInfo> synchronizeFromServer() {
        List<ParsedFoodInfo> parsedFoodInfoList = new ArrayList<>();
        try {
            Connection connectionDb = DriverManager.getConnection(DATABASE);
            Statement statement = connectionDb.createStatement();

            ResultSet rs = statement.executeQuery("SELECT hash FROM food");
            JSONArray localHashes = new JSONArray();
            while (rs.next()) {
                localHashes.put(rs.getString("hash"));
            }

            JSONObject json = new JSONObject();
            json.put("hashes", localHashes);

            URL url = new URL("https://logs.havocandhearth.net/food/synchronize");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("x-security-token", "G6gDF0Gdfgdfghj6a8e6ZGF5c2c");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush();
            }

            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    JSONArray jsonArray = new JSONArray(response.toString());

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject foodObject = jsonArray.getJSONObject(i);
                        ParsedFoodInfo foodInfo = new ParsedFoodInfo();

                        foodInfo.id = foodObject.optInt("id", 0);
                        foodInfo.hash = foodObject.optString("hash", "");
                        foodInfo.itemName = foodObject.optString("name", "");
                        foodInfo.resourceName = foodObject.optString("resource", "");
                        foodInfo.energy = foodObject.optInt("energy", 0);
                        foodInfo.hunger = foodObject.optDouble("hunger", 0);
                        foodInfo.totalFep = foodObject.optDouble("totalfep", 0);

                        foodInfo.str1 = foodObject.optDouble("str1", 0);
                        foodInfo.str2 = foodObject.optDouble("str2", 0);
                        foodInfo.agi1 = foodObject.optDouble("agi1", 0);
                        foodInfo.agi2 = foodObject.optDouble("agi2", 0);
                        foodInfo.int1 = foodObject.optDouble("int1", 0);
                        foodInfo.int2 = foodObject.optDouble("int2", 0);
                        foodInfo.con1 = foodObject.optDouble("con1", 0);
                        foodInfo.con2 = foodObject.optDouble("con2", 0);
                        foodInfo.per1 = foodObject.optDouble("per1", 0);
                        foodInfo.per2 = foodObject.optDouble("per2", 0);
                        foodInfo.cha1 = foodObject.optDouble("cha1", 0);
                        foodInfo.cha2 = foodObject.optDouble("cha2", 0);
                        foodInfo.dex1 = foodObject.optDouble("dex1", 0);
                        foodInfo.dex2 = foodObject.optDouble("dex2", 0);
                        foodInfo.wil1 = foodObject.optDouble("wil1", 0);
                        foodInfo.wil2 = foodObject.optDouble("wil2", 0);
                        foodInfo.psy1 = foodObject.optDouble("psy1", 0);
                        foodInfo.psy2 = foodObject.optDouble("psy2", 0);

                        JSONArray ingredientsArray = foodObject.getJSONArray("ingredients");
                        for (int j = 0; j < ingredientsArray.length(); j++) {
                            JSONObject ingredientObject = ingredientsArray.getJSONObject(j);
                            ParsedFoodInfo.FoodIngredient ingredient = new ParsedFoodInfo.FoodIngredient(
                                    ingredientObject.getString("name"),
                                    ingredientObject.getInt("percentage")
                            );
                            foodInfo.ingredients.add(ingredient);
                        }
                        parsedFoodInfoList.add(foodInfo);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return parsedFoodInfoList;
    }

    public static void synchronizeRecipes(GameUI gui) {
        RecipeLoadWindow prog = new RecipeLoadWindow("Synchronizing Recipes");
        Thread th = new HackThread(() -> {
            List<ParsedFoodInfo> missingRecipes;
            try {
                missingRecipes = synchronizeFromServer();
            } catch (Exception e) {
                gui.error("Error during synchronization");
                return;
            }

            if (missingRecipes == null || missingRecipes.isEmpty()) {
                gui.msg("No new recipes to synchronize.");
                return;
            }

            try (Connection conn = DriverManager.getConnection(DATABASE)) {
                conn.setAutoCommit(false);

                try (PreparedStatement insertIngredient = conn.prepareStatement("INSERT OR IGNORE INTO ingredient(name) VALUES(?)");
                     PreparedStatement insertFood = conn.prepareStatement("INSERT INTO food(hash, name, resource, energy, hunger, totalFep, str1, str2, agi1, agi2, int1, int2, con1, con2, per1, per2, cha1, cha2, dex1, dex2, wil1, wil2, psy1, psy2) " +
                             "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                     PreparedStatement insertFoodIngredient = conn.prepareStatement("INSERT INTO food_ingredient(food_id, ingredient_id, percentage) VALUES(?, ?, ?)")) {

                    // Insert Ingredients and fetch IDs.
                    Map<String, Integer> ingredientIdMap = new HashMap<>();
                    for (ParsedFoodInfo recipe : missingRecipes) {
                        for (ParsedFoodInfo.FoodIngredient ingredient : recipe.ingredients) {
                            insertIngredient.setString(1, ingredient.name);
                            insertIngredient.addBatch();
                        }
                    }
                    insertIngredient.executeBatch();
                    conn.commit();

                    try (ResultSet rs = conn.createStatement().executeQuery("SELECT id, name FROM ingredient")) {
                        while (rs.next()) ingredientIdMap.put(rs.getString("name"), rs.getInt("id"));
                    }

                    // Insert Foods.
                    for (ParsedFoodInfo recipe : missingRecipes) {
                        if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                        insertFood.setString(1, recipe.hash);
                        insertFood.setString(2, recipe.itemName);
                        insertFood.setString(3, recipe.resourceName);
                        insertFood.setInt(4, recipe.energy);
                        insertFood.setDouble(5, recipe.hunger);
                        insertFood.setDouble(6, recipe.totalFep);
                        insertFood.setDouble(7, recipe.str1);
                        insertFood.setDouble(8, recipe.str2);
                        insertFood.setDouble(9, recipe.agi1);
                        insertFood.setDouble(10, recipe.agi2);
                        insertFood.setDouble(11, recipe.int1);
                        insertFood.setDouble(12, recipe.int2);
                        insertFood.setDouble(13, recipe.con1);
                        insertFood.setDouble(14, recipe.con2);
                        insertFood.setDouble(15, recipe.per1);
                        insertFood.setDouble(16, recipe.per2);
                        insertFood.setDouble(17, recipe.cha1);
                        insertFood.setDouble(18, recipe.cha2);
                        insertFood.setDouble(19, recipe.dex1);
                        insertFood.setDouble(20, recipe.dex2);
                        insertFood.setDouble(21, recipe.wil1);
                        insertFood.setDouble(22, recipe.wil2);
                        insertFood.setDouble(23, recipe.psy1);
                        insertFood.setDouble(24, recipe.psy2);
                        insertFood.addBatch();
                    }
                    prog.prog(String.format("Inserting %d foods...", missingRecipes.size()));
                    insertFood.executeBatch();
                    conn.commit();

                    // Select IDs of inserted Foods.
                    List<String> insertedHashes = missingRecipes.stream().map(r -> r.hash).collect(Collectors.toList());
                    String inClause = String.join(",", Collections.nCopies(insertedHashes.size(), "?"));
                    String selectSql = "SELECT id, hash FROM food WHERE hash IN (" + inClause + ")";

                    Map<String, Integer> foodIdMap = new HashMap<>();
                    try (PreparedStatement selectStatement = conn.prepareStatement(selectSql)) {
                        for (int i = 0; i < insertedHashes.size(); i++) {
                            selectStatement.setString(i + 1, insertedHashes.get(i));
                        }
                        try (ResultSet rs = selectStatement.executeQuery()) {
                            while (rs.next()) {
                                foodIdMap.put(rs.getString("hash"), rs.getInt("id"));
                            }
                        }
                    }

                    List<FoodIngredientMapping> foodIngredientMappings = new ArrayList<>();
                    for (ParsedFoodInfo recipe : missingRecipes) {
                        int foodId = foodIdMap.get(recipe.hash);
                        for (ParsedFoodInfo.FoodIngredient ingredient : recipe.ingredients) {
                            foodIngredientMappings.add(new FoodIngredientMapping(foodId, ingredientIdMap.get(ingredient.name), ingredient.percentage));
                        }
                    }

                    for (FoodIngredientMapping mapping : foodIngredientMappings) {
                        insertFoodIngredient.setInt(1, mapping.foodId); // setting food_id
                        insertFoodIngredient.setInt(2, mapping.ingredientId); // setting ingredient_id
                        insertFoodIngredient.setDouble(3, mapping.percentage); // setting percentage
                        insertFoodIngredient.addBatch();
                    }
                    prog.prog(String.format("Inserting %d food-ingredient relations...", foodIngredientMappings.size()));
                    insertFoodIngredient.executeBatch();
                    conn.commit();
                    gui.msg(String.format("Synchronization completed successfully! %d new recipes were added.", missingRecipes.size()));
                } catch (SQLException | InterruptedException e) {
                    try {
                        if (!conn.isClosed()) conn.rollback();
                    } catch (SQLException ignored) {}
                    gui.error("Synchronization Failed");
                }
            } catch (SQLException e) {
                gui.error("Connection Error.");
            }
            prog.destroy();
        }, "Recipe Synchronizer");

        prog.run(th);
        gui.adda(prog, gui.sz.div(2), 0.5, 1.0);
    }



    static class FoodIngredientMapping {
        final int foodId;
        final int ingredientId;
        final double percentage;

        FoodIngredientMapping(int foodId, int ingredientId, double percentage) {
            this.foodId = foodId;
            this.ingredientId = ingredientId;
            this.percentage = percentage;
        }
    }
}
