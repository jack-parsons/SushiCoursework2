package comp1206.sushi.server;

import comp1206.sushi.common.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
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
            printWriter.printf("POSTCODE:%s\n", postcode.getName());
        }

        if (!postcodes.contains(restaurant.getLocation())) {
            printWriter.printf("POSTCODE:%s\n", restaurant.getLocation().getName());
        }
        printWriter.printf("RESTAURANT:%s:%s\n", restaurant.getName(), restaurant.getLocation().getName());

        for (Supplier supplier : suppliers) {
            printWriter.printf("SUPPLIER:%s:%s\n", supplier.getName(), supplier.getPostcode().getName());
        }

        for (Ingredient ingredient: ingredients) {
            printWriter.printf("INGREDIENT:%s:%s:%s:%s:%s:%s\n", ingredient.getName(), ingredient.getUnit(),
                    ingredient.getSupplier().getName(), ingredient.getRestockThreshold(), ingredient.getRestockAmount(),
                    ingredient.getWeight());
        }

        for (Dish dish : dishes) {
            //Name:Description:Price:Restock Threshold:Restock Amount:Quantity * Item,Quantity * Item...
            printWriter.printf("DISH:%s:%s:%s:%s:%s:%s\n", dish.getName(), dish.getDescription(), dish.getPrice(), dish.getRestockThreshold(),
                    dish.getRestockAmount(), dish.gete);
        }
    }

    private static String generateQuantifiedConfigString(Map<Model, Number> quantifiedItems) {

    }
}
