package comp1206.sushi.common;

import java.util.HashMap;
import java.util.Map;

public class StockManager {
    private Map<Dish, Number> dishStock = new HashMap<>();
    private Map<Ingredient, Number> ingredientStock = new HashMap<>();

    public Number getDishStock(Dish dish) {
        return dishStock.get(dish);
    }

    public Number getIngredientsStock(Ingredient dish) {
        return ingredientStock.get(dish);
    }

    public Number setDishStock(Dish dish, Number quantity) {
        return dishStock.put(dish, quantity);
    }

    public Number setIngredientsStock(Ingredient dish, Number quantity) {
        return ingredientStock.put(dish, quantity);
    }
}
