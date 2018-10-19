#!/bin/bash
str=`jps |grep job-center-container`
pid=${str%%job*}
kill -9 $pid

