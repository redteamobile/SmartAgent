# Smart Agent Java版本

[ENGLISH](https://github.com/redteamobile/SmartAgent/blob/smart-agent-java-dev/README-EG.md)

* 本仓库主要管理红茶端侧安卓方案，开放部分源码。用户可以基于这部分源码，做定制化开发，以满足自己的业务场景

## 系统架构

* 整个系统端到端的精简架构如下图所示，红茶物联网平台与SMDP+是服务端组件，Agent与Monitor是端侧的组件。
* 在使用实体eSIM只需要集成Agent即可，如需要软卡(iSIM)需要集成Agent与Monitor。
![image](https://github.com/redteamobile/SmartAgent/blob/master-smart-agent/doc/img/system.jpg)

## 功能特色

* 全功能 Smart Agent
* 全功能 实体eSIM与iSIM
* Open Interface 管理Agent，管理卡片
* Open Interface 选择卡片类型(实体eSIM或iSIM(软卡))
* 内置共享种子号，BootStrap提供初始连接
* Demo 测试界面快速上手调试

## 模块说明

* LIB Agent的核心代码，主要使用C语言实现
* SRC 是Agent的开源的源码，包括系统接口调用及Agent对外的接口
* assets是共享种子号，通过BootStrap提供初始连接

## 运行环境

* 集成开发环境Android Studio，与PC的操作系统无关

## 说明

* Open Interface 在JniInterface的类中有定义及详细的说明

Change Log:
  1.v1.0.1-- 新增 Insert ,Stop 接口；
