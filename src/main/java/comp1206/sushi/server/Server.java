package comp1206.sushi.server;

import java.io.IOException;
import java.net.SocketException;
import java.util.*;
import java.util.Map.Entry;

import comp1206.sushi.client.Client;
import comp1206.sushi.common.*;
import comp1206.sushi.common.Comms;
import javafx.geometry.Pos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
 
public class Server implements ServerInterface {

    private static final Logger logger = LogManager.getLogger("Server");
	
	private Restaurant restaurant;
	private ArrayList<Dish> dishes = new ArrayList<>();
	private ArrayList<Drone> drones = new ArrayList<>();
	private ArrayList<Ingredient> ingredients = new ArrayList<>();
	private ArrayList<Order> orders = new ArrayList<>();
	private ArrayList<Staff> staff = new ArrayList<>();
	private ArrayList<Supplier> suppliers = new ArrayList<>();
	private ArrayList<User> users = new ArrayList<>();
	private ArrayList<Postcode> postcodes = new ArrayList<>();
	private StockManager stockManager = new StockManager();
	private ArrayList<UpdateListener> listeners = new ArrayList<>();

	private ServerCommsController commsController;
	
	public Server() {
        logger.info("Starting up server...");

//        loadConfiguration("Configuration.txt");

		commsController = new ServerCommsController();
		
		Postcode restaurantPostcode = new Postcode("SO17 1BJ");
		restaurant = new Restaurant("Mock Restaurant",restaurantPostcode);

		Postcode postcode1 = addPostcode("SO17 1TJ");
		Postcode postcode2 = addPostcode("SO17 1BX");
		Postcode postcode3 = addPostcode("SO17 2NJ");
		addPostcode("SO17 1TW");
		addPostcode("SO17 2LB");

		Supplier supplier1 = addSupplier("Supplier 1",postcode1);
		Supplier supplier2 = addSupplier("Supplier 2",postcode2);
		Supplier supplier3 = addSupplier("Supplier 3",postcode3);

		Ingredient ingredient1 = addIngredient("Ingredient 1","grams",supplier1,1,5,1);
		Ingredient ingredient2 = addIngredient("Ingredient 2","grams",supplier2,1,5,1);
		Ingredient ingredient3 = addIngredient("Ingredient 3","grams",supplier3,1,5,1);

		Dish dish1 = addDish("Dish 1","Dish 1",1,1,10);
		Dish dish2 = addDish("Dish 2","Dish 2",2,1,10);
		Dish dish3 = addDish("Dish 3","Dish 3",3,1,10);

//		orders.add(new Order());

		addIngredientToDish(dish1,ingredient1,1);
		addIngredientToDish(dish1,ingredient2,2);
		addIngredientToDish(dish2,ingredient2,3);
		addIngredientToDish(dish2,ingredient3,1);
		addIngredientToDish(dish3,ingredient1,2);
		addIngredientToDish(dish3,ingredient3,1);

		addStaff("Staff 1");
		addStaff("Staff 2");
		addStaff("Staff 3");

		addDrone(1);
		addDrone(2);
		addDrone(3);
		addDrone(30);

		startStaff();
		startDrones();

		addUpdateListener(updateEvent -> {
			for (ClientConnection clientConnection : commsController.getClientConnections()) {
				updateClient(clientConnection);
			}
			stockManager.setOrders(orders);
		});

		new Thread(commsController).start();

		new Thread(() -> {
			while(true) {
				for (ClientConnection clientConnection : commsController.getClientConnections()) {
					try {
						if (!clientConnection.checkUpdated()) {
							updateClient(clientConnection);
							clientConnection.sendMessage(Comms.MessageType.FINISH_INIT.name());
						}
						String reply = "";
						try {
							reply = clientConnection.receiveMessage();
						} catch (SocketException e) {
							if (clientConnection.getUser() != null)
								System.out.println("Client disconnected: " + clientConnection.getUser().getName());
							else
								System.out.println("Client disconnected");
							commsController.removeClientConnection(clientConnection);
							continue;
						}
						System.out.println(reply);
						if (reply != null && Comms.extractMessageType(reply) != null) {
							switch (Objects.requireNonNull(Comms.extractMessageType(reply))) {
								case REGISTER:
									// Add the new user to server
									String username = Comms.extractMessageAttribute(reply, Comms.MessageAttribute.USERNAME);
									String password = Comms.extractMessageAttribute(reply, Comms.MessageAttribute.PASSWORD);
									String address = Comms.extractMessageAttribute(reply, Comms.MessageAttribute.ADDRESS);
									String postcodeRaw = Comms.extractMessageAttribute(reply, Comms.MessageAttribute.POSTCODE);
									Postcode postcode = null;
									for (Postcode postcodeTest : postcodes) {
										if(postcodeTest.getName().equals(postcodeRaw)) {
											postcode = postcodeTest;
										}
									}
									if (postcode == null) {
										throw new IllegalArgumentException("Postcode not found: " + postcodeRaw);
									}
									users.add(new User(username, password, address, postcode));
								case LOGIN:
									for (User user : users) {
										if (user.getName().equals(Comms.extractMessageAttribute(reply, Comms.MessageAttribute.USERNAME))) {
											if (user.checkPassword(Comms.extractMessageAttribute(reply, Comms.MessageAttribute.PASSWORD))) {
												clientConnection.setUser(user);
												clientConnection.sendMessage(String.format("NEW_USER|ADDRESS=%s|POSTCODE=%s", "", user.getPostcode()));
											} else {
												clientConnection.sendMessage("LOGIN_REJECTED");
											}
										}
									}
									break;
								case ADD_ORDER:
									if (clientConnection.getUser() != null) {
										String dishesRaw = Comms.extractMessageAttribute(reply, Comms.MessageAttribute.DISHES);
										Order order;
										if (dishesRaw != null) {
											Map<String, Dish> dishMap = new HashMap<>();
											for (Dish dish : dishes)
												dishMap.put(dish.getName(), dish);
											order = Configuration.retrieveOrder(clientConnection.getUser(), dishesRaw, dishMap);
											order.setUser(clientConnection.getUser());
										} else {
											order = new Order(clientConnection.getUser());
										}

										order.setName(Comms.extractMessageAttribute(reply, Comms.MessageAttribute.NAME));
										clientConnection.getUser().getOrders().add(order);
										addOrder(order);
									}
									break;
								case CANCEL_ORDER:
									String orderName = Comms.extractMessageAttribute(reply, Comms.MessageAttribute.NAME);
									orders.removeIf(order -> order.getName().equals(orderName));
									break;
								case BASKET_UPDATE:
									String dishesRaw = Comms.extractMessageAttribute(reply, Comms.MessageAttribute.DISHES);
									Order order;
									if (dishesRaw != null) {
										Map<String, Dish> dishMap = new HashMap<>();
										for (Dish dish : dishes)
											dishMap.put(dish.getName(), dish);
										order = Configuration.retrieveOrder(clientConnection.getUser(), dishesRaw, dishMap);
										clientConnection.getUser().updateBasket(order.getDishQuantities());
									}
							}
						}

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();

	}

	private void addOrder(Order order) {
		orders.add(order);
		stockManager.setOrders(orders);
	}

	private void updateClient(ClientConnection clientConnection) {
	    clientConnection.sendMessage("CLEAR_POSTCODES");
	    for (Postcode postcode : postcodes) {
            clientConnection.sendMessage("ADD_POSTCODE|POSTCODE="+postcode);
        }

		clientConnection.sendMessage("CLEAR_DISHES");
        for (Dish dish : dishes) {
			clientConnection.sendMessage(String.format("ADD_DISH|NAME=%s|DESCRIPTION=%s|PRICE=%s", dish.getName(), dish.getDescription(), dish.getPrice()));
		}
//
		if (clientConnection.getUser() != null) {
			clientConnection.sendMessage("CLEAR_ORDERS");
			for (Order order : clientConnection.getUser().getOrders()) {
				clientConnection.sendMessage(String.format("ADD_ORDER|DISHES=%s", order));
			}
		}

		clientConnection.sendMessage(String.format("ADD_RESTAURANT|NAME=%s|POSTCODE=%s", restaurant.getName(), restaurant.getLocation()));
    }
	
	@Override
	public List<Dish> getDishes() {
		return this.dishes;
	}

	@Override
	public Dish addDish(String name, String description, Number price, Number restockThreshold, Number restockAmount) {
		Dish newDish = new Dish(name,description,price,restockThreshold,restockAmount);
		this.dishes.add(newDish);
		this.notifyUpdate();
		stockManager.setDishStock(newDish, 0);
		return newDish;
	}
	
	@Override
	public void removeDish(Dish dish) {
		this.dishes.remove(dish);
		this.notifyUpdate();
		// TODO deal with stock manager stock when removed
	}

	@Override
	public Map<Dish, Number> getDishStockLevels() {
		List<Dish> dishes = getDishes();
		HashMap<Dish, Number> levels = new HashMap<>();
		for(Dish dish : dishes) {
			levels.put(dish, stockManager.getDishStock(dish));
		}
		return levels;
	}
	
	@Override
	public void setRestockingIngredientsEnabled(boolean enabled) {
		stockManager.setRestockingIngredientsEnabled(enabled);
	}

	@Override
	public void setRestockingDishesEnabled(boolean enabled) {
		stockManager.setRestockingDishesEnabled(enabled);
	}
	
	@Override
	public void setStock(Dish dish, Number stock) {
		stockManager.setDishStock(dish, stock);
	}

	@Override
	public void setStock(Ingredient ingredient, Number stock) {
		stockManager.setIngredientsStock(ingredient, stock);
	}



	@Override
	public List<Ingredient> getIngredients() {
		return this.ingredients;
	}

	@Override
	public Ingredient addIngredient(String name, String unit, Supplier supplier,
			Number restockThreshold, Number restockAmount, Number weight) {
		Ingredient mockIngredient = new Ingredient(name,unit,supplier,restockThreshold,restockAmount,weight);
		this.ingredients.add(mockIngredient);
		this.notifyUpdate();
		stockManager.setIngredientsStock(mockIngredient, 0);
		return mockIngredient;
	}

	@Override
	public void removeIngredient(Ingredient ingredient) {
		this.ingredients.remove(ingredient);
		this.notifyUpdate();
	}

	@Override
	public List<Supplier> getSuppliers() {
		return this.suppliers;
	}

	@Override
	public Supplier addSupplier(String name, Postcode postcode) {
		Supplier mock = new Supplier(name,postcode);
		this.suppliers.add(mock);
		return mock;
	}


	@Override
	public void removeSupplier(Supplier supplier) {
		this.suppliers.remove(supplier);
		this.notifyUpdate();
	}

	@Override
	public List<Drone> getDrones() {
		return this.drones;
	}

	@Override
	public Drone addDrone(Number speed) {
		Drone mock = new Drone(speed, stockManager, restaurant);
		this.drones.add(mock);
		return mock;
	}

	@Override
	public void removeDrone(Drone drone) {
		// TODO stop deletion if in use
		this.drones.remove(drone);
		this.notifyUpdate();
	}

	@Override
	public List<Staff> getStaff() {
		return this.staff;
	}

	@Override
	public Staff addStaff(String name) {
		Staff mock = new Staff(name, stockManager);
		this.staff.add(mock);
		return mock;
	}

	@Override
	public void removeStaff(Staff staff) {
		this.staff.remove(staff);
		this.notifyUpdate();
	}

	@Override
	public List<Order> getOrders() {
		return this.orders;
	}

	@Override
	public void removeOrder(Order order) {
		this.orders.remove(order);
		this.notifyUpdate();
	}
	
	@Override
	public Number getOrderCost(Order order) {
		Random random = new Random();
		return random.nextInt(100);
	}

	@Override
	public Map<Ingredient, Number> getIngredientStockLevels() {
		HashMap<Ingredient, Number> levels = new HashMap<>();
		for(Ingredient ingredient : getIngredients()) {
			levels.put(ingredient, stockManager.getIngredientsStock(ingredient));
		}
		return levels;
	}

	@Override
	public Number getSupplierDistance(Supplier supplier) {
		return supplier.getDistance();
	}

	@Override
	public Number getDroneSpeed(Drone drone) {
		return drone.getSpeed();
	}

	@Override
	public Number getOrderDistance(Order order) {
		return order.getDistance();
	}

	@Override
	public void addIngredientToDish(Dish dish, Ingredient ingredient, Number quantity) {
		if(quantity.equals(0)) {
			removeIngredientFromDish(dish,ingredient);
		} else {
			dish.getRecipe().put(ingredient,quantity);
		}
	}

	@Override
	public void removeIngredientFromDish(Dish dish, Ingredient ingredient) {
		dish.getRecipe().remove(ingredient);
		this.notifyUpdate();
	}

	@Override
	public Map<Ingredient, Number> getRecipe(Dish dish) {
		return dish.getRecipe();
	}

	@Override
	public List<Postcode> getPostcodes() {
		return this.postcodes;
	}

	@Override
	public Postcode addPostcode(String code) {
		Postcode mock = new Postcode(code);
		this.postcodes.add(mock);
		this.notifyUpdate();
		return mock;
	}

	@Override
	public void removePostcode(Postcode postcode) throws UnableToDeleteException {
		this.postcodes.remove(postcode);
		// TODO make throw exception if cannot delete
		this.notifyUpdate();
	}

	@Override
	public List<User> getUsers() {
		return this.users;
	}
	
	@Override
	public void removeUser(User user) {
		this.users.remove(user);
		this.notifyUpdate();
	}

	@Override
	public void loadConfiguration(String filename) {
		Configuration config = new Configuration(filename);

		restaurant = config.getRestaurant();
		postcodes = config.getPostcodes();
		suppliers = config.getSupplier();
		ingredients = config.getIngredients();
		dishes = config.getDishes();
		users = config.getUsers();
		staff = config.getStaff();
		drones = config.getDrones();
		orders = config.getOrders();
		stockManager = config.getStockManager();

		startStaff();
		startDrones();
		stockManager.initStock(dishes);

		System.out.println("Loaded configuration: " + filename);
	}

	private void startStaff() {
		for (Staff s : staff) {
			new Thread(s).start();
		}
	}

	private void startDrones() {
		for (Drone drone : drones) {
			new Thread(drone).start();
		}
	}

	@Override
	public void setRecipe(Dish dish, Map<Ingredient, Number> recipe) {
		for(Entry<Ingredient, Number> recipeItem : recipe.entrySet()) {
			addIngredientToDish(dish,recipeItem.getKey(),recipeItem.getValue());
		}
		this.notifyUpdate();
	}

	@Override
	public boolean isOrderComplete(Order order) {
		return true;
	}

	@Override
	public String getOrderStatus(Order order) {
		return order.getStatus();
	}
	
	@Override
	public String getDroneStatus(Drone drone) {
		return drone.getStatus();
	}
	
	@Override
	public String getStaffStatus(Staff staff) {
		return staff.getStatus();
	}

	@Override
	public void setRestockLevels(Dish dish, Number restockThreshold, Number restockAmount) {
		dish.setRestockThreshold(restockThreshold);
		dish.setRestockAmount(restockAmount);
		this.notifyUpdate();
	}

	@Override
	public void setRestockLevels(Ingredient ingredient, Number restockThreshold, Number restockAmount) {
		ingredient.setRestockThreshold(restockThreshold);
		ingredient.setRestockAmount(restockAmount);
		this.notifyUpdate();
	}

	@Override
	public Number getRestockThreshold(Dish dish) {
		return dish.getRestockThreshold();
	}

	@Override
	public Number getRestockAmount(Dish dish) {
		return dish.getRestockAmount();
	}

	@Override
	public Number getRestockThreshold(Ingredient ingredient) {
		return ingredient.getRestockThreshold();
	}

	@Override
	public Number getRestockAmount(Ingredient ingredient) {
		return ingredient.getRestockAmount();
	}

	@Override
	public void addUpdateListener(UpdateListener listener) {
		this.listeners.add(listener);
	}
	
	@Override
	public void notifyUpdate() {
		this.listeners.forEach(listener -> listener.updated(new UpdateEvent()));
	}

	@Override
	public Postcode getDroneSource(Drone drone) {
		return drone.getSource();
	}

	@Override
	public Postcode getDroneDestination(Drone drone) {
		return drone.getDestination();
	}

	@Override
	public Number getDroneProgress(Drone drone) {
		return drone.getProgress();
	}

	@Override
	public String getRestaurantName() {
		return restaurant.getName();
	}

	@Override
	public Postcode getRestaurantPostcode() {
		return restaurant.getLocation();
	}
	
	@Override
	public Restaurant getRestaurant() {
		return restaurant;
	}


}
