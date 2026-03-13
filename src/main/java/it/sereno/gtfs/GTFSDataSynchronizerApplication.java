package it.sereno.gtfs;

import it.sereno.gtfs.service.GTFSStaticDataImportService;
import it.sereno.gtfs.service.GTFSStaticDataIngestService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GTFSDataSynchronizerApplication implements CommandLineRunner {

	public GTFSDataSynchronizerApplication( final GTFSStaticDataImportService importService, final GTFSStaticDataIngestService ingestService ) {
		this.importService = importService;
		this.ingestService = ingestService;
	}

	public static void main( String[] args ) {
		SpringApplication.run( GTFSDataSynchronizerApplication.class, args );
	}

	private final GTFSStaticDataImportService importService;

	private final GTFSStaticDataIngestService ingestService;

	@Override
	public void run( final String... args ) throws Exception {
		// Sync data at start
		final String newHash = ingestService.dataImportNeeded();
		if ( newHash != null ) {
			ingestService.importStaticData( newHash );
			importService.importStopData();
			importService.importRouteData();
		}
		importService.importStopTimetableData();
	}
}
