create table driver
(
    driver_uid     uuid primary key,
    fullname       varchar(255),
    email          varchar(255),
    phone          varchar(255),
    license_number varchar(255)
);


create table telemetry_event
(
    telemetry_event_uid uuid primary key,
    driver_uid          uuid,
    coordinates         geography,
    recorded_at         timestamptz
);

create table route
(
    route_uid       uuid primary key,
    origin_uid      uuid,
    destination_uid uuid,
    waypoints       jsonb
);

create table parcel
(
    parcel_uid    uuid primary key,
    route_uid     uuid,
    driver_uid    uuid,
    tracking_code varchar(255),
    sender_uid    uuid,
    receiver_uid  uuid,
    status        varchar(255),
    pickup_time   timestamptz,
    dropoff_time  timestamptz
);

create table address
(
    address_uid uuid primary key,
    full_name   varchar(255),
    coordinates geography,
    street      varchar(255),
    city        varchar(255),
    postal_code varchar(255),
    country     varchar(255),
    details     varchar(255)
);

create table status_history
(
    status_history_uid uuid primary key,
    parcel_uid         uuid,
    status             varchar(255),
    old_status         varchar(255),
    ts_from            timestamptz,
    ts_to              timestamptz,
    reason             varchar(255)
);

alter table parcel
    add constraint FK_parcel_route foreign key (route_uid) references route (route_uid);
alter table parcel
    add constraint FK_parcel_driver foreign key (driver_uid) references driver (driver_uid);
alter table parcel
    add constraint FK_parcel_sender foreign key (sender_uid) references address (address_uid);
alter table parcel
    add constraint FK_parcel_receiver foreign key (receiver_uid) references address (address_uid);
alter table route
    add constraint FK_route_origin foreign key (origin_uid) references address (address_uid);
alter table route
    add constraint FK_route_destination foreign key (destination_uid) references address (address_uid);
alter table telemetry_event
    add constraint FK_telemetry_event_driver foreign key (driver_uid) references driver (driver_uid);
alter table status_history
    add constraint FK_status_history_parcel foreign key (parcel_uid) references parcel (parcel_uid);
