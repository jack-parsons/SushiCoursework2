package comp1206.sushi.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StockManager {
    private Map<Dish, Number> dishStock = new HashMap<>();
    private Map<Ingredient, Number> ingredientStock = new HashMap<>();
    private boolean restockingIngredients = true;
    private boolean restockingDishes = true;
    private Map<Dish, Number> inProgressDishes = new HashMap<>();

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

    /**
     * Checks the stock levels of dishes, and if there is enough ingredients to create dish
     * @return A dish that needs restocking if any, else null
     */
    synchronized public Dish findDishToPrepare() {
        for (Dish dish : dishStock.keySet()) {
            if (dishStock.get(dish).doubleValue() < dish.getRestockThreshold().doubleValue()) {
                boolean hasStock = true;
                for (Ingredient ingredient : dish.getRecipe().keySet()) {

                    // If the amount required is greater than the amount in stock and being prepared
                    Number amountBeingPrepared = inProgressDishes.getOrDefault(dish, 0);
                    if (dish.getRecipe().get(ingredient).doubleValue() * dish.getRestockAmount().doubleValue()
                            > ingredientStock.get(ingredient).doubleValue() + amountBeingPrepared.doubleValue()) {
                        hasStock = false;
                    }
                }
                if (hasStock) {
                    inProgressDishes.put(dish, inProgressDishes.getOrDefault(
                            dish, 0).doubleValue() + dish.getRestockAmount().doubleValue());
                    return dish;
                }
            }
        }
        return null;
    }

    synchronized public void dishFinished(Dish dish) {
        inProgressDishes.remove(dish);
        setDishStock(dish, getDishStock(dish).intValue() + dish.getRestockAmount().intValue());
    }
}
