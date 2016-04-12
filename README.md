#VOCreator
##说明
利用Java Velocity模块技术生成OC的model通讯类，用于服务器为java语言时，通过java entity生成封装与服务器交互的OC数据对象。
##配置详情
通过config.ini配置对应的数据
```
VO.Prefix=PNR //VO前缀
VO.Suffix=VO //VO后缀
VO.CreateBasic=false //是否创建基础父类，第一次生成时需设置为true,后续生成时可配置为false
VO.ScanPackage=com.diligrp.mobsite.getway.domain // 需要扫描的类包
VO.Output=creator //生成的oc model文件存放的目录
```
##生成对应类
在将对应实体和依赖jar拿到后（以下locationinfo-domain-1.0-SNAPSHOT.jar为包含model的jar），通过以下命令运行
```
java -classpath locationinfo-domain-1.0-SNAPSHOT.jar:VOCreator-1.0-SNAPSHOT-jar-with-dependencies.jar com.forway.tools.Creator config.ini
```
