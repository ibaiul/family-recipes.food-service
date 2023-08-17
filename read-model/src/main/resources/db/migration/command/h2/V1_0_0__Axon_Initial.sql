create sequence DomainEventEntry_SEQ start with 1 increment by 50;

create table DomainEventEntry (
        globalIndex bigint not null,
        sequenceNumber bigint not null,
        aggregateIdentifier varchar(255) not null,
        eventIdentifier varchar(255) not null unique,
        payloadRevision varchar(255),
        payloadType varchar(255) not null,
        timeStamp varchar(255) not null,
        type varchar(255),
        metaData blob,
        payload blob not null,
        constraint DomainEventEntry_pk primary key (globalIndex),
        constraint DomainEventEntry_uk1 unique (sequenceNumber, aggregateIdentifier)
);

create table SnapshotEventEntry (
        sequenceNumber bigint not null,
        aggregateIdentifier varchar(255) not null,
        eventIdentifier varchar(255) not null unique,
        payloadRevision varchar(255),
        payloadType varchar(255) not null,
        timeStamp varchar(255) not null,
        type varchar(255) not null,
        metaData blob,
        payload blob not null,
        constraint SnapshotEventEntry_pk primary key (sequenceNumber, aggregateIdentifier, type)
);

create table TokenEntry (
        segment integer not null,
        owner varchar(255),
        processorName varchar(255) not null,
        timestamp varchar(255) not null,
        tokenType varchar(255),
        token blob,
        constraint TokenEntry_pk primary key (segment, processorName)
);
