package haven.cookbook.importExport;

import haven.GameUI;
import haven.HackThread;
import haven.cookbook.ParsedFoodInfo;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class ImportExportHelper {
    private static final String DATABASE = "jdbc:sqlite:food_recipes.db";

    public static List<ParsedFoodInfo> fetchAllFoodRecipes() {
        List<ParsedFoodInfo> parsedFoodInfoList = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DATABASE)) {
            String sql = "SELECT * FROM food";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }

                    ParsedFoodInfo info = new ParsedFoodInfo();
                    info.id = rs.getInt("id");
                    info.itemName = rs.getString("name");
                    info.resourceName = rs.getString("resource");
                    info.energy = rs.getInt("energy");
                    info.hunger = rs.getDouble("hunger");

                    String ingredientSql = "SELECT i.name, fi.percentage FROM food_ingredient fi " +
                            "JOIN ingredient i ON fi.ingredient_id = i.id WHERE fi.food_id = ?";
                    try (PreparedStatement ingredientStmt = conn.prepareStatement(ingredientSql)) {
                        ingredientStmt.setInt(1, info.id);
                        try (ResultSet ingredientRs = ingredientStmt.executeQuery()) {
                            while (ingredientRs.next()) {
                                String ingredientName = ingredientRs.getString("name");
                                int percentage = ingredientRs.getInt("percentage");
                                info.ingredients.add(new ParsedFoodInfo.FoodIngredient(ingredientName, percentage));
                            }
                        }
                    }

                    String[] fepFields = {"str", "agi", "int", "con", "per", "cha", "dex", "wil", "psy"};
                    for (String fep : fepFields) {
                        double fep1 = rs.getDouble(fep + "1");
                        double fep2 = rs.getDouble(fep + "2");
                        if (fep1 != 0) {
                            info.feps.add(new ParsedFoodInfo.FoodFEP(fep + "1", fep1));
                        }
                        if (fep2 != 0) {
                            info.feps.add(new ParsedFoodInfo.FoodFEP(fep + "2", fep2));
                        }
                    }
                    parsedFoodInfoList.add(info);
                }
            } catch (InterruptedException e) {
                System.out.println("Recipe export aborted;");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return parsedFoodInfoList;
    }

    public static Map<String, ParsedFoodInfo> fetchAllFoodRecipesToMap() {
        Map<Integer, ParsedFoodInfo> parsedFoodInfoMapById = new HashMap<>();

        try (Connection conn = DriverManager.getConnection(DATABASE)) {
            // Fetch all food data
            String sql = "SELECT * FROM food";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }

                    ParsedFoodInfo info = new ParsedFoodInfo();
                    info.id = rs.getInt("id");
                    info.itemName = rs.getString("name");
                    info.resourceName = rs.getString("resource");
                    info.energy = rs.getInt("energy");
                    info.hunger = rs.getDouble("hunger");

                    String[] fepFields = {"str", "agi", "int", "con", "per", "cha", "dex", "wil", "psy"};
                    for (String fep : fepFields) {
                        double fep1 = rs.getDouble(fep + "1");
                        double fep2 = rs.getDouble(fep + "2");
                        if (fep1 != 0) {
                            info.feps.add(new ParsedFoodInfo.FoodFEP(fep + "1", fep1));
                        }
                        if (fep2 != 0) {
                            info.feps.add(new ParsedFoodInfo.FoodFEP(fep + "2", fep2));
                        }
                    }

                    // Add food to the map by ID
                    parsedFoodInfoMapById.put(info.id, info);
                }
            } catch (InterruptedException e) {
                System.out.println("Recipe export aborted;");
            }

            // Fetch all ingredients
            String ingredientSql = "SELECT i.name, fi.percentage, fi.food_id FROM food_ingredient fi " +
                    "JOIN ingredient i ON fi.ingredient_id = i.id";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(ingredientSql)) {
                while (rs.next()) {
                    String ingredientName = rs.getString("name");
                    int percentage = rs.getInt("percentage");
                    int foodId = rs.getInt("food_id");

                    // Get the food and add ingredient to it
                    ParsedFoodInfo food = parsedFoodInfoMapById.get(foodId);
                    if (food != null) {
                        food.ingredients.add(new ParsedFoodInfo.FoodIngredient(ingredientName, percentage));
                    }
                }
            }

            // Now that we have all the ingredients, calculate the hashes
            Map<String, ParsedFoodInfo> parsedFoodInfoMap = new HashMap<>();
            for (ParsedFoodInfo info : parsedFoodInfoMapById.values()) {
                parsedFoodInfoMap.put(createHash(info), info);
            }

            return parsedFoodInfoMap;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        return new HashMap<>();
    }


    public static void importRecipes(GameUI gameUI) {
        java.awt.EventQueue.invokeLater(() -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("Exported Cookbook Recipes", "hcb"));
            if (fc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
                return;
            Path path = fc.getSelectedFile().toPath();
            importRecipesSec(path, gameUI);
        });
    }

    public static void exportRecipes(GameUI gameUI) {
        java.awt.EventQueue.invokeLater(() -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("Exported Cookbook Recipes", "hcb"));
            if (fc.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
                return;
            Path path = fc.getSelectedFile().toPath();
            if (path.getFileName().toString().indexOf('.') < 0)
                path = path.resolveSibling(path.getFileName() + ".hcb");
            exportRecipesSec(path, gameUI);
        });
    }

    public static void importRecipesSec(Path path, GameUI gui) {
        RecipeLoadWindow prog = new RecipeLoadWindow("Importing Recipes");
        Thread th = new HackThread(() -> {
            try {
                // Load existing recipes from DB
                Map<String, ParsedFoodInfo> existingRecipes = fetchAllFoodRecipesToMap();

                // Load new recipes from file
                List<ParsedFoodInfo> newRecipes;
                try (InputStream in = new BufferedInputStream(Files.newInputStream(path))) {
                    ObjectInputStream ois = new ObjectInputStream(in);
                    newRecipes = (List<ParsedFoodInfo>) ois.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    gui.error("Unexpected error occurred when importing recipes.");
                    return;
                }

                long lastUpdateTime = 0;
                int total = newRecipes.size();
                int skipped = 0;
                int added = 0;

                try (Connection conn = DriverManager.getConnection(DATABASE)) {

                    for (ParsedFoodInfo food : newRecipes) {
                        if (Thread.currentThread().isInterrupted()) {
                            throw new InterruptedException();
                        }

                        String hash = createHash(food);
                        if (existingRecipes.containsKey(hash)) {
                            skipped++;
                            continue;
                        } else {
                            added++;
                        }

                        int foodId = insertFood(conn, food, hash);  // Get the returned food id
                        for (ParsedFoodInfo.FoodIngredient foodIngredient : food.getIngredients()) {
                            int ingredientId = getOrInsertIngredient(conn, foodIngredient.getName());
                            insertFoodIngredient(conn, foodId, ingredientId, foodIngredient.getPercentage());
                        }

                        if (System.currentTimeMillis() - lastUpdateTime >= 1000) {
                            prog.prog((total-(skipped+added)) + " recipes left... (" + skipped + " skipped / " + added + " added)");
                            lastUpdateTime = System.currentTimeMillis();
                        }
                    }
                } catch (SQLException | NoSuchAlgorithmException e) {
                    gui.error("Something went wrong");
                    throw new RuntimeException(e);
                }

                gui.msg("Importing finished, added:" + added + " new recipe and skipped: " + skipped + " duplicates.");
            } catch (InterruptedException e) {
                System.out.println("Recipe export aborted;");
            }
        }, "Recipes importer");

        prog.run(th);
        gui.adda(prog, gui.sz.div(2), 0.5, 1.0);
    }

    public static void exportRecipesSec(Path path, GameUI gui) {
        RecipeLoadWindow prog = new RecipeLoadWindow("Exporting Recipes");
        Thread th = new HackThread(() -> {
            boolean complete = false;
            try {
                try {
                    try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(path))) {
                        ObjectOutputStream oos = new ObjectOutputStream(out);
                        List<ParsedFoodInfo> parsedFoodRecipes = fetchAllFoodRecipes();
                        if (Thread.currentThread().isInterrupted()) {
                            throw new InterruptedException();
                        }

                        oos.writeObject(parsedFoodRecipes);
                        oos.flush();
                        oos.close();
                    }
                    complete = true;
                    gui.msg("Exporting finished.");
                } finally {
                    if (!complete)
                        Files.deleteIfExists(path);
                }
            } catch (IOException e) {
                e.printStackTrace();
                gui.error("Unexpected error occurred when exporting recipes.");
            } catch (InterruptedException e) {
                System.out.println("Recipe export aborted;");
            }
        }, "Recipes exporter");
        prog.run(th);
        gui.adda(prog, gui.sz.div(2), 0.5, 1.0);
    }

    private static int insertFood(Connection conn, ParsedFoodInfo food, String hash) throws SQLException {
        double totalFep = 0.0;

        // Initialize the valuesMap with 0.0 for all FEP fields
        Map<String, Double> valuesMap = new HashMap<>();
        String[] fepFields = {"str1", "str2", "agi1", "agi2", "int1", "int2", "con1", "con2",
                "per1", "per2", "cha1", "cha2", "dex1", "dex2", "wil1", "wil2",
                "psy1", "psy2"};
        for (String fep : fepFields) {
            valuesMap.put(fep, 0.0);
        }

        // Calculate totalFep and populate valuesMap with actual values
        for (ParsedFoodInfo.FoodFEP fep : food.getFeps()) {
            totalFep += fep.getValue();
            // fep.getName() already matches your database column names
            valuesMap.put(fep.getName(), fep.getValue());
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

    private static int getOrInsertIngredient(Connection conn, String ingredientName) throws SQLException {
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

    private static void insertFoodIngredient(Connection conn, int foodId, int ingredientId, int percentage) throws SQLException {
        String insertSql = "INSERT INTO food_ingredient(food_id, ingredient_id, percentage) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            pstmt.setInt(1, foodId);
            pstmt.setInt(2, ingredientId);
            pstmt.setInt(3, percentage);
            pstmt.executeUpdate();
        }
    }

    private static String createHash(ParsedFoodInfo foodInfo) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String inputData = foodInfo.getItemName() + foodInfo.getResourceName() + foodInfo.getIngredients().toString();
        byte[] hash = digest.digest(inputData.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
