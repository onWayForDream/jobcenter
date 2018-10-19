#!/bin/bash

scp job-center-container/target/job-center-container-2.1.jar root@jobcenter-prod-1:/tmp
scp job-center-container/target/job-center-container-2.1.jar root@jobcenter-prod-2:/tmp
scp job-center-container/target/job-center-container-2.1.jar root@jobcenter-prod-3:/tmp