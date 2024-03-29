package comp1206.sushi.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import comp1206.sushi.common.Order;

public class Order extends Model {

	private String status;
	private Map<Dish, Number> dishes = new HashMap<>();
	private User user;
	private boolean isComplete = false, isBeingDelivered = false;

//	public Order() {
//		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/YYYY HH:mm:ss");
//		LocalDateTime now = LocalDateTime.now();
//		this.name = dtf.format(now);
//	}
	
	public Order(User user) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/YYYY HH:mm:ss");  
		LocalDateTime now = LocalDateTime.now();  
		this.name = dtf.format(now);
		this.user = user;
		setStatus("Being prepared");
	}

	public Order(Map<Dish, Number> dishes, User user) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/YYYY HH:mm:ss");
		LocalDateTime now = LocalDateTime.now();
		this.name = dtf.format(now);
		this.dishes = dishes;
		this.user = user;
		setStatus("Being prepared");
	}

	public void startDelivery() {
		setStatus("Being delivered");
		isBeingDelivered = true;
	}

	public void deliverOrder() {
		setStatus("Delivered");
		isBeingDelivered = false;
		isComplete = true;
	}

	public void stopDelivery() {
		setStatus("Being prepared");
		isBeingDelivered = false;
		isComplete = false;
	}

	public boolean isBeingDelivered() {
		return isBeingDelivered;
	}

	public boolean isComplete() {
		return isComplete;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public User getUser() {
		return user;
	}

	public void addDish(Dish dish, Number quantity) {
		dishes.put(dish, quantity);
	}

	public Map<Dish, Number> getDishQuantities() {
		return dishes;
	}

	public Number getDistance() {
		return 1;
	}

	@Override
	public String getName() {
		return this.name;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
		notifyUpdate("status",this.status,status);
	}

	public Number getOrderCost() {
		double total = 0;
		for (Dish dish : dishes.keySet()) {
			total += dish.getPrice().doubleValue() * dishes.get(dish).doubleValue();
		}
		return total;
	}

	@Override
	public String toString() {
		return Order.dishQuantitiesToString(dishes);
	}

	public static String dishQuantitiesToString(Map<Dish, Number> dishes) {
		if (dishes.size() > 0) {
			StringBuilder sb = new StringBuilder();
			for (Dish dish : dishes.keySet()) {
				if (dishes.get(dish) != null)
					sb.append(dishes.get(dish)).append(" * ").append(dish.getName()).append(", ");
			}
			sb.delete(sb.length() - 2, sb.length());
			return sb.toString();
		} else {
			return "";
		}
	}

}
