package comp1206.sushi.common;

public class Staff extends Model implements Runnable {
	private static final int UPDATE_DELAY = 1000;
	public static StockManager stockManager;

	private String name;
	private String status;
	private Number fatigue;
	private boolean running;
	
	public Staff(String name) {
		this.setName(name);
		this.setFatigue(0);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Number getFatigue() {
		return fatigue;
	}

	public void setFatigue(Number fatigue) {
		this.fatigue = fatigue;
	}

	public String getStatus() {
		return status;
	}

	public synchronized void setStatus(String status) {
		notifyUpdate("status",this.status,status);
		this.status = status;
	}

	@Override
	public void run() {
		while (running) {
			try {
				Dish restockDish = stockManager.findDishToPrepare();
				if (restockDish != null) {
					// Wait for a random time between 20-60 seconds before finishing the dish
					setStatus(String.format("Preparing %d * %s", restockDish.getRestockAmount().intValue(), restockDish.getName()));
					Thread.sleep(Math.round(Math.random() * 40000 + 20000));
					stockManager.dishFinished(restockDish);
					setStatus("Idle");  // Return to idle state
				}
				Thread.sleep(UPDATE_DELAY);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
