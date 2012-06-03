s/identity/default nextval('id_seq')/
s/foreign key.*//
s/primary key (\(.*\)),/primary key (\1)/
s/float([0-9, ]*)/float8/g
s/uniqueidentifier(..)/text/
s/datetime/timestamp/
