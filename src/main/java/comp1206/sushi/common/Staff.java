package comp1206.sushi.common;

//TODO deal with persistence for in progress dishes

public class Staff extends Model implements Runnable {
	private static final int UPDATE_DELAY = 100;
	private static final double FATIGUE_RATE = 0.001;
	private static final double REST_RATE = 0.005;
	private StockManager stockManager;

	private String name;
	private String status;
	private Number fatigue;

	private boolean running = true;
	private boolean producing = false;
	private boolean resting = false;

	private long startTime = 0;
	private long dishTime = 0;
	private long restTime = 0;
	
	public Staff(String name, StockManager stockManager) {
		this.setName(name);
		this.setFatigue(0);
		this.stockManager = stockManager;
		setStatus("Idle");
	}

	@Override
	public String getName() {
		return name;
	}

	public void stop() {
		running = false;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Number getFatigue() {
		if (fatigue == null)
			return null;
		return Math.max(0, fatigue.floatValue());
	}

	public void setFatigue(Number fatigue) {
		this.fatigue = fatigue;
	}

	public void increaseFatigue(long millisPassed) {
		setFatigue(fatigue.floatValue() + FATIGUE_RATE * millisPassed);
	}

	public void restFatigue(long millisPassed) {
		setFatigue(fatigue.floatValue() - REST_RATE * millisPassed);
	}

	public String getStatus() {
		if (producing && !resting) {
			return status + "(" + ((dishTime + restTime) - (System.currentTimeMillis() - startTime))/1000 + ")";
		}
		return status;
	}

	public synchronized void setStatus(String status) {
//		notifyUpdate("status",this.status,status);
		this.status = status;
	}

	@Override
	public void run() {
		long lastT = System.currentTimeMillis();
		Dish restockDish = null;
		String tempStatus = "";

		while (running) {
			try {
				if (!resting && !producing) {
					if ((restockDish = stockManager.findDishToPrepare()) != null) {
						// Wait for a random time between 20-60 seconds before finishing the dish
						setStatus(String.format("Preparing %d * %s", restockDish.getRestockAmount().intValue(), restockDish.getName()));
						startTime = System.currentTimeMillis();
						dishTime = Math.round(Math.random() * 40000 + 20000);
						producing = true;
					}
				} else if (resting && !producing) {
					restFatigue(System.currentTimeMillis() - lastT);
				} else {
					// TODO fix negative times after resting
					if (!resting && System.currentTimeMillis() - startTime >= dishTime + restTime) {
						// Finished dish
						stockManager.dishFinished(restockDish);
						setStatus("Idle");  // Return to idle state
						restTime = 0;
						producing = false;
					} else {
						if (!resting) {
							// Dish in progress still
							if (getFatigue().floatValue() >= 100) {
								// Staff needs rest
								tempStatus = status;
								setStatus("Resting");
								resting = true;
								restTime = 0;
							}
							increaseFatigue(System.currentTimeMillis() - lastT);
						} else {
							if (getFatigue().floatValue() <= 0) {
								// Finished resting
								restTime += System.currentTimeMillis() - lastT;
								resting = false;
								setStatus(tempStatus);
							}
							restFatigue(System.currentTimeMillis() - lastT);
						}
					}
				}
				lastT = System.currentTimeMillis();
				Thread.sleep(UPDATE_DELAY);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
