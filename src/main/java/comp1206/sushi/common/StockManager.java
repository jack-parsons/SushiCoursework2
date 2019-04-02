package comp1206.sushi.common;

import java.util.HashMap;
import java.util.Map;

public class StockManager {
    private Map<Dish, Number> dishStock = new HashMap<>();
    private Map<Ingredient, Number> ingredientStock = new HashMap<>();
    private boolean restockingIngredients = true;
    private boolean restockingDishes = true;

    public Number getDishStock(Dish dish) {
        Number stock = dishStock.get(dish);
        return stock == null ? 0 : stock;
    }

    public Number getIngredientsStock(Ingredient ingredient) {
        Number stock = ingredientStock.get(ingredient);
        return stock == null ? 0 : stock;
    }

    public Number setDishStock(Dish dish, Number quantity) {
        return dishStock.put(dish, quantity);
    }

    public Number setIngredientsStock(Ingredient dish, Number quantity) {
        return ingredientStock.put(dish, quantity);
    }

    public void setRestockingIngredientsEnabled(boolean enabled) {
        restockingIngredients = enabled;
    }

    public void setRestockingDishesEnabled(boolean enabled) {
        restockingDishes = enabled;
    }
}
