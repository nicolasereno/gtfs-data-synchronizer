package it.sereno.gtfs.controller;

import it.sereno.gtfs.service.WaitTimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class StopTimesController {

	private final WaitTimeService service;

	@GetMapping( "/api/wait-times/{stopIdentifier}" )
	public List<WaitTimeService.WaitTime> getStopTimes( @PathVariable final String stopIdentifier ) {
		return service.getStopWaitTimes( stopIdentifier );
	}
}