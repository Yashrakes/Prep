package com.Uber;

import com.Uber.Enums.RatingStatus;
import com.Uber.Models.Driver;
import com.Uber.Models.Location;
import com.Uber.Models.Rider;
import com.Uber.Models.Trip;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
public class UberApplication {

	public static void main(String[] args) {

		//SpringApplication.run(UberApplication.class, args);
		Rider amit = new Rider("Amit Kumar", RatingStatus.FIVE);
		Rider lalit = new Rider("Lalit Kumar", RatingStatus.TWO);

		RiderManager riderManager = RiderManager.getInstance();
		riderManager.addRider("Amit Kumar",amit);
		riderManager.addRider("Lalit Kumar",lalit);

		Driver yogitaDriver = new Driver("Yogita", RatingStatus.FIVE);
		Driver riddhiDriver = new Driver("Riddhi", RatingStatus.ONE);
		DriverManager driverManager = DriverManager.getInstance();
		driverManager.addDriver("yogita", yogitaDriver);
		driverManager.addDriver("riddhi", riddhiDriver);

		TripManager tripManager = TripManager.getInstance();
		tripManager.createTrip(amit, new Location(10, 10), new Location(30, 30));

		tripManager.createTrip(lalit, new Location(10, 10), new Location(30, 30));

		System.out.println("printinig trips riders and drivers");

		for (Map.Entry<Integer, Trip> me : tripManager.getTripsMap().entrySet()) {

			// Printing keys
			System.out.print(me.getKey() + "->");
			System.out.println("rider : " + me.getValue().getRider().getName() + "->");
			System.out.println("driver :" + me.getValue().getDriver().getName() );
		}


	}

}
