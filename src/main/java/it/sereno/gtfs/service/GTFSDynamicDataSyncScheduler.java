package it.sereno.gtfs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(name = "gtfs.updates-url")
public class GTFSDynamicDataSyncScheduler {

    private final GTFSDynamicDataImportService dynamicDataImportService;

    public GTFSDynamicDataSyncScheduler(
            final GTFSDynamicDataImportService dynamicDataImportService) {
        this.dynamicDataImportService = dynamicDataImportService;
    }

    @Scheduled(cron = "30 * * * * *")
    public void syncCorrectionData() {
        log.debug("Starting GTFS dynamic data synchronization check");
        dynamicDataImportService.importGtfsUpdates();
        log.debug("GTFS dynamic data import completed successfully");
    }

    @Scheduled(cron = "0 * * * * *")
    public void syncVehiclePositions() {
        log.debug("Starting GTFS dynamic gis data synchronization check");
        dynamicDataImportService.fetchAndStoreVehiclePositions();
        log.debug("GTFS dynamic gis data import completed successfully");
    }
}
