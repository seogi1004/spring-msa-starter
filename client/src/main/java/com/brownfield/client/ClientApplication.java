package com.brownfield.client;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
class AppConfiguration {
    @LoadBalanced
    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

@SpringBootApplication
@EnableDiscoveryClient
public class ClientApplication implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(ClientApplication.class);

    @Autowired
    RestTemplate searchClient;
    @Autowired
    RestTemplate bookingClient;
    @Autowired
    RestTemplate checkInClient;

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

    @Override
    public void run(String... strings) throws Exception {
        //Search for a flight
        SearchQuery searchQuery = new SearchQuery("NYC","SFO","22-JAN-18");
// 		Flight[] flights = searchClient.postForObject("http://search-service/search/get", searchQuery, Flight[].class);



        Flight[] flights = searchClient.postForObject("http://search-service/search/get", searchQuery, Flight[].class);

        Arrays.asList(flights).forEach(flight -> logger.info(" flight >"+ flight));

        //create a booking only if there are flights.
        if(flights == null || flights.length == 0){
            return;
        }
        Flight flight = flights[0];
        BookingRecord booking = new BookingRecord(flight.getFlightNumber(),flight.getOrigin(),
                flight.getDestination(), flight.getFlightDate(),null,
                flight.getFares().getFare());
        Set<Passenger> passengers = new HashSet<Passenger>();
        passengers.add(new Passenger("Gavin","Franc","Male", booking));
        booking.setPassengers(passengers);
        long bookingId =0;
        try {
            bookingId = bookingClient.postForObject("http://booking-service/booking/create", booking, long.class);
            logger.info("Booking created "+ bookingId);
        }catch (Exception e){
            logger.error("BOOKING SERVICE NOT AVAILABLE...!!!");
        }

        //check in passenger
        if(bookingId == 0) return;
        try {
            CheckInRecord checkIn = new CheckInRecord("Franc", "Gavin", "28C", null, "BF101","22-JAN-18", bookingId);
            long checkinId = checkInClient.postForObject("http://checkin-service/checkin/create", checkIn, long.class);
            logger.info("Checked IN "+ checkinId);
        }catch (Exception e){
            logger.error("CHECK IN SERVICE NOT AVAILABLE...!!!");
        }
    }

}