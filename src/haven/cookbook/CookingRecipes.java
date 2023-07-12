package haven.cookbook;

import haven.Button;
import haven.Label;
import haven.Window;
import haven.*;
import haven.cookbook.importExport.ImportExportHelper;

import java.awt.*;
import java.sql.*;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CookingRecipes extends Window {
    private static final String DATABASE = "jdbc:sqlite:food_recipes.db";
    private boolean show;
    public FoodList foodList;
    private String query;
    private final Label pageLabel;
    private int page;

    public void setPage(int page) {
        this.page = page;
    }

    public CookingRecipes() {
        super(UI.scale(1100, 675), "CookBook");
        this.show = false;
        this.query = "";
        this.page = 1;
        add(new TextEntry(700, query) {
            @Override
            protected void changed() {
                setQuery(this.buf.line());
                if (page != 1) {
                    setPage(1);
                    pageLabel.settext("1");
                }
            }
        }, UI.scale(10, 5));
        add(new Button(UI.scale(30), "<<") {
            @Override
            public void click() {
                pageLabel.settext(String.valueOf(1));
                setPage(1);
                getData();
            }
        }, UI.scale(740, 3));
        add(new Button(UI.scale(30), "<") {
            @Override
            public void click() {
                if (page > 1) {
                    pageLabel.settext(String.valueOf(page - 1));
                    setPage(page - 1);
                    getData();
                }
            }
        }, UI.scale(780, 3));
        this.pageLabel = add(new Label(String.valueOf(page)), UI.scale(826, 7));
        add(new Button(UI.scale(30), ">") {
            @Override
            public void click() {
                pageLabel.settext(String.valueOf(page + 1));
                setPage(page + 1);
                getData();
            }
        }, UI.scale(860, 3));
        add(new Button(UI.scale(100), "Search") {
            @Override
            public void click() {
                getData();
            }
        }, UI.scale(975, -3));

        add(new Label("Food name"), UI.scale(64, 36));
        Label str = add(new Label("STR"), UI.scale(200, 36));
        str.setcolor(new Color(196, 19, 19));
        Label agi = add(new Label("AGI"), UI.scale(250, 36));
        agi.setcolor(new Color(102, 84, 255));
        Label intel = add(new Label("INT"), UI.scale(300, 36));
        intel.setcolor(new Color(0, 231, 255));
        Label con = add(new Label("CON"), UI.scale(350, 36));
        con.setcolor(new Color(255, 0, 195));
        Label per = add(new Label("PER"), UI.scale(400, 36));
        per.setcolor(new Color(255, 127, 0));
        Label cha = add(new Label("CHA"), UI.scale(450, 36));
        cha.setcolor(new Color(26, 255, 0));
        Label dex = add(new Label("DEX"), UI.scale(500, 36));
        dex.setcolor(new Color(255, 255, 180));
        Label wil = add(new Label("WIL"), UI.scale(550, 36));
        wil.setcolor(new Color(255, 255, 0, 255));
        Label psy = add(new Label("PSY"), UI.scale(600, 36));
        psy.setcolor(new Color(184, 0, 255));
        add(new Label("Ingredients"), UI.scale(775, 36));
        add(new Label("Hunger"), UI.scale(987, 36));
        add(new Label("Energy"), UI.scale(1045, 36));

        foodList = new FoodList(1090, 10);
        add(foodList, UI.scale(5, 50));

        add(new Button(UI.scale(50), "Export") {
            @Override
            public void click() {
                ImportExportHelper.exportRecipes(gameui());
            }
        }, UI.scale(1015, 658));

        add(new Button(UI.scale(50), "Import") {
            @Override
            public void click() {
                ImportExportHelper.importRecipes(gameui());
            }
        }, UI.scale(950, 658));

        getData();
    }

    public void addFood(Food food) {
        synchronized (foodList) {
            foodList.addFood(food);
        }
    }

    public void getData() {
        try (Connection conn = DriverManager.getConnection(DATABASE)) {
            foodList.removeAllFoods();
            String[] conditions = query.split(";");

            StringBuilder sql = new StringBuilder("SELECT * from food");

            boolean whereClauseAdded = false;
            boolean sortAdded = false;
            String orderBy = "";
            for (String condition : conditions) {
                if (Pattern.matches("^name:([a-zA-Z ]{1,50})$", condition)) {
                    if (!whereClauseAdded) {
                        sql.append(" WHERE ");
                        whereClauseAdded = true;
                    } else {
                        sql.append(" AND ");
                    }
                    sql.append("food.name like '%");
                    sql.append(condition.substring(5));
                    sql.append("%'");
                } else if (Pattern.matches("^(str1|str2|agi1|agi2|int1|int2|con1|con2|per1|per2|cha1|cha2|dex1|dex2|wil1|wil2|psy1|psy2)([<>]=?)(100%|[1-9][0-9]?%|[1-9][0-9]{0,2}|1000)$", condition)) {
                    if (!whereClauseAdded) {
                        sql.append(" WHERE ");
                        whereClauseAdded = true;
                    } else {
                        sql.append(" AND ");
                    }
                    String stat = condition.substring(0, 4);
                    String operator = condition.substring(4, 5);
                    String value = condition.substring(5);
                    if (value.endsWith("%")) {
                        String valueNum = value.replace("%", "");
                        Double totalFep = (Double.parseDouble(valueNum) / 10);
                        sql.append("food.").append(stat).append(" * 10 ").append(operator).append(" food.totalFep * ").append(totalFep);
                    } else {
                        sql.append("food.").append(stat).append(" ").append(operator).append(" ").append(value);
                    }
                } else if (Pattern.matches("^sort:(str1|str2|agi1|agi2|int1|int2|con1|con2|per1|per2|cha1|cha2|dex1|dex2|wil1|wil2|psy1|psy2)$", condition)) {
                    String stat = condition.split(":")[1];
                    orderBy = " ORDER BY food." + stat + " DESC";
                    sortAdded = true;
                } else if (Pattern.matches("^from:[a-zA-Z ]{1,50}$", condition)) {
                    if (!whereClauseAdded) {
                        sql.append(" WHERE ");
                        whereClauseAdded = true;
                    } else {
                        sql.append(" AND ");
                    }
                    String ingredientName = condition.split(":")[1];
                    sql.append(" food.id IN (SELECT food_id from food_ingredient WHERE ingredient_id IN (SELECT id from ingredient WHERE name like '%").append(ingredientName).append("%'))");

                } else if (Pattern.matches("^-from:[a-zA-Z ]{1,50}$", condition)) {
                    if (!whereClauseAdded) {
                        sql.append(" WHERE ");
                        whereClauseAdded = true;
                    } else {
                        sql.append(" AND ");
                    }
                    String ingredientName = condition.split(":")[1];
                    sql.append(" food.id NOT IN (SELECT food_id from food_ingredient WHERE ingredient_id IN (SELECT id from ingredient WHERE name like '%").append(ingredientName).append("%'))");
                }
            }

            if (!sortAdded) {
                sql.append(" ORDER BY food.id");
            } else {
                sql.append(orderBy);
            }
            sql.append(" LIMIT 10 OFFSET ");
            sql.append(page * 10 - 10);

            Statement stmt = conn.createStatement();

            ResultSet rs = stmt.executeQuery(sql.toString());

            java.util.List<ParsedFoodInfo> foods = new ArrayList<>();
            List<Integer> foodIds = new ArrayList<>();
            while (rs.next()) {
                ParsedFoodInfo foodInfo = parseFood(rs);
                foods.add(foodInfo);
                foodIds.add(rs.getInt("id"));
            }

            StringBuilder ingredientSql = new StringBuilder("SELECT * FROM food_ingredient JOIN ingredient ON food_ingredient.ingredient_id = ingredient.id WHERE food_ingredient.food_id IN (");
            for (int i = 0; i < foodIds.size(); i++) {
                ingredientSql.append("?");
                if (i < foodIds.size() - 1) {
                    ingredientSql.append(", ");
                }
            }
            ingredientSql.append(")");
            PreparedStatement pstmt = conn.prepareStatement(ingredientSql.toString());
            for (int i = 0; i < foodIds.size(); i++) {
                pstmt.setInt(i + 1, foodIds.get(i));
            }
            ResultSet ingredientRs = pstmt.executeQuery();

            Map<Integer, ParsedFoodInfo> foodMap = foods.stream().collect(Collectors.toMap(food -> food.id, food -> food));
            while (ingredientRs.next()) {
                int foodId = ingredientRs.getInt("food_id");
                ParsedFoodInfo food = foodMap.get(foodId);
                String ingredientName = ingredientRs.getString("name");
                int ingredientPercentage = ingredientRs.getInt("percentage");
                food.ingredients.add(new ParsedFoodInfo.FoodIngredient(ingredientName, ingredientPercentage));
            }
            for (ParsedFoodInfo food : foods) {
                try {
                    addFood(new Food(food));
                } catch (Exception ignored) {
                }
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving foods: " + e.getMessage());
        }
    }

    private ParsedFoodInfo parseFood(ResultSet rs) throws SQLException {
        ParsedFoodInfo foodInfo = new ParsedFoodInfo();
        foodInfo.id = rs.getInt("id");
        foodInfo.itemName = rs.getString("name");
        foodInfo.resourceName = rs.getString("resource");
        foodInfo.energy = rs.getInt("energy");
        foodInfo.hunger = rs.getDouble("hunger");

        String[] fepNames = {"str1", "str2", "agi1", "agi2", "int1", "int2", "con1", "con2", "per1", "per2", "cha1", "cha2", "dex1", "dex2", "wil1", "wil2", "psy1", "psy2"};
        for (String fepName : fepNames) {
            Double fepValue = rs.getDouble(fepName);
            if (fepValue != 0) {
                ParsedFoodInfo.FoodFEP foodFEP = new ParsedFoodInfo.FoodFEP(fepName, fepValue);
                foodInfo.feps.add(foodFEP);
            }
        }
        return foodInfo;
    }


    public void toggleShow() {
        if (show) {
            this.show = false;
            this.hide();
        } else {
            this.show = true;
            this.show();
        }
    }

    @Override
    public void draw(GOut g) {
        super.draw(g);
        g.chcolor(new Color(255, 255, 180, 120));
        g.frect(new Coord(35, 73), UI.scale(new Coord(1, 620)));
        g.frect(new Coord(212, 73), UI.scale(new Coord(1, 620)));
        g.frect(new Coord(262, 73), UI.scale(new Coord(1, 620)));
        g.frect(new Coord(312, 73), UI.scale(new Coord(1, 620)));
        g.frect(new Coord(362, 73), UI.scale(new Coord(1, 620)));
        g.frect(new Coord(412, 73), UI.scale(new Coord(1, 620)));
        g.frect(new Coord(462, 73), UI.scale(new Coord(1, 620)));
        g.frect(new Coord(512, 73), UI.scale(new Coord(1, 620)));
        g.frect(new Coord(562, 73), UI.scale(new Coord(1, 620)));
        g.frect(new Coord(612, 73), UI.scale(new Coord(1, 620)));
        g.frect(new Coord(662, 73), UI.scale(new Coord(1, 620)));
        g.frect(new Coord(1000, 73), UI.scale(new Coord(1, 620)));
        g.frect(new Coord(1065, 73), UI.scale(new Coord(1, 620)));
        g.frect(new Coord(1115, 73), UI.scale(new Coord(1, 620)));


        g.frect(new Coord(35, 73), UI.scale(new Coord(1080, 1)));
        g.frect(new Coord(35, 93), UI.scale(new Coord(1080, 1)));
        g.frect(new Coord(35, 152), UI.scale(new Coord(1080, 1)));
        g.frect(new Coord(35, 213), UI.scale(new Coord(1080, 1)));
        g.frect(new Coord(35, 273), UI.scale(new Coord(1080, 1)));
        g.frect(new Coord(35, 333), UI.scale(new Coord(1080, 1)));
        g.frect(new Coord(35, 393), UI.scale(new Coord(1080, 1)));
        g.frect(new Coord(35, 453), UI.scale(new Coord(1080, 1)));
        g.frect(new Coord(35, 513), UI.scale(new Coord(1080, 1)));
        g.frect(new Coord(35, 573), UI.scale(new Coord(1080, 1)));
        g.frect(new Coord(35, 633), UI.scale(new Coord(1080, 1)));
        g.frect(new Coord(35, 693), UI.scale(new Coord(1080, 1)));


        g.chcolor(new Color(255, 255, 255, 255));
    }

    public void setQuery(String query) {
        this.query = query;
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (Objects.equals(msg, "close"))) {
            this.hide();
            this.show = false;
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    public class FoodList extends Widget {
        ArrayList<Food> foods = new ArrayList<>();
        int rowHeight = UI.scale(60);
        int rows, w;

        public FoodList(int w, int rows) {
            this.rows = rows;
            this.w = w;
            this.sz = new Coord(UI.scale(w), rowHeight * rows);
        }

        public void addFood(Food item) {
            add(item);
            foods.add(item);
        }

        public void removeFood(Food item) {
            item.dispose();
            foods.remove(item);
        }

        public void removeAllFoods() {
            for (Food item : foods) {
                item.dispose();
            }
            foods.clear();
        }

        @Override
        public void draw(GOut g) {
            for (int i = 0; i < rows; i++) {
                if (i >= foods.size())
                    break;
                GOut ig = g.reclip(new Coord(UI.scale(15), i * rowHeight), UI.scale(w - UI.scale(15), rowHeight));
                foods.get(i).draw(ig);
            }
            super.draw(g);
        }

        @Override
        public void wdgmsg(Widget sender, String msg, Object... args) {
            if (msg.equals("delete") && sender instanceof Food) {
                removeFood((Food) sender);
            } else {
                super.wdgmsg(sender, msg, args);
            }
        }
    }

    public static class Food extends Widget {
        private final Label foodName;
        private final Label hunger;
        private final Label energy;
        private final Label str1 = new Label("");
        private final Label str2 = new Label("");
        private final Label agi1 = new Label("");
        private final Label agi2 = new Label("");
        private final Label int1 = new Label("");
        private final Label int2 = new Label("");
        private final Label con1 = new Label("");
        private final Label con2 = new Label("");
        private final Label per1 = new Label("");
        private final Label per2 = new Label("");
        private final Label cha1 = new Label("");
        private final Label cha2 = new Label("");
        private final Label dex1 = new Label("");
        private final Label dex2 = new Label("");
        private final Label wil1 = new Label("");
        private final Label wil2 = new Label("");
        private final Label psy1 = new Label("");
        private final Label psy2 = new Label("");

        private final Label ingredient1 = new Label("");
        private final Label ingredient2 = new Label("");
        private final Label ingredient3 = new Label("");
        private final Label ingredient4 = new Label("");


        public Food(ParsedFoodInfo food) {
            this.foodName = new Label(food.itemName);
            add(this.foodName, UI.scale(5, 25));
            this.hunger = new Label(String.valueOf(food.getHunger()));
            add(this.hunger, UI.scale(977, 25));
            this.energy = new Label(String.valueOf(food.getEnergy()));
            add(this.energy, UI.scale(1037, 25));

            Map<String, Label> labels = new HashMap<>();
            labels.put("str1", str1);
            labels.put("str2", str2);
            labels.put("agi1", agi1);
            labels.put("agi2", agi2);
            labels.put("int1", int1);
            labels.put("int2", int2);
            labels.put("con1", con1);
            labels.put("con2", con2);
            labels.put("per1", per1);
            labels.put("per2", per2);
            labels.put("cha1", cha1);
            labels.put("cha2", cha2);
            labels.put("dex1", dex1);
            labels.put("dex2", dex2);
            labels.put("wil1", wil1);
            labels.put("wil2", wil2);
            labels.put("psy1", psy1);
            labels.put("psy2", psy2);

            Map<String, Integer> scales = new HashMap<>();
            scales.put("str1", 185);
            scales.put("str2", 185);
            scales.put("agi1", 235);
            scales.put("agi2", 235);
            scales.put("int1", 285);
            scales.put("int2", 285);
            scales.put("con1", 335);
            scales.put("con2", 335);
            scales.put("per1", 385);
            scales.put("per2", 385);
            scales.put("cha1", 435);
            scales.put("cha2", 435);
            scales.put("dex1", 485);
            scales.put("dex2", 485);
            scales.put("wil1", 535);
            scales.put("wil2", 535);
            scales.put("psy1", 585);
            scales.put("psy2", 585);

            Map<String, Color> colors = new HashMap<>();
            colors.put("str1", new Color(176, 6, 6, 255));
            colors.put("str2", new Color(213, 0, 0));
            colors.put("agi1", new Color(80, 69, 189));
            colors.put("agi2", new Color(102, 84, 255));
            colors.put("int1", new Color(44, 154, 166));
            colors.put("int2", new Color(0, 231, 255));
            colors.put("con1", new Color(169, 0, 128));
            colors.put("con2", new Color(255, 0, 195));
            colors.put("per1", new Color(180, 91, 0));
            colors.put("per2", new Color(255, 127, 0));
            colors.put("cha1", new Color(20, 171, 0));
            colors.put("cha2", new Color(26, 255, 0));
            colors.put("dex1", new Color(194, 194, 133));
            colors.put("dex2", new Color(255, 255, 180));
            colors.put("wil1", new Color(180, 180, 0, 255));
            colors.put("wil2", new Color(255, 255, 0, 255));
            colors.put("psy1", new Color(144, 0, 201));
            colors.put("psy2", new Color(184, 0, 255));

            for (ParsedFoodInfo.FoodFEP fep : food.feps) {
                String name = fep.getName();
                if (labels.containsKey(name)) {
                    Label label = labels.get(name);
                    label.settext(String.valueOf(fep.getValue()));
                    add(label, UI.scale(scales.get(name) - label.getText().text.length(), (name.endsWith("1") ? 17 : 33)));
                    label.setcolor(colors.get(name));
                }
            }

            if (food.ingredients.size() == 1) {
                ingredient1.settext(food.ingredients.get(0).getName() + " " + food.ingredients.get(0).getPercentage() + "%");
                add(this.ingredient1, UI.scale(625, 8));
            } else if (food.ingredients.size() == 2) {
                ingredient1.settext(food.ingredients.get(0).getName() + " " + food.ingredients.get(0).getPercentage() + "%, " + food.ingredients.get(1).getName() + " " + food.ingredients.get(1).getPercentage() + "%");
                add(this.ingredient1, UI.scale(625, 8));
            } else if (food.ingredients.size() == 3) {
                ingredient1.settext(food.ingredients.get(0).getName() + " " + food.ingredients.get(0).getPercentage() + "%, " + food.ingredients.get(1).getName() + " " + food.ingredients.get(1).getPercentage() + "%,");
                ingredient2.settext(food.ingredients.get(2).getName() + " " + food.ingredients.get(2).getPercentage() + "%");
                add(this.ingredient1, UI.scale(625, 8));
                add(this.ingredient2, UI.scale(625, 21));
            } else if (food.ingredients.size() == 4) {
                ingredient1.settext(food.ingredients.get(0).getName() + " " + food.ingredients.get(0).getPercentage() + "%, " + food.ingredients.get(1).getName() + " " + food.ingredients.get(1).getPercentage() + "%,");
                ingredient2.settext(food.ingredients.get(2).getName() + " " + food.ingredients.get(2).getPercentage() + "%, " + food.ingredients.get(3).getName() + " " + food.ingredients.get(3).getPercentage() + "%");
                add(this.ingredient1, UI.scale(625, 8));
                add(this.ingredient2, UI.scale(625, 21));
            } else if (food.ingredients.size() == 5) {
                ingredient1.settext(food.ingredients.get(0).getName() + " " + food.ingredients.get(0).getPercentage() + "%, " + food.ingredients.get(1).getName() + " " + food.ingredients.get(1).getPercentage() + "%,");
                ingredient2.settext(food.ingredients.get(2).getName() + " " + food.ingredients.get(2).getPercentage() + "%, " + food.ingredients.get(3).getName() + " " + food.ingredients.get(3).getPercentage() + "%,");
                ingredient3.settext(food.ingredients.get(4).getName() + " " + food.ingredients.get(4).getPercentage() + "%");
                add(this.ingredient1, UI.scale(625, 8));
                add(this.ingredient2, UI.scale(625, 21));
                add(this.ingredient3, UI.scale(625, 34));
            } else if (food.ingredients.size() == 6) {
                ingredient1.settext(food.ingredients.get(0).getName() + " " + food.ingredients.get(0).getPercentage() + "%, " + food.ingredients.get(1).getName() + " " + food.ingredients.get(1).getPercentage() + "%,");
                ingredient2.settext(food.ingredients.get(2).getName() + " " + food.ingredients.get(2).getPercentage() + "%, " + food.ingredients.get(3).getName() + " " + food.ingredients.get(3).getPercentage() + "%,");
                ingredient3.settext(food.ingredients.get(4).getName() + " " + food.ingredients.get(4).getPercentage() + "%, " + food.ingredients.get(5).getName() + " " + food.ingredients.get(5).getPercentage() + "%");
                add(this.ingredient1, UI.scale(625, 8));
                add(this.ingredient2, UI.scale(625, 21));
                add(this.ingredient3, UI.scale(625, 34));
            } else if (food.ingredients.size() == 7) {
                ingredient1.settext(food.ingredients.get(0).getName() + " " + food.ingredients.get(0).getPercentage() + "%, " + food.ingredients.get(1).getName() + " " + food.ingredients.get(1).getPercentage() + "%,");
                ingredient2.settext(food.ingredients.get(2).getName() + " " + food.ingredients.get(2).getPercentage() + "%, " + food.ingredients.get(3).getName() + " " + food.ingredients.get(3).getPercentage() + "%,");
                ingredient3.settext(food.ingredients.get(4).getName() + " " + food.ingredients.get(4).getPercentage() + "%, " + food.ingredients.get(5).getName() + " " + food.ingredients.get(5).getPercentage() + "%,");
                ingredient4.settext(food.ingredients.get(6).getName() + " " + food.ingredients.get(6).getPercentage() + "%");
                add(this.ingredient1, UI.scale(625, 8));
                add(this.ingredient2, UI.scale(625, 21));
                add(this.ingredient3, UI.scale(625, 34));
                add(this.ingredient4, UI.scale(625, 47));
            } else if (food.ingredients.size() == 8) {
                ingredient1.settext(food.ingredients.get(0).getName() + " " + food.ingredients.get(0).getPercentage() + "%, " + food.ingredients.get(1).getName() + " " + food.ingredients.get(1).getPercentage() + "%,");
                ingredient2.settext(food.ingredients.get(2).getName() + " " + food.ingredients.get(2).getPercentage() + "%, " + food.ingredients.get(3).getName() + " " + food.ingredients.get(3).getPercentage() + "%,");
                ingredient3.settext(food.ingredients.get(4).getName() + " " + food.ingredients.get(4).getPercentage() + "%, " + food.ingredients.get(5).getName() + " " + food.ingredients.get(5).getPercentage() + "%,");
                ingredient4.settext(food.ingredients.get(6).getName() + " " + food.ingredients.get(6).getPercentage() + "%, " + food.ingredients.get(7).getName() + " " + food.ingredients.get(7).getPercentage() + "%");
                add(this.ingredient1, UI.scale(625, 8));
                add(this.ingredient2, UI.scale(625, 21));
                add(this.ingredient3, UI.scale(625, 34));
                add(this.ingredient4, UI.scale(625, 47));
            }
        }

        @Override
        public void draw(GOut g) {
            super.draw(g);
        }
    }
}

