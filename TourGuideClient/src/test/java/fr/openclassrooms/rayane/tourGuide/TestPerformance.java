package fr.openclassrooms.rayane.tourGuide;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Before;
import org.junit.Test;
import rewardCentral.RewardCentral;
import tourGuide.helper.InternalTestHelper;
import tourGuide.service.RewardsService;
import tourGuide.service.TourGuideService;
import tourGuide.user.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public class TestPerformance {
  @Before
  public void setUp() {
    Locale.setDefault(new Locale("en", "US", "WIN"));
  }

  /*
   * A note on performance improvements:
   *
   *     The number of users generated for the high volume tests can be easily adjusted via this method:
   *
   *     		InternalTestHelper.setInternalUserNumber(100000);
   *
   *
   *     These tests can be modified to suit new solutions, just as long as the performance metrics
   *     at the end of the tests remains consistent.
   *
   *     These are performance metrics that we are trying to hit:
   *
   *     highVolumeTrackLocation: 100,000 users within 15 minutes:
   *     		assertTrue(TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
   *
   *     highVolumeGetRewards: 100,000 users within 20 minutes:
   *          assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
   */

  @Test
  public void highVolumeTrackLocation() throws ExecutionException, InterruptedException {
    GpsUtil gpsUtil = new GpsUtil();
    RewardCentral rewardCentral = new RewardCentral();
    RewardsService rewardsService = new RewardsService(gpsUtil, rewardCentral);
    // Users should be incremented up to 100,000, and test finishes within 15 minutes
    InternalTestHelper.setInternalUserNumber(100);
    TourGuideService tourGuideService =
        new TourGuideService(gpsUtil, rewardsService, rewardCentral);

    List<User> allUsers = new ArrayList<>();
    List<CompletableFuture> completableFutureList = new ArrayList<>();
    allUsers = tourGuideService.getAllUsers();

    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    for (User user : allUsers) {
      CompletableFuture completableFuture =
          CompletableFuture.runAsync(
              () -> {
                try {
                  tourGuideService.trackUserLocation(user);
                } catch (ExecutionException | InterruptedException e) {
                  e.printStackTrace();
                }
              },
              Executors.newFixedThreadPool(20));
      completableFutureList.add(completableFuture);
    }
    CompletableFuture.allOf(
            completableFutureList.toArray(new CompletableFuture[completableFutureList.size()]))
        .join();
    stopWatch.stop();
    tourGuideService.tracker.stopTracking();

    System.out.println(
        "highVolumeTrackLocation: Time Elapsed: "
            + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime())
            + " seconds.");
    assertTrue(
        TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
  }

  // deux options de solutions de l'erreur possible
  @Test
  public void highVolumeGetRewards() {
    GpsUtil gpsUtil = new GpsUtil();
    RewardCentral rewardCentral = new RewardCentral();
    RewardsService rewardsService = new RewardsService(gpsUtil, rewardCentral);

    // Users should be incremented up to 100,000, and test finishes within 20 minutes
    InternalTestHelper.setInternalUserNumber(100);
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    TourGuideService tourGuideService =
        new TourGuideService(gpsUtil, rewardsService, rewardCentral);

    Attraction attraction = gpsUtil.getAttractions().get(0);
    List<User> allUsers = new ArrayList<>();
    List<CompletableFuture> completableFutureList = new ArrayList<>();
    allUsers = tourGuideService.getAllUsers();
    allUsers.forEach(
        u -> u.addToVisitedLocations(new VisitedLocation(u.getUserId(), attraction, new Date())));

    allUsers.forEach(
        u -> {
          CompletableFuture completableFuture =
              CompletableFuture.runAsync(
                  () -> {
                    u.addToVisitedLocations(
                        new VisitedLocation(u.getUserId(), attraction, new Date()));
                    try {
                      rewardsService.calculateRewards(u);
                    } catch (ExecutionException | InterruptedException e) {
                      e.printStackTrace();
                    }
                  },
                  Executors.newFixedThreadPool(20));
          completableFutureList.add(completableFuture);
        });

    CompletableFuture.allOf(
            completableFutureList.toArray(new CompletableFuture[completableFutureList.size()]))
        .join();

    for (User user : allUsers) {
      assertTrue(user.getUserRewards().size() > 0);
    }
    stopWatch.stop();
    tourGuideService.tracker.stopTracking();

    System.out.println(
        "highVolumeGetRewards: Time Elapsed: "
            + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime())
            + " seconds.");
    assertTrue(
        TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
  }
}
