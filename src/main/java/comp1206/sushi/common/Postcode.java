package comp1206.sushi.common;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import comp1206.sushi.common.Postcode;

public class Postcode extends Model {

	private static final double EARTHRADIUS = 6371;
	private String name;
	private Map<String,Double> latLong;
	private Number distance;

	public Postcode(String code) {
		this.name = code;
		calculateLatLong();
		this.distance = Integer.valueOf(0);
	}
	
	public Postcode(String code, Restaurant restaurant) {
		this.name = code;
		calculateLatLong();
		calculateDistance(restaurant);
	}
	
	@Override
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public Number getDistance() {
		return this.distance;
	}

	public Map<String,Double> getLatLong() {
		return this.latLong;
	}
	
	protected void calculateDistance(Restaurant restaurant) {
		//This function needs implementing
		Postcode destination = restaurant.getLocation();
		this.distance = Integer.valueOf(0);
	}
	
	public double calculateDistance(Postcode destination) {
		double distance = 0;
		if (this.isValidPostcode() && destination.isValidPostcode()) {
				double diffLat = Math.toRadians(getLatLong().get("lat") - destination.getLatLong().get("lat"));
				double diffLong = Math.toRadians(getLatLong().get("lon") - destination.getLatLong().get("lon"));
				double lat1 = Math.toRadians(this.getLatLong().get("lat"));
				double lat2 = Math.toRadians(destination.getLatLong().get("lat"));
				double a = Math.sin(diffLat / 2) * Math.sin(diffLat / 2) + Math.cos(lat1) * Math.cos(lat2) * Math.sin(diffLong / 2) * Math.sin(diffLong / 2);
				double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
				distance = EARTHRADIUS * c;
			}
		return distance;
	}


	public boolean isValidPostcode() {
		return getLatLong() != null &&
						getLatLong().get("lat") != null && getLatLong().get("lon") != null;
	}

	protected void calculateLatLong() {

		this.latLong = new HashMap<String,Double>();
		try {
				URL postCodeAPI = new URL(String.format("https://www.southampton.ac.uk/~ob1a12/postcode/postcode.php?postcode=%s", name.replace(" ", "")));
				Scanner latLongScanner = new Scanner(postCodeAPI.openStream(), "UTF-8").useDelimiter(",");
				while (latLongScanner.hasNext()) {
						String[] parts = latLongScanner.next().split(":");
						if (parts.length == 2) {
								if (parts[0].equals("\"lat\"")) {
										latLong.put("lat", Double.parseDouble(parts[1].replace("\"", "")));
									}
								if (parts[0].equals("\"long\"")) {
										latLong.put("lon", Double.parseDouble(parts[1].replace("\"", "").replace("}", "")));
									}
							}
					}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
}
