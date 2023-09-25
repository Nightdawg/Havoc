package haven.automated.cookbook;

import java.util.ArrayList;
import java.util.Objects;

public class ParsedFoodInfo {
    public Integer id;
    public String hash;
    public String itemName;
    public String resourceName;
    public Integer energy;
    public double hunger;
    public double totalFep;
    public Double str1, str2, agi1, agi2, int1, int2, con1, con2, per1, per2, cha1, cha2, dex1, dex2, wil1, wil2, psy1, psy2;
    public ArrayList<FoodIngredient> ingredients;

    public ParsedFoodInfo() {
        this.id = null;
        this.itemName = "";
        this.resourceName = "";
        this.energy = 0;
        this.hunger = 0.0;
        this.totalFep = 0.0;
        this.str1 = this.str2 = this.agi1 = this.agi2 = this.int1 = this.int2 = this.con1 = this.con2 = this.per1 = this.per2 = this.cha1 = this.cha2 = this.dex1 = this.dex2 = this.wil1 = this.wil2 = this.psy1 = this.psy2 = 0.0;
        this.ingredients = new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParsedFoodInfo that = (ParsedFoodInfo) o;
        return  Objects.equals(id, that.id) &&
                Objects.equals(itemName, that.itemName) &&
                Objects.equals(resourceName, that.resourceName) &&
                Objects.equals(energy, that.energy) &&
                Double.compare(that.hunger, hunger) == 0 &&
                Double.compare(that.totalFep, totalFep) == 0 &&
                Objects.equals(str1, that.str1) &&
                Objects.equals(str2, that.str2) &&
                Objects.equals(agi1, that.agi1) &&
                Objects.equals(agi2, that.agi2) &&
                Objects.equals(int1, that.int1) &&
                Objects.equals(int2, that.int2) &&
                Objects.equals(con1, that.con1) &&
                Objects.equals(con2, that.con2) &&
                Objects.equals(per1, that.per1) &&
                Objects.equals(per2, that.per2) &&
                Objects.equals(cha1, that.cha1) &&
                Objects.equals(cha2, that.cha2) &&
                Objects.equals(dex1, that.dex1) &&
                Objects.equals(dex2, that.dex2) &&
                Objects.equals(wil1, that.wil1) &&
                Objects.equals(wil2, that.wil2) &&
                Objects.equals(psy1, that.psy1) &&
                Objects.equals(psy2, that.psy2) &&
                Objects.equals(ingredients, that.ingredients);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, hash, itemName, resourceName, energy, hunger, totalFep,
                str1, str2, agi1, agi2, int1, int2, con1, con2, per1, per2, cha1, cha2, dex1, dex2, wil1, wil2, psy1, psy2, ingredients);
    }

    @Override
    public String toString() {
        return "ParsedFoodInfo{" +
                "hash='" + hash + '\'' +
                ", itemName='" + itemName + '\'' +
                ", resourceName='" + resourceName + '\'' +
                ", energy=" + energy +
                ", hunger=" + hunger +
                ", totalFep=" + totalFep +
                ", str1=" + str1 +
                ", str2=" + str2 +
                ", agi1=" + agi1 +
                ", agi2=" + agi2 +
                ", int1=" + int1 +
                ", int2=" + int2 +
                ", con1=" + con1 +
                ", con2=" + con2 +
                ", per1=" + per1 +
                ", per2=" + per2 +
                ", cha1=" + cha1 +
                ", cha2=" + cha2 +
                ", dex1=" + dex1 +
                ", dex2=" + dex2 +
                ", wil1=" + wil1 +
                ", wil2=" + wil2 +
                ", psy1=" + psy1 +
                ", psy2=" + psy2 +
                ", ingredients=" + ingredients +
                '}';
    }

    public static class FoodIngredient {
        public final String name;
        public final Integer percentage;

        public FoodIngredient(String name, Integer percentage) {
            this.name = name;
            this.percentage = percentage;
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

        @Override
        public String toString() {
            return "FoodIngredient{" +
                    "name='" + name + '\'' +
                    ", percentage=" + percentage +
                    '}';
        }
    }
}