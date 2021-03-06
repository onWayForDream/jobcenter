#   基本概念

###   project

一个project代表一个功能的集合，从代码层面来讲，一个project包含一组jar包


###   job

一个job代表一个功能，或一件流程，或一个运行模板，或一件事情。从技术上来讲，每个job都包含
-   一个实现了PJob接口的java类（PJob接口定义在job-center-api包中）
-   运行时参数
-   重复方式（不重复/按固定时间间隔重复/cron表达式）
-   开始运行时间
-   失效时间（到时间后自动停止）

###   job-schedule-log

每次启动或停止一个job，就会生成一个job-schedule-log，它记录了事件发生的时间，操作用户，事件类型（用户启动/系统启动/用户停止等），重复方式以及运行时参数等


###   job-run-log

job启动以后会重复运行，每运行一次就会生成一个job-run-log，它包括了运行开始时间，结束时间，是否成功，运行结果（或错误消息）等


#   已实现的特性

jobcenter是一个高可用的、低入侵性的的调度框架(schedule framework)。具有以下特点

-   分布式运行环境，理论上可以调度海量的job。
-   高可用，当一台jobcenter服务器宕机时，它上面寄宿的job会被立刻均匀地分配到其余的服务器上继续运行。
-   不同账号之间相互隔离，同一账号下的不同project之间相互隔离，互不影响。
-   完美兼容spring-boot项目，将项目迁移过来的同时，可以继续享受spring-boot给开发带来的便利
-   可视化运行结果，job的每次执行都有详细记录，包括：运行时间、运行结果、运行日志。
-   便利的运行监控，可以对每次运行进行监控，如果job运行失败或者运行结果异常，可在第一时间收到通知。（待开发）
-   通用组件支持，基于对已有业务的理解，jobcenter已经内置了一些开箱即用的基础组件，如：读取opentsdb的组件，发送rabbitmq消息的组件，调用HTTP接口的组件，读写mysql数据库的组件等。（待开发）

关于产品每次新增的功能，可以查看 [RELEASE-NOTE.MD](RELEASE-NOTE.md)

关于目前正在开发的功能，可以查看 [ROAD-MAP.md](ROAD-MAP.md)
