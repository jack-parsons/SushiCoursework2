package comp1206.sushi.common;

import com.sun.xml.internal.ws.api.ha.StickyFeature;

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
    private Map<Ingredient, Number> inTransitIngredients = new HashMap<>();
    private List<Order> orders = new ArrayList<>();

    public void setOrders(List<Order> orders) {
        this.orders = orders;
    }

    public void initStock(List<Dish> dishes) {
        for (Dish dish : dishes) {
            if (!dishStock.containsKey(dish)) {
                dishStock.put(dish, 0);
            }
        }
    }

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
        System.out.println(quantity);
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
            if (dishStock.get(dish).doubleValue() + inProgressDishes.getOrDefault(dish, 0).doubleValue() <
                    dish.getRestockThreshold().doubleValue()) {
                boolean hasStock = true;
                for (Ingredient ingredient : dish.getRecipe().keySet()) {

                    // If the amount required is greater than the amount in stock and being prepared
                    Number amountBeingPrepared = inProgressDishes.getOrDefault(dish, 0);
                    if (dish.getRecipe().get(ingredient).doubleValue() * dish.getRestockAmount().doubleValue()
                            >= ingredientStock.get(ingredient).doubleValue() + amountBeingPrepared.doubleValue()) {
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
        inProgressDishes.put(dish, inProgressDishes.getOrDefault(
                dish, 0).doubleValue() - dish.getRestockAmount().doubleValue());
        setDishStock(dish, getDishStock(dish).intValue() + dish.getRestockAmount().intValue());
    }

    synchronized public Ingredient findIngredientToRestock(Number capacityLeft) {
        for (Ingredient ingredient : ingredientStock.keySet()) {
            if (ingredientStock.get(ingredient).floatValue() + inTransitIngredients.getOrDefault(ingredient, 0).floatValue() < ingredient.getRestockThreshold().floatValue()) {
                // If insufficient stock in reserves then produce more
                informRestockingIngredient(ingredient, Math.min(capacityLeft.floatValue(), ingredient.getRestockAmount().floatValue()));
                return ingredient;
            }
        }
        return null;
    }

    synchronized public void informRestockingIngredient(Ingredient ingredient, Number quantity) {
        inTransitIngredients.put(ingredient, quantity.floatValue() + inTransitIngredients.getOrDefault(ingredient, 0).floatValue());
    }

    synchronized public void informDeliveryCompleted(Ingredient ingredient, Number quantity) {
        inTransitIngredients.put(ingredient, inTransitIngredients.getOrDefault(ingredient, 0).floatValue() - quantity.floatValue());
    }

    synchronized public Order findOrderToDeliver() {
        for (Order order : orders) {
            if (!order.isComplete() && !order.isBeingDelivered()) {
                boolean suffientStock = true;
                for (Dish dish : order.getDishQuantities().keySet()) {
                    if (dishStock.get(dish).floatValue() < order.getDishQuantities().get(dish).floatValue()) {
                        suffientStock = false;
                    }
                }
                if (suffientStock) {
                    order.startDelivery();
                    return order;
                }
            }
        }
        return null;
    }
}
