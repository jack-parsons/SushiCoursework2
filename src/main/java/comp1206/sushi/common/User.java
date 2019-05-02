package comp1206.sushi.common;

import comp1206.sushi.common.Postcode;
import comp1206.sushi.common.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User extends Model {
	
	private String name;
	private String password;
	private String address;
	private Postcode postcode;
	private Map<Dish, Number> basket = new HashMap<>();
	private List<Order> orders = new ArrayList<>();

	public User(String username, String password, String address, Postcode postcode) {
		this.name = username;
		this.password = password;
		this.address = address;
		this.postcode = postcode;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Number getDistance() {
		return postcode.getDistance();
	}

	public Postcode getPostcode() {
		return this.postcode;
	}
	
	public void setPostcode(Postcode postcode) {
		this.postcode = postcode;
	}

	public Map<Dish, Number> getBasket() {
		return basket;
	}

	public void addDishToBasket(Dish dish, Number quantity) {
		if (basket.containsKey(dish)) {
			basket.put(dish, basket.get(dish).intValue() + quantity.intValue());
		} else {
			basket.put(dish, quantity);
		}
	}

	public Number getBasketCost() {
		return new Order(basket).getOrderCost();  // Create temp order and calculate price with it
	}

	public void updateBasket(Map<Dish, Number> basket) {
		this.basket = basket;
	}

	public void updateDishInBasket(Dish dish, Number quantity) {
		basket.put(dish, quantity);
	}

	public void clearBasket() {
		basket.clear();
	}

	public List<Order> getOrders() {
		return orders;
	}

	public void clearOrders() {
		orders.clear();
	}

	public boolean checkPassword(String passString) {
		return passString.equals(password);
	}
}
