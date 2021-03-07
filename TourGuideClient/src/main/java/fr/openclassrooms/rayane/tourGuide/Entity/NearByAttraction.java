package fr.openclassrooms.rayane.tourGuide.Entity;

import gpsUtil.location.Attraction;
import gpsUtil.location.Location;

public class NearByAttraction extends Attraction implements Comparable<NearByAttraction> {
    public final Location userLocation;
    public final double distanceToAttraction; // in miles
    public final int rewardPoints;

    public NearByAttraction(Attraction attraction, Location userLocation, double distanceToAttraction, int rewardPoints) {
        super(attraction.attractionName, attraction.city, attraction.state, attraction.latitude, attraction.longitude);
        this.userLocation = userLocation;
        this.distanceToAttraction = distanceToAttraction;
        this.rewardPoints = rewardPoints;
    }

    @Override
    public int compareTo(NearByAttraction nearByAttraction) {
        return Double.compare(distanceToAttraction, nearByAttraction.distanceToAttraction);
    }
}
