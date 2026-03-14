package it.sereno.gtfs;

import it.sereno.gtfs.service.GTFSStaticDataImportService;
import it.sereno.gtfs.service.GTFSStaticDataIngestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class GTFSDataSynchronizerApplication implements CommandLineRunner {

    public GTFSDataSynchronizerApplication(final GTFSStaticDataImportService importService, final GTFSStaticDataIngestService ingestService) {
        this.importService = importService;
        this.ingestService = ingestService;
    }

    public static void main(String[] args) {
        SpringApplication.run(GTFSDataSynchronizerApplication.class, args);
    }

    private final GTFSStaticDataImportService importService;

    private final GTFSStaticDataIngestService ingestService;

    @Override
    public void run(final String... args) throws Exception {
        // Sync data at start
        log.info("Checking static data hash");
        final String newHash = ingestService.dataImportNeeded();
        if (newHash != null) {
            log.info("Ingesting static data");
            ingestService.importStaticData(newHash);
            log.info("Importing stops data");
            importService.importStopData();
            log.info("Importing route data");
            importService.importRouteData();
        }
        log.info("Importing timetable data");
        importService.importStopTimetableData();
        log.info("Initial synchronization complete: system is up and running!");
    }
}
