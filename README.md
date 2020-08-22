# Smart Agent
README [中文](https://github.com/redteamobile/SmartAgent/blob/master-smart-agent/README.md) | [ENGLISH](https://github.com/redteamobile/SmartAgent/blob/master-smart-agent/README-EG.md) 


## 系统架构
整个系统端到端的精简架构如下图所示，红茶物联网平台与SMDP+是服务器组件，Agent与Monitor是端侧的组件
在使用实体eSIM只需要集成Agent即可，如需要软卡(iSIM)需要集成Agent与Monitor
![image](https://github.com/redteamobile/SmartAgent/blob/master-smart-agent/doc/img/system.jpg)

## 功能特色

1. *全功能 Smart Agent*
2. *全功能 实体eSIM与iSIM*
3. *Open Interface 管理Agent，管理卡片*
4. *Open Interface 选择卡片类型(实体eSIM或iSIM(软卡))*
5. *内置共享种子号，BootStrap提供初始连接*
6. *Demo 测试界面快速上手调试*

## 模块说明

* LIB Agent的核心代码，主要使用C语言实现
* SRC 是Agent的开源的源码，包括系统接口调用及Agent对外的接口
* assets是共享种子号，通过BootStrap提供初始连接

## 运行环境
* 集成开发环境Android Studio，与PC的操作系统无关