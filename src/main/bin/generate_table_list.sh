#!/bin/sh

cat ddl/ddl.sql | grep 'create table' | awk '{print $3}' > tablelist.txt
