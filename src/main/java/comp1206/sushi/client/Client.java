package comp1206.sushi.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import comp1206.sushi.common.*;
import comp1206.sushi.common.Comms;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Client implements ClientInterface {

    private static final Logger logger = LogManager.getLogger("Client");
	private ArrayList<UpdateListener> listeners = new ArrayList<UpdateListener>();

    private Restaurant restaurant = new Restaurant("Test", new Postcode("SO17 1AW")); // TODO get actual value
    private List<Postcode> postcodes = new ArrayList<>();
    private List<Dish> dishes = new ArrayList<>();

    private ClientComms clientComms;
	
	public Client() {
        logger.info("Starting up client...");

		try {
			clientComms = new ClientComms();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public Restaurant getRestaurant() {
		return restaurant;
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
	public User register(String username, String password, String address, Postcode postcode) {
		clientComms.sendMessage(String.format("REGISTER|USERNAME=%s|PASSWORD=%s|ADDRESS=%s|POSTCODE=%s", username, password, address, postcode.getName()));
		return new User(username, password, address, postcode);
	}

	@Override
	public User login(String username, String password) {
        clientComms.sendMessage(String.format("LOGIN|USERNAME=%s|PASSWORD=%s", username, password));
		try {
			String reply = clientComms.receiveMessageWait();
			String postcode = Comms.extractMessageAttribute(reply, Comms.MessageAttribute.POSTCODE);
			String address = Comms.extractMessageAttribute(reply, Comms.MessageAttribute.ADDRESS);
			System.out.println(username+ password +address+postcode);
			return new User(username, password, address, new Postcode(postcode));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public List<Postcode> getPostcodes() {
		return postcodes;
	}

	@Override
	public List<Dish> getDishes() {
		return dishes;
	}

	@Override
	public String getDishDescription(Dish dish) {
		return dish.getDescription();
	}

	@Override
	public Number getDishPrice(Dish dish) {
		return dish.getPrice();
	}

	@Override
	public Map<Dish, Number> getBasket(User user) {
		return user.getBasket();
	}

	@Override
	public Number getBasketCost(User user) {
		return user.getBasketCost();
	}

	@Override
	public void addDishToBasket(User user, Dish dish, Number quantity) {
		user.addDishToBasket(dish, quantity);
	}

	@Override
	public void updateDishInBasket(User user, Dish dish, Number quantity) {
		user.updateDishInBasket(dish, quantity);
	}

	@Override
	public Order checkoutBasket(User user) {
		// TODO finish
		return new Order(user.getBasket());
	}

	@Override
	public void clearBasket(User user) {
		user.clearBasket();
	}

	@Override
	public List<Order> getOrders(User user) {
		return user.getOrders();
	}

	@Override
	public boolean isOrderComplete(Order order) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getOrderStatus(Order order) {
		return order.getStatus();
	}

	@Override
	public Number getOrderCost(Order order) {
		return order.getOrderCost();
	}

	@Override
	public void cancelOrder(Order order) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addUpdateListener(UpdateListener listener) {
		listeners.add(listener);
	}

	@Override
	public void notifyUpdate() {
		this.listeners.forEach(listener -> listener.updated(new UpdateEvent()));
	}

}
