package comp1206.sushi.common;

import comp1206.sushi.common.Drone;

import java.util.HashMap;
import java.util.Map;

public class Drone extends Model implements Runnable {

	private Number speed;
	private Number progress = null;
	
	private Number capacity;
	private Number battery;
	
	private String status;
	
	private Postcode source;
	private Postcode destination;

	private Map<Model, Number> cargo = new HashMap<>();
	private boolean running = true;
	private StockManager stockManager;
	private Restaurant restaurant;

	private long lastT;

//	public Drone(Number speed, StockManager stockManager) {
//		this.setSpeed(speed);
//		this.setCapacity(1);
//		this.setBattery(100);
//		this.stockManager = stockManager;
//		setStatus("Idle");
//	}

	public Drone(Number speed, StockManager stockManager, Restaurant restaurant) {
		this.setSpeed(speed);
		this.setCapacity(1);
		this.setBattery(100);
		this.stockManager = stockManager;
		setStatus("Idle");
		this.restaurant = restaurant;
		setSource(restaurant.getLocation());
	}

	public Number getSpeed() {
		return speed;
	}

	
	public Number getProgress() {
		return progress;
	}
	
	public void setProgress(Number progress) {
		this.progress = progress;
	}
	
	public void setSpeed(Number speed) {
		this.speed = speed;
	}
	
	@Override
	public String getName() {
		return "Drone (" + getSpeed() + " speed)";
	}

	public Postcode getSource() {
		return source;
	}

	public void setSource(Postcode source) {
		this.source = source;
	}

	public Postcode getDestination() {
		return destination;
	}

	public void setDestination(Postcode destination) {
		this.destination = destination;
	}

	public Number getCapacity() {
		return capacity;
	}

	public void setCapacity(Number capacity) {
		this.capacity = capacity;
	}

	public Number getBattery() {
		return battery;
	}

	public void setBattery(Number battery) {
		this.battery = battery;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		notifyUpdate("status",this.status,status);
		this.status = status;
	}

	public void setRestaurant(Restaurant restaurant) {
		this.restaurant = restaurant;
	}

	@Override
	public void run() {
		lastT = System.currentTimeMillis();
		while (running) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (progress == null) {
				// If the drone is at restaurant idle
				Ingredient ingredientToRestock = stockManager.findIngredientToRestock();
				if (ingredientToRestock != null) {
					// If there is an ingredient to restock
					setDestination(ingredientToRestock.getSupplier().getPostcode());
					addCargo(ingredientToRestock, Math.min(ingredientToRestock.getRestockAmount().floatValue(), capacityRemaining().floatValue()));
					progress = 0;
					setStatus("Flying to supplier for ingredient: " + ingredientToRestock.getName());
				} else {
					Order orderToDeliver = stockManager.findOrderToDeliver();
					if (orderToDeliver != null) {
						setDestination(orderToDeliver.getUser().getPostcode());
						addCargo(orderToDeliver, Math.min(ingredientToRestock.getRestockAmount().floatValue(), capacityRemaining().floatValue()));
						progress = 0;
						setStatus(String.format("Flying to user: %s for order: %s", orderToDeliver.getUser().getName(), orderToDeliver.getName()));
					}
				}
			} else {
				if (progress.floatValue() < 100) {
					// If the drone has not yet reached its destination and is in transit
					progress = progress.floatValue() + (((System.currentTimeMillis() - lastT) / 1000.0 * getSpeed().floatValue()) /
							(getSource().calculateDistance(getDestination()) * 1000.0)) * 100;
				} else {
					// If the drone has arrived at its destination
					if (destination.equals(restaurant.getLocation())) {
						// If the drone's destination was the restaurant go idle
						setSource(destination);
						setStatus("Idle");
						progress = null;
					} else {
						// If the drone's destination was not the restaurant, then return to restaurant
						setSource(destination);
						setDestination(restaurant.getLocation());
						progress = 0;
						setStatus("Returning to restaurant");
					}
				}
			}
			lastT = System.currentTimeMillis();
		}
	}

	public Number cargoCarrying() {
		float t = 0;
		for (Number q : cargo.values()) {
			t += q.floatValue();
		}
		return t;
	}

	public Number capacityRemaining() {
		return getCapacity().floatValue() - cargoCarrying().floatValue();
	}

	/**
	 * Add cargo to the drone's storage
	 * @param item The item to be added
	 * @param quantity The quantity to add
	 * @return Whether the quantity fitted
	 */
	public boolean addCargo(Model item, Number quantity) {
		if (quantity.floatValue() + cargoCarrying().floatValue() < getCapacity().floatValue()) {
			cargo.put(item, quantity.floatValue() + cargo.getOrDefault(item, 0).floatValue());
			return true;
		} else {
			return false;
		}
	}
}
