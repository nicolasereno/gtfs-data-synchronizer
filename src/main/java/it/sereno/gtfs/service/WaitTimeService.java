package it.sereno.gtfs.service;

import it.sereno.gtfs.base.model.RouteTimetable;
import it.sereno.gtfs.base.model.StopTimeTable;
import it.sereno.gtfs.base.repository.StopTimeTableRepository;
import it.sereno.gtfs.updates.model.TripCorrection;
import it.sereno.gtfs.updates.repository.TripCorrectionsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WaitTimeService {

	private final StopTimeTableRepository stopTimeTableRepository;
	private final TripCorrectionsRepository tripCorrectionsRepository;

	public List<WaitTime> getStopWaitTimes( final String stopId ) {

		final List<WaitTime> waitTimes = new ArrayList<>();
		final String startTime = Instant.now().atZone( ZoneId.systemDefault() ).toLocalTime().toString().substring( 0, 8 );
		final String stopTime = Instant.now().atZone( ZoneId.systemDefault() ).plusMinutes( 60 ).toLocalTime().toString().substring( 0, 8 );

		final List<RouteTimetable> timeTableElements = stopTimeTableRepository.findById( stopId ).map( StopTimeTable::getTimetable )
				.orElse( List.of() );

		for ( final RouteTimetable timetable : timeTableElements ) {
			for ( final RouteTimetable.ArrivalTime at : timetable.getArrivalTimes() ) {
				final Optional<TripCorrection> tripCorrections = tripCorrectionsRepository.findCorrectionsById( at.getTripIdentifier() );
				final boolean correction = tripCorrections.isPresent();
				final String arrivalTime = correction
						? tripCorrections.get().getCorrections().get( stopId )
						: at.getArrivalTime();

				if ( (arrivalTime != null && (correction || (arrivalTime.compareTo( startTime ) >= 0 && arrivalTime.compareTo( stopTime ) <= 0))) ) {
					final int minutesToWait = minutesToWait( arrivalTime );
					if ( minutesToWait > -1 ) {
						waitTimes.add( new WaitTime(
								timetable.getRouteIdentifier(), timetable.getDirectionDescription(), arrivalTime, minutesToWait, correction ) );
					}
				}
			}
		}

		return waitTimes.stream().sorted( Comparator.comparing( WaitTime::arrivalTime ) ).toList();
	}

	private int minutesToWait( String time ) {
		LocalTime now = LocalTime.now();
		LocalTime arrivalTime = LocalTime.parse( time, DateTimeFormatter.ofPattern( "HH:mm:ss" ) );
		return (int) Duration.between( now, arrivalTime ).toMinutes();
	}

	public record WaitTime(
			String routeIdentifier, String directionDescription, String arrivalTime, int minutes, boolean corrected) {
	}
}