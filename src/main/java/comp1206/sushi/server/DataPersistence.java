package comp1206.sushi.server;

import comp1206.sushi.common.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public class DataPersistence {

    private File file;
    private PrintWriter printWriter;


    public DataPersistence(File file) {
        try {
            boolean fileExists = file.createNewFile();
            if (!fileExists) {
                System.out.println("File created");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.file = file;
    }

    public void writeStateToFile(Restaurant restaurant, List<Dish> dishes, List<Drone> drones, List<Ingredient> ingredients,
                                 List<Order> orders, List<Staff> staff, List<Supplier> suppliers, List<User> users,
                                 List<Postcode> postcodes, StockManager stockManager) {
        try {
            printWriter = new PrintWriter(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        for (Postcode postcode : postcodes) {
            printWriter.printf("POSTCODE:%s \n", postcode.getName());
        }

        if (!postcodes.contains(restaurant.getLocation())) {
            printWriter.printf("POSTCODE:%s \n", restaurant.getLocation().getName());
        }
        printWriter.printf("RESTAURANT:%s:%s \n", restaurant.getName(), restaurant.getLocation().getName());

        for (Supplier supplier : suppliers) {
            printWriter.printf("SUPPLIER:%s:%s \n", supplier.getName(), supplier.getPostcode().getName());
        }

        for (Ingredient ingredient: ingredients) {
            printWriter.printf("INGREDIENT:%s:%s:%s:%s:%s:%s \n", ingredient.getName(), ingredient.getUnit(),
                    ingredient.getSupplier().getName(), ingredient.getRestockThreshold(), ingredient.getRestockAmount(),
                    ingredient.getWeight());
        }

        for (Dish dish : dishes) {
            printWriter.printf("DISH:%s:%s:%s:%s:%s:%s \n", dish.getName(), dish.getDescription(), dish.getPrice(), dish.getRestockThreshold(),
                    dish.getRestockAmount(), generateDishConfigString(dish.getRecipe()));
        }

        for (User user : users) {
            printWriter.printf("USER:%s:%s:%s:%s \n", user.getName(), user.getPassword(), user.getAddress(), user.getPostcode().getName());
        }

        for (Order order : orders) {
            printWriter.printf("ORDER:%s:%s \n", order.getUser().getName(), order.toString());
        }

        for (Dish dish : dishes) {
            printWriter.printf("STOCK:%s:%s \n", dish.getName(), stockManager.getDishStock(dish).intValue());
        }
        for (Ingredient ingredient : ingredients) {
            printWriter.printf("STOCK:%s:%s \n", ingredient.getName(), stockManager.getIngredientsStock(ingredient).intValue());
        }

        for (Staff staffMember : staff) {
            printWriter.printf("STAFF:%s \n", staffMember.getName());
        }

        for (Drone drone : drones) {
            printWriter.printf("DRONE:%s \n", drone.getSpeed());
        }
        printWriter.flush();
        printWriter.close();
    }

    private static String generateDishConfigString(Map<Ingredient, Number> quantifiedItems) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry item : quantifiedItems.entrySet()) {
            sb.append(item.getValue()).append(" * ").append(item.getKey()).append(",");
        }
        return sb.toString();
    }
}
