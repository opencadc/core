create table <schema>.ModelVersion
(
    model varchar(32) not null primary key,
    version varchar(32) not null,
    lastModified timestamp not null
)
;
