package it.sereno.gtfs.controller;

import it.sereno.gtfs.service.WaitTimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = {"https://smart-mobility.homelinuxserver.org", "http://localhost:4200"})
@RequestMapping("/api/wait-times")
@RequiredArgsConstructor
public class StopTimesController {

    private final WaitTimeService service;

    @GetMapping("/{stopIdentifier}")
    public List<WaitTimeService.WaitTime> getStopTimes(@PathVariable final String stopIdentifier) {
        return service.getStopWaitTimes(stopIdentifier);
    }
}