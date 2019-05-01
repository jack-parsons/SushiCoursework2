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
	
	public Order() {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/YYYY HH:mm:ss");  
		LocalDateTime now = LocalDateTime.now();  
		this.name = dtf.format(now);
	}

	public Order(Map<Dish, Number> dishes) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/YYYY HH:mm:ss");
		LocalDateTime now = LocalDateTime.now();
		this.name = dtf.format(now);
		this.dishes = dishes;
	}

	public void addDish(Dish dish, Number quantity) {
		dishes.put(dish, quantity);
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
		notifyUpdate("status",this.status,status);
		this.status = status;
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
		StringBuilder sb = new StringBuilder();
		for (Dish dish : dishes.keySet()) {
			sb.append(dishes.get(dish)).append(" * ").append(dish.getName()).append(", ");
		}
		sb.delete(sb.length()-2, sb.length());
		return sb.toString();
	}

}
