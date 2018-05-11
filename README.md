# IcoBle
# 引入方式
## Gradle
```
compile 'ico.ico.ble:IcoBle:1.0.2'
```
## Maven
```
<dependency>
  <groupId>ico.ico.ble</groupId>
  <artifactId>IcoBle</artifactId>
  <version>1.0.2</version>
  <type>pom</type>
</dependency>
```
## lvy
```
<dependency org='ico.ico.ble' name='IcoBle' rev='1.0.2'>
  <artifact name='IcoBle' ext='pom' ></artifact>
</dependency>
```


# 项目说明
基于代码设计复用性的考虑，我设计了一套蓝牙操作以及针对实际工作中基于蓝牙协议版本设计的一系列蓝牙操作的类

基于提高开发效率的初衷，在代码设计中，尽量让开发者只考虑具体的业务逻辑实现，而不去考虑底层实现

根据思考我发现了蓝牙开发中以下几个特点：
### 1、不管谁做蓝牙开发，基于android api的蓝牙处理代码都是一样的
### 2、针对不同的蓝牙设备操作，只有蓝牙的服务通道UUID，发送和接收的数据格式不同

#### 根据特点1：我编写了ico.ico.ble中的BleCallback,BleHelper,BleSocket
他们主要负责调用android api，完成蓝牙的搜索，连接，通道建立,数据收发等操作
他们是根据特点1，对android api的蓝牙处理代码进行的抽离和封装

##### BleCallback
用于蓝牙回调，包括蓝牙发现，连接成功/失败，断开连接，发送数据成功/失败，接收数据
##### BleHelper
主要负责蓝牙的搜索，设备的蓝牙开关以及检查权限等功能；
##### BleSocket
BleHelper在搜索到设备后，将蓝牙设备对象交由BleSocket进行处理，BleSocket的封装，围绕着蓝牙设备对象，对其进行各种操作处理；包括连接，断开连接，发送数据，接收数据等；


#### 根据特点2，我编写了BleMgr，BleMgr的设计原理基于管理者模式以及插件化（没看过设计模式，不过应该大同小异）
根据实际项目的协议格式,编写对应的BleMgr,通过插入不同的BleSocket进行控制/数据收发/数据解析
工欲善其事必先利其器，这里的BleMgr就是器，针对不同协议构建的器

插件化指得是BleSocket，将不同的BleSocket插入到BleMgr，就可以根据BleMgr对应的协议对BleSocket进行控制操作；

##### BleMgr
其中封装了协议所具有的操作以及根据对应的协议版本进行的数据解析，其中还包括了一个CurrentOperateFlag，用于标识当前蓝牙的操作状态,根据实际的协议格式修改一下数据协议和数据解析就可以使用
##### BleMgr.Command
这个类用于提供基于协议的操作数据
##### CurrentOperateFlag
由于蓝牙数据发送太快会导致断包粘包的问题，所以在实际的处理中，通常是发送一条数据后，等待接收成功后再发下一条指令，所以需要通过标志来判断当前处于什么操作，或者说当前是否正在操作中；
