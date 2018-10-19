#!/bin/bash
nohup java -Dspring.profiles.active=prod -jar ./job-center-container-1.0-SNAPSHOT.jar > /dev/null 2>&1 &
