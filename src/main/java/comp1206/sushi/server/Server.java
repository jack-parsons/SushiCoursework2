package comp1206.sushi.server;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.util.*;
import java.util.Map.Entry;

import comp1206.sushi.common.*;
import comp1206.sushi.common.Comms;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
 
public class Server implements ServerInterface {

    private static final Logger logger = LogManager.getLogger("Server");
    private static final String PERSISTENCE_FILENAME = "ServerAutoSave.txt";
	
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

		if (new File(PERSISTENCE_FILENAME).exists())
			loadConfiguration(PERSISTENCE_FILENAME);
		else
			loadDefaultConfig();

		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			public void run()
			{
				System.out.println("Saving data on shutdown");
				saveState(PERSISTENCE_FILENAME);
			}
		});


		commsController = new ServerCommsController();

		addUpdateListener(updateEvent -> {
			for (ClientConnection clientConnection : commsController.getClientConnections()) {
				updateClient(clientConnection, false);
			}
			stockManager.setOrders(orders);
		});

		new Thread(commsController).start();

		new Thread(() -> {
			while(true) {
				processClientComms();
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();

		new Thread(() -> {
			while(true) {
				for (ClientConnection clientConnection : commsController.getClientConnections()) {
					if (!clientConnection.checkUpdated()) {
						updateClient(clientConnection, true);
						clientConnection.sendMessage("FINISH_INIT");
					}
				}
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();

		saveState(PERSISTENCE_FILENAME);
		addUpdateListener((e) -> saveState(PERSISTENCE_FILENAME));
		addUpdateListener((e) -> {
			if (e.property != null && e.property.equals("status")) {
				for (ClientConnection clientConnection : commsController.getClientConnections()) {
					if (clientConnection.getUser() != null) {
						clientConnection.sendMessage("FINISH_INIT");
					}
				}
			}
		});

	}

	private void processClientComms() {
		for (ClientConnection clientConnection : commsController.getClientConnections()) {
			try {
				if (!clientConnection.checkUpdated()) {
					updateClient(clientConnection, true);
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
							notifyUpdate();
						case LOGIN:
							boolean successfulLogin = false;
							for (User user : users) {
								if (user.getName().equals(Comms.extractMessageAttribute(reply, Comms.MessageAttribute.USERNAME))) {
									if (user.checkPassword(Comms.extractMessageAttribute(reply, Comms.MessageAttribute.PASSWORD))) {
										clientConnection.setUser(user);
										clientConnection.sendMessage(String.format("NEW_USER|USERNAME=%s|PASSWORD=%s|ADDRESS=%s|POSTCODE=%s", user.getName(), user.getPassword(), user.getAddress(), user.getPostcode()));
										successfulLogin = true;
										updateClient(clientConnection, true);
									}
								}
							}
							if (!successfulLogin) {
								clientConnection.sendMessage("LOGIN_REJECTED");
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
								order.addUpdateListener(e->{
									for (ClientConnection clientConnection2 : commsController.getClientConnections()) {
										// Update all clients logged in as this user
										if (clientConnection2.getUser() == clientConnection.getUser())
											updateClient(clientConnection2, false);
									}
								});

								order.setName(Comms.extractMessageAttribute(reply, Comms.MessageAttribute.NAME));
								clientConnection.getUser().getOrders().add(order);
								addOrder(order);
								for (ClientConnection clientConnection2 : commsController.getClientConnections()) {
									updateClient(clientConnection2, false);
								}
								notifyUpdate();
							}
							break;
						case CANCEL_ORDER:
							String orderName = Comms.extractMessageAttribute(reply, Comms.MessageAttribute.NAME);
							orders.removeIf(order -> order.getName().equals(orderName));
							for (ClientConnection clientConnection2 : commsController.getClientConnections()) {
								// Update all clients logged in as this user
								if (clientConnection2.getUser() == clientConnection.getUser())
									updateClient(clientConnection2, false);
							}
							notifyUpdate();
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
							for (ClientConnection clientConnection2 : commsController.getClientConnections()) {
								// Update all clients logged in as this user
								if (clientConnection2.getUser() == clientConnection.getUser())
									updateClient(clientConnection2, true);
							}
							notifyUpdate();
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void addOrder(Order order) {
		orders.add(order);
		stockManager.setOrders(orders);
	}

	private void updateClient(ClientConnection clientConnection, boolean includeDishes) {
	    clientConnection.sendMessage("CLEAR_POSTCODES");
	    for (Postcode postcode : postcodes) {
            clientConnection.sendMessage("ADD_POSTCODE|POSTCODE="+postcode);
        }

		clientConnection.sendMessage(String.format("ADD_RESTAURANT|NAME=%s|POSTCODE=%s", restaurant.getName(), restaurant.getLocation()));

	    if (includeDishes) {
			clientConnection.sendMessage("CLEAR_DISHES");
			for (Dish dish : dishes) {
				clientConnection.sendMessage(String.format("ADD_DISH|NAME=%s|DESCRIPTION=%s|PRICE=%s", dish.getName(), dish.getDescription(), dish.getPrice()));
			}
			if (clientConnection.getUser() != null)
				clientConnection.sendMessage(String.format("BASKET_UPDATE|DISHES=%s", Order.dishQuantitiesToString(clientConnection.getUser().getBasket())));
		}
//
		if (clientConnection.getUser() != null) {
			clientConnection.sendMessage("CLEAR_ORDERS");
			for (Order order : orders) {
				if (order.getUser().getName().equals(clientConnection.getUser().getName())) {
					clientConnection.sendMessage(String.format("ADD_ORDER|STATUS=%s|NAME=%s|DISHES=%s", order.getStatus(), order.getName(), order));
				}
			}
		}
    }
	
	@Override
	public List<Dish> getDishes() {
		return this.dishes;
	}
	@Override
	public Dish addDish(String name, String description, Number price, Number restockThreshold, Number restockAmount) {
		Dish newDish = new Dish(name,description,price,restockThreshold,restockAmount);
		this.dishes.add(newDish);
//		this.notifyUpdate();
		if (commsController != null)
			for (ClientConnection clientConnection: commsController.getClientConnections()) {
				updateClient(clientConnection, true);
				clientConnection.sendMessage("FINISH_INIT");
			}
		stockManager.setDishStock(newDish, 0);
		return newDish;
	}
	
	@Override
	public void removeDish(Dish dish) throws UnableToDeleteException{
		for (Order order : orders) {
			if (order.getDishQuantities().keySet().contains(dish)) {
				throw new UnableToDeleteException("Dish is used in order: " + order.getName());
			}
		}
		for (Drone drone : drones) {
			for (Model item : drone.getCargo().keySet()) {
				if (item.equals(dish)) {
					throw new UnableToDeleteException("Dish is being transported by drone: " + drone.getName());
				}
			}
		}
		this.dishes.remove(dish);
		stockManager.removeDish(dish);
		if (commsController != null)
			for (ClientConnection clientConnection: commsController.getClientConnections()) {
				updateClient(clientConnection, true);
				clientConnection.sendMessage("FINISH_INIT");
			}
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
	public void removeIngredient(Ingredient ingredient) throws UnableToDeleteException{
		for (Dish dish : dishes) {
			if (dish.getRecipe().keySet().contains(ingredient)) {
				throw new UnableToDeleteException("Ingredient is used in dish: " + dish.getName());
			}
		}
		for (Drone drone : drones) {
			for (Model item : drone.getCargo().keySet()) {
				if (item.equals(ingredient)) {
					throw new UnableToDeleteException("Ingredient is being transported by drone: " + drone.getName());
				}
			}
		}
		stockManager.removeIngredient(ingredient);
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
	public void removeSupplier(Supplier supplier) throws UnableToDeleteException{
		for (Ingredient ingredient : ingredients) {
			if (ingredient.getSupplier().equals(supplier)) {
				throw new UnableToDeleteException("Supplier is used by ingredient: " + ingredient.getName());
			}
		}
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
		new Thread(mock).start();
		return mock;
	}

	@Override
	public void removeDrone(Drone drone) throws UnableToDeleteException {
		for (Model item : drone.getCargo().keySet()) {
			if (item instanceof Order) {
				((Order) item).stopDelivery();
			}
		}
		drone.stop();
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
		new Thread(mock).start();
		return mock;
	}

	@Override
	public void removeStaff(Staff staff) {
		staff.stop();
		this.staff.remove(staff);
		this.notifyUpdate();
	}

	@Override
	public List<Order> getOrders() {
		return this.orders;
	}

	@Override
	public void removeOrder(Order order) throws UnableToDeleteException{

		for (Drone drone : drones) {
			for (Model item : drone.getCargo().keySet()) {
				if (item.equals(order)) {
					throw new UnableToDeleteException("Order is being transported by drone: " + drone.getName());
				}
			}
		}

		this.orders.remove(order);
		this.notifyUpdate();
	}
	
	@Override
	public Number getOrderCost(Order order) {
		return order.getOrderCost();
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
		for (Postcode postcode : postcodes) {
			if (postcode.getName().equals(code)) {
				throw new IllegalArgumentException("Added postcode that already exists");
			}
		}
		Postcode mock = new Postcode(code);
		this.postcodes.add(mock);
		this.notifyUpdate();
		return mock;
	}

	@Override
	public void removePostcode(Postcode postcode) throws UnableToDeleteException {
		for (Supplier supplier : suppliers) {
			if (supplier.getPostcode().equals(postcode)) {
				throw new UnableToDeleteException("Postcode is used by supplier: " + supplier.getName());
			}
		}
		for (User user : users) {
			if (user.getPostcode().equals(postcode)) {
				throw new UnableToDeleteException("Postcode is used by user: " + user.getName());
			}
		}
		if (restaurant.getLocation().equals(postcode)) {
			throw new UnableToDeleteException("Postcode is used by restaurant");
		}
		this.postcodes.remove(postcode);
		this.notifyUpdate();
	}

	@Override
	public List<User> getUsers() {
		return this.users;
	}
	
	@Override
	public void removeUser(User user) throws UnableToDeleteException{
		for (ClientConnection clientConnection : commsController.getClientConnections()) {
			if (clientConnection.getUser().equals(user)) {
				throw new UnableToDeleteException("User is used by client");
			}
		}
		this.users.remove(user);
		this.notifyUpdate();
	}

	public void loadDefaultConfig() {
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

		Ingredient ingredient1 = addIngredient("Ingredient 1","grams",supplier1,20,5,1);
		Ingredient ingredient2 = addIngredient("Ingredient 2","grams",supplier2,20,5,1);
		Ingredient ingredient3 = addIngredient("Ingredient 3","grams",supplier3,20,5,1);

		Dish dish1 = addDish("Dish 1","Dish 1",1,1,3);
		Dish dish2 = addDish("Dish 2","Dish 2",2,1,3);
		Dish dish3 = addDish("Dish 3","Dish 3",3,1,3);

		addIngredientToDish(dish1,ingredient1,1);
		addIngredientToDish(dish1,ingredient2,2);
		addIngredientToDish(dish2,ingredient2,3);
		addIngredientToDish(dish2,ingredient3,1);
		addIngredientToDish(dish3,ingredient1,2);
		addIngredientToDish(dish3,ingredient3,1);

		addStaff("Staff 1");
		addStaff("Staff 2");
		addStaff("Staff 3");

		addDrone(20);
		addDrone(15);
		addDrone(10);
		addDrone(30);

		stockManager = new StockManager();
		stockManager.initStock(dishes);
		stockManager.addUpdateListener(e -> notifyUpdate());
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
		stockManager.initStock(dishes);
		stockManager.setOrders(orders);
		stockManager.addUpdateListener(e -> notifyUpdate());
		startStaff();
		startDrones();

		notifyUpdate();

		saveState(PERSISTENCE_FILENAME);



		System.out.println("Loaded configuration: " + filename);
	}

	private void startStaff() {
		for (Staff staffMember : staff) {
			new Thread(staffMember).start();
		}
	}

	private void startDrones() {
		for (Drone drone : drones) {
			new Thread(drone).start();
		}
	}

	public void saveState(String filename) {
		new DataPersistence(new File(filename)).writeStateToFile(restaurant, dishes, drones, ingredients, orders, staff,
				suppliers, users, postcodes, stockManager);
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
		return order.isComplete();
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
