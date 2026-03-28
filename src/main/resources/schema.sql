CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE IF NOT EXISTS GTFS_HASH (
    hash TEXT
);

CREATE TABLE IF NOT EXISTS GTFS_DAY (
    timetable_day DATE
);

CREATE TABLE IF NOT EXISTS GTFS_AGENCY (
    agency_id TEXT PRIMARY KEY,
    agency_name TEXT NOT NULL,
    agency_url TEXT NOT NULL,
    agency_timezone TEXT NOT NULL,
    agency_lang TEXT,
    agency_phone TEXT,
    agency_fare_url TEXT
);

CREATE TABLE IF NOT EXISTS GTFS_STOPS (
    stop_id TEXT PRIMARY KEY,
    stop_code TEXT,
    stop_name TEXT NOT NULL,
    stop_desc TEXT,
    stop_lat DOUBLE PRECISION NOT NULL,
    stop_lon DOUBLE PRECISION NOT NULL,
    zone_id TEXT,
    stop_url TEXT,
    location_type INTEGER,
    parent_station TEXT,
    stop_timezone TEXT,
    wheelchair_boarding INTEGER
);

CREATE TABLE IF NOT EXISTS GTFS_ROUTES (
    route_id TEXT PRIMARY KEY,
    agency_id TEXT,
    route_short_name TEXT,
    route_long_name TEXT,
    route_desc TEXT,
    route_type INTEGER NOT NULL,
    route_url TEXT,
    route_color TEXT,
    route_text_color TEXT
);

CREATE TABLE IF NOT EXISTS GTFS_TRIPS (
    route_id TEXT NOT NULL,
    service_id TEXT NOT NULL,
    trip_id TEXT PRIMARY KEY,
    trip_headsign TEXT,
    trip_short_name TEXT,
    direction_id INTEGER,
    block_id TEXT,
    shape_id TEXT,
    wheelchair_accessible INTEGER,
    exceptional TEXT
);

CREATE TABLE IF NOT EXISTS GTFS_STOP_TIMES (
    trip_id TEXT NOT NULL,
    arrival_time TEXT,
    departure_time TEXT,
    stop_id TEXT NOT NULL,
    stop_sequence INTEGER NOT NULL,
    stop_headsign TEXT,
    pickup_type INTEGER,
    drop_off_type INTEGER,
    shape_dist_traveled DOUBLE PRECISION,
    timepoint INTEGER,
    PRIMARY KEY (trip_id, stop_sequence)
);

CREATE TABLE IF NOT EXISTS GTFS_CALENDAR (
    service_id TEXT PRIMARY KEY,
    monday INTEGER NOT NULL,
    tuesday INTEGER NOT NULL,
    wednesday INTEGER NOT NULL,
    thursday INTEGER NOT NULL,
    friday INTEGER NOT NULL,
    saturday INTEGER NOT NULL,
    sunday INTEGER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL
);

CREATE TABLE IF NOT EXISTS GTFS_CALENDAR_DATES (
    service_id TEXT NOT NULL,
    date DATE NOT NULL,
    exception_type INTEGER NOT NULL,
    PRIMARY KEY (service_id, date)
);

CREATE TABLE IF NOT EXISTS GTFS_FARE_ATTRIBUTES (
    fare_id TEXT PRIMARY KEY,
    price NUMERIC(10,2) NOT NULL,
    currency_type TEXT NOT NULL,
    payment_method INTEGER NOT NULL,
    transfers INTEGER,
    agency_id TEXT,
    transfer_duration INTEGER
);

CREATE TABLE IF NOT EXISTS GTFS_FARE_RULES (
    fare_id TEXT NOT NULL,
    route_id TEXT,
    origin_id TEXT,
    destination_id TEXT,
    contains_id TEXT
);

CREATE TABLE IF NOT EXISTS GTFS_SHAPES (
    shape_id TEXT NOT NULL,
    shape_pt_lat DOUBLE PRECISION NOT NULL,
    shape_pt_lon DOUBLE PRECISION NOT NULL,
    shape_pt_sequence INTEGER NOT NULL,
    shape_dist_traveled DOUBLE PRECISION,
    PRIMARY KEY (shape_id, shape_pt_sequence)
);

CREATE TABLE IF NOT EXISTS GTFS_FREQUENCIES (
    trip_id TEXT NOT NULL,
    start_time TEXT NOT NULL,
    end_time TEXT NOT NULL,
    headway_secs INTEGER NOT NULL,
    exact_times INTEGER
);

CREATE TABLE IF NOT EXISTS GTFS_TRANSFERS (
    from_stop_id TEXT NOT NULL,
    to_stop_id TEXT NOT NULL,
    transfer_type INTEGER NOT NULL,
    min_transfer_time INTEGER,
    PRIMARY KEY (from_stop_id, to_stop_id)
);

CREATE TABLE IF NOT EXISTS GTFS_FEED_INFO (
    feed_publisher_name TEXT NOT NULL,
    feed_publisher_url TEXT NOT NULL,
    feed_lang TEXT NOT NULL,
    feed_start_date DATE,
    feed_end_date DATE,
    feed_version TEXT
);

DROP INDEX IF EXISTS idx_trips_route_id;
CREATE INDEX idx_trips_route_id ON GTFS_TRIPS(route_id);
DROP INDEX IF EXISTS idx_trips_service_id;
CREATE INDEX idx_trips_service_id ON GTFS_TRIPS(service_id);
DROP INDEX IF EXISTS idx_stop_times_trip_id;
CREATE INDEX idx_stop_times_trip_id ON GTFS_STOP_TIMES(trip_id);
DROP INDEX IF EXISTS idx_stop_times_stop_id;
CREATE INDEX idx_stop_times_stop_id ON GTFS_STOP_TIMES(stop_id);
DROP INDEX IF EXISTS idx_shapes_shape_id;
CREATE INDEX idx_shapes_shape_id ON GTFS_SHAPES(shape_id);