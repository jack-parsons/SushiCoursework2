package comp1206.sushi.client;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.util.*;

import comp1206.sushi.common.*;
import comp1206.sushi.common.Comms;
import comp1206.sushi.server.ClientConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Client implements ClientInterface {

    private static final Logger logger = LogManager.getLogger("Client");
	private ArrayList<UpdateListener> listeners = new ArrayList<UpdateListener>();

    private Restaurant restaurant;
    private List<Postcode> postcodes = new ArrayList<>();
    private List<Dish> dishes = new ArrayList<>();

    private User user;

    private ClientComms clientComms;
    private boolean finishedInit = false;
    private boolean loggingIn = false;
	
	public Client() {
        logger.info("Starting up client...");

        postcodes.add(new Postcode(""));

        connectToServer();


        new Thread(() -> {
            while (true) {
                try {
                	if (clientComms != null && finishedInit && !loggingIn) {
						String message = clientComms.receiveMessage();
						if (message != null)
							processMessage(message, user);
					}
					Thread.sleep(10);
				} catch (SocketException e) {
                    // Start trying to reconnect to server
                    clientComms.disconnect();
                    connectToServer();
                } catch (InterruptedException | IOException e) {
					e.printStackTrace();
				}
			}
        }).start();
	}

	private void processMessage(String message, User user) {
		System.out.println(message);
		Comms.MessageType type = Comms.extractMessageType(message);
		if (type != null) {
			switch (type) {
				case CLEAR_POSTCODES:
					postcodes.clear();
					break;
				case ADD_POSTCODE:
					String name = Comms.extractMessageAttribute(message, Comms.MessageAttribute.POSTCODE);
					// Check that postcode is not already added
					Iterator<Postcode> postcodeIterator = postcodes.iterator();
					while (postcodeIterator.hasNext()) {
						Postcode curPostcode = postcodeIterator.next();
						if (curPostcode.getName().equals(name)) {
							break;
						} else if (curPostcode.getName().equals("")) {
							// Remove placeholder postcode
							postcodeIterator.remove();
						}
					}
					postcodes.add(new Postcode(name));
					break;
				case CLEAR_DISHES:
					dishes.clear();
					break;
				case ADD_DISH:
					dishes.add(new Dish(
							Comms.extractMessageAttribute(message, Comms.MessageAttribute.NAME),
							Comms.extractMessageAttribute(message, Comms.MessageAttribute.DESCRIPTION),
							Float.parseFloat(Objects.requireNonNull(Comms.extractMessageAttribute(message, Comms.MessageAttribute.PRICE))),
							0, 0));
					break;
				case ADD_RESTAURANT:
					for (Postcode postcode : postcodes) {
						if (postcode.getName().equals(Comms.extractMessageAttribute(message, Comms.MessageAttribute.POSTCODE))) {
							restaurant = new Restaurant(
									Comms.extractMessageAttribute(message, Comms.MessageAttribute.NAME), postcode);
						}
					}
					break;
				case CLEAR_ORDERS:
					if (user != null)
						user.clearOrders();
					break;
				case ADD_ORDER:
					if (user != null) {
						String dishesRaw = Comms.extractMessageAttribute(message, Comms.MessageAttribute.DISHES);
						Order order;
						if (dishesRaw != null) {
							Map<String, Dish> dishMap = new HashMap<>();
							for (Dish dish : dishes)
								dishMap.put(dish.getName(), dish);
							order = Configuration.retrieveOrder(user, dishesRaw, dishMap);
						} else
							order = new Order(user);

						order.setName(Comms.extractMessageAttribute(message, Comms.MessageAttribute.NAME));
						user.getOrders().add(order);
					}
					break;
				case FINISH_INIT:
					finishedInit = true;
					break;
				case NEW_USER:
					String postcode = Comms.extractMessageAttribute(message, Comms.MessageAttribute.POSTCODE);
					String address = Comms.extractMessageAttribute(message, Comms.MessageAttribute.ADDRESS);
					String username = Comms.extractMessageAttribute(message, Comms.MessageAttribute.USERNAME);
					String password = Comms.extractMessageAttribute(message, Comms.MessageAttribute.PASSWORD);
					this.user = new User(username, password, address, new Postcode(postcode));
					loggingIn = false;
					break;
				case LOGIN_REJECTED:
					this.user = null;
					loggingIn = false;
					break;
				default:
					System.err.println("Login rejected in wrong place");
			}
		} else {
			throw new IllegalArgumentException("Type of message not found: " + message);
		}
	}

	public void connectToServer() {
        try {
            clientComms = new ClientComms();
            while (!finishedInit) {
				if (!loggingIn) {
					String message = clientComms.receiveMessageWait();
					processMessage(message, user);
				}

			}
        } catch (ConnectException e) {
            // Keep trying to connect
            new Thread(() -> {
                while (true) {
                    try {
                        clientComms = new ClientComms();
                        finishedInit = false;
						while (!finishedInit && !loggingIn) {
							String message = clientComms.receiveMessageWait();
							processMessage(message, user);
						}
                        System.out.println("Connection to server successful");
                        break;
                    } catch (ConnectException ignored) {

                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }).start();
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
		if (postcode != null)
			clientComms.sendMessage(String.format("REGISTER|USERNAME=%s|PASSWORD=%s|ADDRESS=%s|POSTCODE=%s", username, password, address, postcode.getName()));
		return user = new User(username, password, address, postcode);
	}

	@Override
	public User login(String username, String password) {
		loggingIn = true;
		clientComms.sendMessage(String.format("LOGIN|USERNAME=%s|PASSWORD=%s", username, password));
//		System.out.println("1");
//			String reply = clientComms.receiveMessageWait();
		while (loggingIn) {
		}
		loggingIn = false;
		System.out.println("Logging in");
		return user;
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
		clientComms.sendMessage(String.format("BASKET_UPDATE|DISHES=%s", new Order(user.getBasket(), user)));
	}

	@Override
	public Order checkoutBasket(User user) {
		Order basket = new Order(user.getBasket(), user);
		clientComms.sendMessage(String.format("ADD_ORDER|NAME=%s|DISHES=%s", basket.getName(), basket));
		user.getOrders().add(basket);
		return basket;
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
		clientComms.sendMessage(String.format("CANCEL_ORDER|NAME=%s", order.getName()));
		user.getOrders().remove(order);
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
