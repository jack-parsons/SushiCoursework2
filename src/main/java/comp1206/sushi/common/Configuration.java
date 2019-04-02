package comp1206.sushi.common;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Configuration {
    private Restaurant restaurant;
    private Map<String, Postcode> postcodes = new HashMap<>();
    private Map<String, Supplier> supplier = new HashMap<>();
    private Map<String, Ingredient> ingredients = new HashMap<>();
    private Map<String, Dish> dishes = new HashMap<>();
    private Map<String, User> users = new HashMap<>();
    private Map<String, Staff> staff = new HashMap<>();
    private Map<String, Drone> drones = new HashMap<>();
    private Map<String, Order> orders = new HashMap<>();
    private StockManager stockManager = new StockManager();

    public Configuration(String filename) {
        File configFile = new File(filename);
        if (configFile.exists()) {
            try {
                parseFile(configFile);
            } catch (IOException e) {
                throw new IllegalArgumentException("File: " + filename + "cannot be read");
            }
        } else {
            throw new IllegalArgumentException("File: " + filename + "does not exit");
        }
    }

    private void parseFile(File configFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(configFile));
        String[] parts;
        while ((parts=splitLine(reader.readLine())) != null) {
            if (parts.length >= 1 && !parts[0].equals("")) {
                try {
                    switch (parts[0]) {
                        case "RESTAURANT":
                            restaurant = new Restaurant(parts[1], postcodes.get(parts[2]));
                            break;
                        case "POSTCODE":
                            postcodes.put(parts[1], new Postcode(parts[1]));
                            break;
                        case "SUPPLIER":
                            supplier.put(parts[1], new Supplier(parts[1], postcodes.get(parts[2])));
                            break;
                        case "INGREDIENT":
                            ingredients.put(parts[1], new Ingredient(parts[1], parts[2], supplier.get(parts[3]),
                                    Integer.parseInt(parts[4]), Integer.parseInt(parts[5]), Integer.parseInt(parts[6])));
                            break;
                        case "DISH":
                            dishes.put(parts[1], new Dish(parts[1], parts[2],
                                    Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), Integer.parseInt(parts[5])));
                            break;
                        case "USER":
                            users.put(parts[1], new User(parts[1], parts[2], parts[3], postcodes.get(parts[4])));
                            break;
                        case "STAFF":
                            staff.put(parts[1], new Staff(parts[1]));
                            break;
                        case "DRONE":
                            drones.put(parts[1], new Drone(Integer.parseInt(parts[1])));
                            break;
                        case "ORDER":
                            Order order = new Order();
                            for (String dish : parts[2].split(",")) {
                                String[] dishParts = dish.split("\\*");
                                order.addDish(dishes.get(dishParts[1].trim()), Integer.parseInt(dishParts[0].trim()));
                            }
                            orders.put(parts[1], order);
                            break;
                        case "STOCK":
                            if (dishes.containsKey(parts[1])) {
                                stockManager.setDishStock(dishes.get(parts[1]), Integer.parseInt(parts[2]));
                            } else if (ingredients.containsKey(parts[1])) {
                                stockManager.setIngredientsStock(ingredients.get(parts[1]), Integer.parseInt(parts[2]));
                            }
                            break;
                        default:
                            throw new IllegalArgumentException("Model type not valid: " + parts[0]);
                    }
                } catch (IndexOutOfBoundsException e) {
                    throw new IllegalArgumentException(String.format("Illegal number of parameters: %d for model %s", parts.length, parts[0]));
                }
            }
        }
    }

    private String[] splitLine(String line) {
        if (line == null) {
            return null;
        }
        String[] lines = line.split(":");
        for (int i = 0; i < lines.length; i ++) {
            lines[i] = lines[i].trim();
        }
        return lines;
    }



    public Restaurant getRestaurant() {
        return restaurant;
    }

    public ArrayList<Postcode> getPostcodes() {
        return new ArrayList<>(postcodes.values());
    }

    public ArrayList<Supplier> getSupplier() {
        return new ArrayList<>(supplier.values());
    }

    public ArrayList<Ingredient> getIngredients() {
        return new ArrayList<>(ingredients.values());
    }

    public ArrayList<Dish> getDishes() {
        return new ArrayList<>(dishes.values());
    }

    public ArrayList<User> getUsers() {
        return new ArrayList<>(users.values());
    }

    public ArrayList<Staff> getStaff() {
        return new ArrayList<>(staff.values());
    }

    public ArrayList<Drone> getDrones() {
        return new ArrayList<>(drones.values());
    }

    public ArrayList<Order> getOrders() {
        return new ArrayList<>(orders.values());
    }

    public StockManager getStockManager() {
        return stockManager;
    }
}
