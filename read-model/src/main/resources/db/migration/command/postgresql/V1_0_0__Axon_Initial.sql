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
        metaData oid,
        payload oid not null,
        primary key (globalIndex),
        unique (sequenceNumber, aggregateIdentifier)
);

create table SnapshotEventEntry (
        sequenceNumber bigint not null,
        aggregateIdentifier varchar(255) not null,
        eventIdentifier varchar(255) not null unique,
        payloadRevision varchar(255),
        payloadType varchar(255) not null,
        timeStamp varchar(255) not null,
        type varchar(255) not null,
        metaData oid,
        payload oid not null,
        primary key (sequenceNumber, aggregateIdentifier, type)
);

create table TokenEntry (
        segment integer not null,
        owner varchar(255),
        processorName varchar(255) not null,
        timestamp varchar(255) not null,
        tokenType varchar(255),
        token oid,
        primary key (segment, processorName)
);