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
		// TODO replace with actual capacity if needed
		return Integer.MAX_VALUE;
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
				Ingredient ingredientToRestock = stockManager.findIngredientToRestock(capacityRemaining());
				if (ingredientToRestock != null) {
					// If there is an ingredient to restock
					setDestination(ingredientToRestock.getSupplier().getPostcode());
					float amountRestock = Math.min(ingredientToRestock.getRestockAmount().floatValue(), capacityRemaining().floatValue());
					addCargo(ingredientToRestock, amountRestock);
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
						setDestination(null);
						setStatus("Idle");
						progress = null;
					} else {
						// Transfer cargo
						for (Model item : cargo.keySet()) {
							if (item instanceof Order) {
								((Order) item).deliverOrder();
							} else if (item instanceof Ingredient) {
								stockManager.setIngredientsStock((Ingredient)item, stockManager.getIngredientsStock((Ingredient)item).floatValue() + cargo.get(item).floatValue());
								stockManager.informDeliveryCompleted((Ingredient) item, cargo.getOrDefault(item, 0));
							} else {
								throw new IllegalArgumentException("Invalid model given to drone cargo");
							}
						}
						cargo.clear();

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
	public void addCargo(Model item, Number quantity) {
		if (quantity.floatValue() + cargoCarrying().floatValue() <= getCapacity().floatValue()) {
			cargo.put(item, quantity.floatValue() + cargo.getOrDefault(item, 0).floatValue());
		} else {
			throw new IllegalArgumentException("Not enough space in drone capacity: " + quantity);
		}
	}

	public Map<Model, Number> getCargo() {
		return cargo;
	}
}
