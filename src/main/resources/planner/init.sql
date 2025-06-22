create table if not exists artefacts
(
    coordId int not null
        constraint artefacts_pk
            primary key,
    small_arte int default 0,
    large_arte int default 0,
    unique_arte int default 0
);

create table if not exists attacks
(
    a_coordId int not null,
    t_coordId int not null,
    landing_time String not null,
    waves int not null,
    realTgt int not null,
    conq int not null,
    time_shift int not null,
    unit_speed int not null,
    server_speed int not null,
    server_size int not null
);

create table if not exists operation_meta
(
    flex_seconds int,
    defaultLandingTime String
);

create table if not exists participants
(
    id integer
        constraint participants_pk
            primary key autoincrement,
    account String,
    xCoord int not null,
    yCoord int not null,
    ts int not null,
    speed double not null,
    tribe int not null,
    offstring String not null,
    offsize int not null,
    catas int not null,
    chiefs int not null,
    sendmin String,
    sendmax String,
    comment String
);

create table if not exists templates
(
    template1 String,
    template2 String
);

create table if not exists updated
(
    last String not null
        constraint updated_pk
            primary key
);

create table if not exists village_data
(
    coordId int not null
        constraint village_data_pk
            primary key,
    capital int default 0 not null,
    offvillage int default 0 not null,
    wwvillage int default 0
);

create table if not exists x_world
(
    coordId int not null
        constraint x_world_pk
            primary key,
    xCoord int not null,
    yCoord int not null,
    tribe int not null,
    villageId int not null,
    villageName String not null,
    playerId int not null,
    playerName String not null,
    allyId int not null,
    allyName String not null,
    population int not null
);
