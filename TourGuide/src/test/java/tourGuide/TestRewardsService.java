package tourGuide;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tourGuide.helper.InternalTestHelper;
import tourGuide.service.RewardsService;
import tourGuide.service.TourGuideService;
import tourGuide.user.User;
import tourGuide.user.UserReward;

public class TestRewardsService {
  @Before
  public void setUp() {
    Locale.setDefault(new Locale("en", "US", "WIN"));
  }

  @Test
  public void userGetRewards() throws ExecutionException, InterruptedException {
    GpsUtil gpsUtil = new GpsUtil();
    RewardCentral rewardCentral = new RewardCentral();
    RewardsService rewardsService = new RewardsService(gpsUtil, rewardCentral);

    InternalTestHelper.setInternalUserNumber(0);
    TourGuideService tourGuideService =
        new TourGuideService(gpsUtil, rewardsService, rewardCentral);

    User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
    Attraction attraction = gpsUtil.getAttractions().get(0);
    user.addToVisitedLocations(new VisitedLocation(user.getUserId(), attraction, new Date()));
    tourGuideService.trackUserLocation(user);
    rewardsService.calculateRewards(user);
    HashMap<String, UserReward> userRewards = user.getUserRewards();
    tourGuideService.tracker.stopTracking();
    assertTrue(userRewards.size() == 1);
  }

  @Test
  public void isWithinAttractionProximity() {
    GpsUtil gpsUtil = new GpsUtil();
    RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
    Attraction attraction = gpsUtil.getAttractions().get(0);
    assertTrue(rewardsService.isWithinAttractionProximity(attraction, attraction));
  }

  @Test
  public void nearAllAttractions() throws ExecutionException, InterruptedException {
    GpsUtil gpsUtil = new GpsUtil();
    RewardCentral rewardCentral = new RewardCentral();
    RewardsService rewardsService = new RewardsService(gpsUtil, rewardCentral);
    rewardsService.setProximityBuffer(Integer.MAX_VALUE);

    InternalTestHelper.setInternalUserNumber(1);
    TourGuideService tourGuideService =
        new TourGuideService(gpsUtil, rewardsService, rewardCentral);

    rewardsService.setProximityBuffer(10000);
    rewardsService.calculateRewards(tourGuideService.getAllUsers().get(0));
    HashMap<String, UserReward> userRewards =
        tourGuideService.getUserRewards(tourGuideService.getAllUsers().get(0));
    tourGuideService.tracker.stopTracking();

    assertEquals(gpsUtil.getAttractions().size(), userRewards.size());
  }
}
