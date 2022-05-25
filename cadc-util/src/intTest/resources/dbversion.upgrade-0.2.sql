
alter table <schema>.test_table
	add column baz double precision;

create index foo_bar on <schema>.test_table (foo, bar);

