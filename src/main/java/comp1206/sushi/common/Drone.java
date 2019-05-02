package comp1206.sushi.common;

import comp1206.sushi.common.Drone;

import java.util.HashMap;
import java.util.Map;

public class Drone extends Model implements Runnable {

	private Number speed;
	private Number progress;
	
	private Number capacity;
	private Number battery;
	
	private String status;
	
	private Postcode source;
	private Postcode destination;

	private Map<Model, Number> cargo = new HashMap<>();
	private boolean running = true;
	private StockManager stockManager;

	private long lastT;

	public Drone(Number speed, StockManager stockManager) {
		this.setSpeed(speed);
		this.setCapacity(1);
		this.setBattery(100);
		this.stockManager = stockManager;
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

	@Override
	public void run() {
		lastT = System.currentTimeMillis();
		while (running) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (status.contains("Idle") || status.equals("")) {
				Ingredient ingredientToRestock = stockManager.findIngredientToRestock();
				if (ingredientToRestock != null) {
					// If there is an ingredient to restock
					setDestination(ingredientToRestock.getSupplier().getPostcode());
					addCargo(ingredientToRestock, Math.min(ingredientToRestock.getRestockAmount().floatValue(), capacityRemaining().floatValue()));
					progress = 0;
				} else {
					Order orderToDeliver = stockManager.findOrderToDeliver();
					if (orderToDeliver != null) {
						setDestination(orderToDeliver.get().getPostcode());
						addCargo(ingredientToRestock, Math.min(ingredientToRestock.getRestockAmount().floatValue(), capacityRemaining().floatValue()));
						progress = 0;
					}
				}
			} else {
				if (progress.floatValue() < 100) {
					progress = progress.floatValue() + ((System.currentTimeMillis() - lastT) / 1000 * getSpeed().floatValue()) /
							(getSource().calculateDistance(getDestination()) / 1000);
				} else {
					setSource(destination);
					setStatus("Idle");
					progress = null;
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
