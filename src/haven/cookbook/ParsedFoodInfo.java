package haven.cookbook;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

public class ParsedFoodInfo implements Serializable {
    public Integer id;
    public String itemName;
    public String resourceName;
    public Integer energy;
    public double hunger;
    public ArrayList<FoodIngredient> ingredients;
    public ArrayList<FoodFEP> feps;

    public ParsedFoodInfo() {
        this.itemName = "";
        this.resourceName = "";
        this.ingredients = new ArrayList<>();
        this.feps = new ArrayList<>();
    }

    @Override
    public String toString() {
        return "ParsedFoodInfo{" +
                "itemName='" + itemName + '\'' +
                ", resourceName='" + resourceName + '\'' +
                ", energy=" + energy +
                ", hunger=" + hunger +
                ", ingredients=" + ingredients +
                ", feps=" + feps +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ParsedFoodInfo that = (ParsedFoodInfo) obj;
        return itemName.equals(that.itemName) &&
                resourceName.equals(that.resourceName) &&
                ingredients.equals(that.ingredients);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemName, resourceName, ingredients);
    }

    public String getItemName() {
        return itemName;
    }

    public String getResourceName() {
        return resourceName;
    }

    public Integer getEnergy() {
        return energy;
    }

    public double getHunger() {
        return hunger;
    }

    public ArrayList<FoodIngredient> getIngredients() {
        return ingredients;
    }

    public ArrayList<FoodFEP> getFeps() {
        return feps;
    }


    public static class FoodIngredient implements Serializable {
        private final String name;
        private final Integer percentage;

        public FoodIngredient(String name, Integer percentage) {
            this.name = name;
            this.percentage = percentage;
        }

        @Override
        public String toString() {
            return "FoodIngredient{" +
                    "name='" + name + '\'' +
                    ", percentage=" + percentage +
                    '}';
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            FoodIngredient that = (FoodIngredient) obj;
            return Objects.equals(name, that.name) &&
                    Objects.equals(percentage, that.percentage);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, percentage);
        }

        public String getName() {
            return name;
        }

        public Integer getPercentage() {
            return percentage;
        }
    }
    public static class FoodFEP implements Serializable {
        private final String name;
        private final Double value;

        public FoodFEP(String name, Double value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return "FoodFEP{" +
                    "name='" + name + '\'' +
                    ", value=" + value +
                    '}';
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value);
        }

        public String getName() {
            return name;
        }

        public Double getValue() {
            return value;
        }
    }
}