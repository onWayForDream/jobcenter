#   v1.0

-   对project，job等概念进行重新的梳理，重构了很多类名和表名
-   project模块已开发完成，可以通过网页对project进行CRUD
-   lib模块已开发完成，可以通过网页上传和删除jar包，上传的文件最大可达100M
-   job模块实现了创建/删除/修改/查询/启动/停止/显示运行状态/显示sub-job-list等功能
-   job可以按照MANUAL/INTERVAL/CRON——EXPRESSION三种方式调度，还支持开始时间和结束时间（FUTURE_TASK)
-   job每运行一次，都会生成一个唯一的UUID，并作为参数传递给job，job可以记录在日志中，方便问题的追溯
-   基本支持了运行linux命令（测试成功，没有在产品中体现出来）
-   日志存储到文件系统的某个位置，按每月生成一个文件夹，每天生成一个日志文件
-   重启jobcenter后，读取数据库中状态为RUNNING的job，并按照不同策略恢复他们的运行


#   v1.1

-   显示job_run_log的记录
-   根据run-id显示运行日志
-   job并发选项：允许同时运行/阻塞等待/跳过本次运行
-   运行成功的job可以返回一个String类型的运行结果，保存到job_run_log数据表中，作为对本次运行结果的概述
-   job在运行时可以缓存java对象，便于多次运行之间交换数据

#   v1.3

-   加载resource下的配置文件的工作，由jobcenter来完成，并通过jobcenter-api中的PJob接口的方法参数传递给job
-   为了便于job的开发和调试，jobcenter-api中新增了PropertiesUtil类来完成加载配置文件的工作

#   v2.0,分布式化：

-   jar包存档到hdfs上。
-   增加zookeeper作为状态服务器，所有的jobcenter进程在启动时都与zookeeper建立一个连接
-   新增一台nginx作为负载均衡服务器
-   当一个jobcenter进程接到启动job的请求时，先请求zookeeper，获取job数量最少节点，在zookeeper中创建一个节点表示一条启动指定，带上该节点的名称，该节点从zookeeper中拿到指令后，在自己的节点内启动job，然后删除该节点，表示任务已完成
-   当一个jobcenter进程接到停止job的请求时，查询mysql中该job的寄宿节点，然后向zookeeper中创建一个节点表示一条停止指令，带上节点名称，该节点从zookeeper中拿到指令后，停止自己的job，并删除该节点，表示任务已完成
-   当一个jobcenter进程挂掉后，其余的jobcenter进程试图去zookeeper中获取一个分布式锁，拿到锁的进程负责恢复挂掉的进程内的job。恢复job的过程同启动job的流程


##   基于zookeeper的分布式化方案
-   jobcenter创建ephemeral node来保持与zookeeper的心跳。znode的值是当前主机运行的任务数量
-   多个jobcenter节点同时创建一个固定命名的znode，创建成功者视为获取到了分布式锁(zookeeper默认提供的分布式锁的机制不适合这个场景)
-   jobcenter监听某个znode下的新children节点，如果节点的值与自己的名字(默认使用主机名)相同的话，则认为这个任务是发送给自己的，否则直接忽略
-   jobcenter接收到发送给自己的任务时，先删掉该节点（从根源上避免任务的争抢），然后再处理任务。
-   发送任务可以通过zookeeper提供的分布式队列的机制来实现
-   需要调用日志时，由调用方向队列中写入一个任务，任务包括[run_id],[hostname],[random_znode]，其中，第三个参数是由调用方生成的一个znode的地址，调用方会调用exist(random_znode,true)来等待特定的host将日志内容发送过来。这个过程有点类似RPC。

##   znode的设计

-   /jobcenter  ：   顶层znode，值无意义
-   /jobcenter/clients/<hostname1>...           ：   此节点下是ephemeral node，存在的节点一定是正在运行的节点。所有节点都监听/jobcenter/clients/，发现children数量变少了则表示有机器down掉了
-   /jobcenter/jobqueues/start/...              :    此节点保存需要由某台jobcenter来启动的任务。
-   /jobcenter/jobqueues/stop/...               :    此节点保存需要由某台jobcenter来停止的任务。
-   /jobcenter/restorelocks/<hostname>          :    成功创建该znode的jobcenter进程拿到了锁，由它来负责<hostname>的恢复工作


##   修改mysql表结构的脚本

```
alter table job add `running_host` varchar(50) DEFAULT NULL;
alter table job_run_log  add `running_host` varchar(50) DEFAULT NULL;
```

#   v2.1 登录和权限

大致思路如下：

-   初期阶段，帐号由超级管理员分配
-   角色包括admin和developer，admin是超级管理员，拥有最高权限，而developer可以理解为某个部分的开发者，仅对自己创建的项目拥有权限
-   暂不支持user group

修改mysql的语句

```
alter table project add owner varchar(50) ;
```


#   v2.2 spring-boot-support

jobcenter为业务而生，为业务服务，现在业务需要jobcenter对spring-boot项目提供支持。

需要改动的点：

## classLoader完全隔离

多个spring-boot项目之间必须互不影响，并且由于jobcenter本身也基于spring-boot框架，所以spring-boot项目也要与jobcenter本身互不影响，所以，jobcenter在load spring-boot的context时，传递的classLoader不能是jobcenter的ApplicationClassLoader，而是jobcenter的ApplicationClassLoader.getParent()。这样可以做到jobcenter和多个spring-boot项目之间的完全隔离。


## job-center-api接口共享


同一个Class文件（例如PJob接口)，如果由两个不同的classLoadload出来的话，其实是两个类，无法进行类型转换。这样的话，类也是可以工作的，但对类进行的所有操作（如实例化，调用方法等）都需要通过反射来完成，会造成开发成本高，代码可读性差，运行性能差。为了解决这个问题，我将job-center-api.jar放到$JAVA_HOME/jre/lib/ext目录下，由ExtensionClassLoader来加载，根据JVM的父级委派模型，classLoader在load一个类之前，会先让父级的classLoader来加载，而ExtensionClassLoader就是jobcenter和spring-boot-job的公共parent，所以，这就保证了定义在job-center-api包内的所有类和接口在整个应用程序中只由一个classLoader来load，这就可以放心的进行类型转换了，还能直接调用接口内的方法，而无需通过反射来进行。

需要注意的是，job-center-springboot-api中的类，以及所有spring-boot相关的类（如ConfigurableApplicationContext），每个classLoader单独加载，互补冲突，但不能互相转换，调用的话仍需要通过反射来进行（getBean()方法就是在SpringContextGhost内部通过反射来完成的）


## 加载springApplicationContext的classpath


spring-boot本身的机制并不是直接运行spring-boot项目内的main方法，而是通过`org.springframework.boot.loader.JarLauncher`来启动。springboot-starter-plugin生成的uber-jar的目录结构对于这个类来讲没有问题，但如果直接loadClass的话，是找不到类的。这就需要将uber-jar解压缩，然后再拼接出一个UrlClassLoader的classpath，包含以下3部分：

-   当前目录。包含`org.springframework.boot.loader.JarLauncher`类及相关依赖
-   `/BOOT-INF/classes`。包含了spring-boot项目中用户编写的类
-   `/BOOT-INF/lib`。包含类用户编写的类的所有依赖项

这么做的话，既可以load出`org.springframework.boot.loader.JarLauncher`类并调用它的`main`方法，又可以load出`SpringApplicationContextProvider`类并拿到`ConfigurableApplicationContext`。

但如果要修改jar包的话，需要删除jar包然后再次上传。再次上传时，直接解压缩并覆盖已有内容。

## 加载springApplicationContext的时机

在考虑加载springApplicationContext的时机时，我又梳理了POJO的加载模式。

已有的加载POJO的模式是，每启动一个job，就会使用一个新的URLClassLoader来加载这个class，创建实例，转换成PJob类，然后调用executeJob方法，这样的话没有太大的性能问题，但job之间是隔离的，如果某个job依赖一个单例类的话，每个job都会创建出来一个单例（每个类确实都只有一个实例，但出现了多个同名的类）

这个过程可以优化为：

启动job时，如果当前项目没有classLoader，则创建新的classLoader实例，下面是伪代码

```
function runPJob(){
    var classLoader = getClassloaderOfCurProject(projectId);
    if(classLoader == null){
        classLoader = new URLClassLoader(...);
    }
    if(!classLoaser.isLatest()){
        classLoader = new URLClassLoader(...);
    }
    var userClass = classLoader.loadClass('className');
}
```
这里面考虑了何时需要重新载入一个新的classLoader。这样的话，用户可以随时删除或新增project下的jar文件，并且，只要重新运行job，就会自动载入最新的文件。

## 重构loadPJob和loadPJobResolver的过程


loadPJob和loadPJobResolver的过程写的有些繁琐，可读性太差，代码重复度有些高，可以考虑重构一下。重构时，要通盘考虑linuxJob,javaJob,springbootJob，以及loadResolver的问题。

这个接口可以包括以下方法

-   loadPojoJob
-   loadPojoResolver
-   loadSpringBootBeanJob
-   loadSpringBootBeanResolver
-   loadLinuxJob
-   loadConfigProperties


## 遗留的bug


-   删除hdfs中的lib时，没有直接删除本地文件，这就造成了在页面上的lib列表中以及在hdfs中只有一个最新的lib，但在本地磁盘上却保存了好几个版本的lib，这会造成不确定的问题。（已解决）
-   job解析器如果更新了一个job，那么，这个job就会被直接删除，在页面上就查不到它的运行记录和运行日志了。(已解决）

##  新暴露出的bug

-   由于job与jobcenter在classloader层级是完全平级的，导致job无法将日志写到jobcenter设置的日志目录下，在jobcenter的页面上无法查看日志。（已解决）
