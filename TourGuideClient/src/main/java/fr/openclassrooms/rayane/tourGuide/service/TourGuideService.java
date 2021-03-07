package fr.openclassrooms.rayane.tourGuide.service;

import fr.openclassrooms.rayane.tourGuide.Entity.NearByAttraction;
import fr.openclassrooms.rayane.tourGuide.tracker.Tracker;
import fr.openclassrooms.rayane.tourGuide.user.User;
import fr.openclassrooms.rayane.tourGuide.user.UserReward;
import fr.openclassrooms.thirdparty.TripPricer;
import fr.openclassrooms.thirdparty.RewardCentral;
import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import rewardCentral.RewardCentral;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utilise : - TripPricer ( définit au hasard un objet Provider (qui représente une agence de
 * voyage) ainsi qu'un prix pour celui-ci )
 *
 * <p>Constructeur: - (param) gpsUtil ( Gère la localisation des personnes et attractions ) , -
 * (param) RewardService et - (initilisé dans cons) Tracker
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

  public TourGuideService(
      GpsUtil gpsUtil, RewardsService rewardsService, RewardCentral rewardCentral) {
    this.gpsUtil = gpsUtil;
    this.rewardsService = rewardsService;
    this.rewardCentral = rewardCentral;

    if (testMode) {
      logger.info("TestMode enabled");
      logger.debug("Initializing users");
      initializeInternalUsers();
      logger.debug("Finished initializing users");
    }
    tracker = new Tracker(this);
    addShutDownHook(); // add tracker to ShutDownHook
  }

  public HashMap<String, UserReward> getUserRewards(User user) {
    return user.getUserRewards();
  }

  /**
   * get User location if exist, otherwise call trackUserLocation() to create one
   *
   * @param user
   * @return User Visited location object
   */
  @Async
  public VisitedLocation getUserLocation(User user)
      throws ExecutionException, InterruptedException {
    VisitedLocation visitedLocation =
        (user.getVisitedLocations().size() > 0)
            ? user.getLastVisitedLocation()
            : trackUserLocation(user);
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
    return new ArrayList<>(internalUserMap.values());
  }

  public HashMap<UUID, Location> getAllUsersLocation() {
    List<User> allUsers = getAllUsers();
    HashMap<UUID, Location> currentUsersLocation = new HashMap<>();
    allUsers.stream()
        .map(
            user ->
                currentUsersLocation.put(user.getUserId(), user.getLastVisitedLocation().location));
    return currentUsersLocation;
  }

  /**
   * Add to internalUserMap a new user using it's username attribute as the key
   *
   * @param user
   */
  public void addUser(User user) {
    if (!internalUserMap.containsKey(user.getUserName())) {
      internalUserMap.put(user.getUserName(), user);
    }
  }

  public List<Provider> getTripDeals(User user) {
    int cumulatativeRewardPoints =
        user.getUserRewards().values().stream().mapToInt(i -> i.getRewardPoints()).sum();
    List<Provider> providers =
        tripPricer.getPrice(
            tripPricerApiKey,
            user.getUserId(),
            user.getUserPreferences().getNumberOfAdults(),
            user.getUserPreferences().getNumberOfChildren(),
            user.getUserPreferences().getTripDuration(),
            cumulatativeRewardPoints);
    user.setTripDeals(providers);
    return providers;
  }

  /**
   * Create a random location to the user and define the reward for visiting that location (?)
   *
   * @param user
   * @return the location visited
   */
  @Async
  public VisitedLocation trackUserLocation(User user)
      throws ExecutionException, InterruptedException {
    VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
    user.addToVisitedLocations(visitedLocation);
    CompletableFuture.runAsync(
        () -> {
          try {
            rewardsService.calculateRewards(user);
          } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
          }
        });
    return visitedLocation;
  }

  // juste retourner les 5
  public List<NearByAttraction> getNearByAttractions(VisitedLocation visitedLocation) {
    List<NearByAttraction> nearbyAttractions = new ArrayList<NearByAttraction>();
    for (Attraction attraction : gpsUtil.getAttractions()) {
      double distanceToLocation = rewardsService.getDistance(visitedLocation.location, attraction);
      int reward =
          rewardCentral.getAttractionRewardPoints(attraction.attractionId, visitedLocation.userId);
      nearbyAttractions.add(
          new NearByAttraction(attraction, visitedLocation.location, distanceToLocation, reward));
    }
    Collections.sort(nearbyAttractions);
    return nearbyAttractions.stream()
        .limit(5)
        .collect(Collectors.toList()); // retourne les 5 premiers éléments
  }

  /**
   * Method to stop the tracker when the application is about to stop by calling the Tracker method
   * stopTracking()
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
  // Database connection will be used for external users, but for testing purposes internal users
  // are provided and stored in memory
  private final Map<String, User> internalUserMap = new HashMap<>();

  private void initializeInternalUsers() {
    IntStream.range(0, InternalTestHelper.getInternalUserNumber())
        .forEach(
            i -> {
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
    IntStream.range(0, 3)
        .forEach(
            i -> {
              user.addToVisitedLocations(
                  new VisitedLocation(
                      user.getUserId(),
                      new Location(generateRandomLatitude(), generateRandomLongitude()),
                      getRandomTime()));
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
