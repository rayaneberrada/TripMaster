package tourGuide.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tourGuide.Entity.NearByAttraction;
import tourGuide.helper.InternalTestHelper;
import tourGuide.tracker.Tracker;
import tourGuide.user.User;
import tourGuide.user.UserReward;
import tripPricer.Provider;
import tripPricer.TripPricer;


/**
 *  Utilise :
 *     - TripPricer ( définit au hasard un objet Provider (qui représente une agence de voyage) ainsi qu'un prix pour celui-ci )
 *
 *     Constructeur:
 *        - (param) gpsUtil ( Gère la localisation des personnes et attractions ) ,
 *        - (param) RewardService et
 *        - (initilisé dans cons) Tracker

 */
@Service
public class TourGuideService {
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final RewardCentral rewardCentral;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	boolean testMode = true;
	
	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;
		this.rewardCentral = rewardCentral;

		if(testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook(); // add tracker to ShutDownHook
	}
	
	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}

	/**
	 * get User location if exist, otherwise call trackUserLocation() to create one
	 *
	 * @param user
	 * @return User Visited location object
	 */
	public VisitedLocation getUserLocation(User user) {
		VisitedLocation visitedLocation = (user.getVisitedLocations().size() > 0) ?
			user.getLastVisitedLocation() :
			trackUserLocation(user);
		return visitedLocation;
	}

	/**
	 * Retrieve a use in the internalUserMap by it's name
	 *
	 * @param userName
	 * @return the User matching the param userName
	 */
	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}
	
	public List<User> getAllUsers() {
		return internalUserMap.values().stream().collect(Collectors.toList());
	}

	/**
	 * Add to internalUserMap a new user using it's username attribute as the key
	 *
	 * @param user
	 */
	public void addUser(User user) {
		if(!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}


	public List<Provider> getTripDeals(User user) {
		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(), user.getUserPreferences().getNumberOfAdults(), 
				user.getUserPreferences().getNumberOfChildren(), user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}

	/**
	 * Create a random location to the user and define the reward for visiting that location (?)
	 *
	 * @param user
	 * @return the location visited
	 */
	public VisitedLocation trackUserLocation(User user) {
		VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
		user.addToVisitedLocations(visitedLocation);
		rewardsService.calculateRewards(user);
		return visitedLocation;
	}

	//  Instead: Get the closest five tourist attractions to the user - no matter how far away they are.
	//  Return a new JSON object that contains:
	// Name of Tourist attraction,
	// Tourist attractions lat/long,
	// The user's location lat/long,
	// The distance in miles between the user's location and each of the attractions.
	// The reward points for visiting each Attraction.
	//    Note: Attraction reward points can be gathered from RewardsCentral
	public List<NearByAttraction> getNearByAttractions(VisitedLocation visitedLocation) {
		List<NearByAttraction> nearbyAttractions = new ArrayList<>();
		for(Attraction attraction : gpsUtil.getAttractions()) {
				double distanceToLocation = rewardsService.getDistance(attraction, visitedLocation.location);
				int reward = rewardCentral.getAttractionRewardPoints(attraction.attractionId, visitedLocation.userId);
				nearbyAttractions.add(new NearByAttraction(attraction, visitedLocation.location, distanceToLocation, reward));
		}
		Collections.sort(nearbyAttractions);
		return nearbyAttractions;
	}

	/**
	 * Method to stop the tracker when the application is about to stop by calling the Tracker method stopTracking()
	 */
	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> tracker.stopTracking()));
	}
	
	/**********************************************************************************
	 * 
	 * Methods Below: For Internal Testing
	 * 
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();
	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);
			
			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}
	
	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i-> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(), new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}
	
	private double generateRandomLongitude() {
		double leftLimit = -180;
	    double rightLimit = 180;
	    return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}
	
	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
	    double rightLimit = 85.05112878;
	    return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}
	
	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
	    return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}
	
}
