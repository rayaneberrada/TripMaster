package fr.openclassrooms.rayane.gps.controller;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/gps")
public class gps {

    private GpsUtil gpsUtil = new GpsUtil();

    @GetMapping(value = "/getGps")
    public GpsUtil getGpsUtil() {
        return gpsUtil;
    }

    @GetMapping(value = "/getLocation/{userId}")
    public VisitedLocation getUserLocation(@PathVariable UUID userId) {
        return gpsUtil.getUserLocation(userId);
    }

    @GetMapping(value = "/getAttractions")
    public List<Attraction> attractions() {
        return gpsUtil.getAttractions();
    }
}
