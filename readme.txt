Copyright 版权所有 @IFuture Technology
本项目是IFuture科技公司(intelligent future technolgy)旗下IHome(智能家庭)项目中 用户Android的APP
基于Android

1.项目负责人:feather(王辰浩)
2.项目文件指引:
  包
  1.com.feather.activity           
		   ClientActivity.java     登陆界面
		   ClientMainActivity.java 登陆成功后控制界面
                   Instructions.Java       自定义通信协议指令
  2.com.feather.bottombar   APP下方切换界面栏
		   BaseFragment.java       所有Fragment父类
		   BottomBarPanel.java     低栏          
  3.com.feather.fragment
		   FragmentIHome.java      智能家居主控界面Fragment
	           FragmentVideo.java      视频界面的Fragment
  4.com.feather.service
		   IHomeService.java       后台服务用于连接服务器,stm32，收发信息等。

3.版本信息与功能介绍:

v2.0  @Date:2015/12/27  能进行基础的控制，灯和湿度。
v2.10 @Date:2016/1/2    修复了手机待机程序自动崩溃的重大BUG，修复了登陆界面和控制界面来回多次切换显示不正确的BUG。
v2.20 @Date:2016/1/18   增加了wifi模式，也支持wifi模式和以太网模式间却换


4.工作完成的代码量

feather（王辰浩）贡献代码量：3210行