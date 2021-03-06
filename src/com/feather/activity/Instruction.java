package com.feather.activity;

/** 
 * @CopyRight: 王辰浩 2015~2025
 * @Author Feather Hunter(猎羽)
 * @qq: 975559549
 * @Version: 1.1
 * @Date: 2016/1/10
 * @Description: IHome自制的通信协议。包含了其中所有指令。
 **/

public class Instruction {

    public static final byte COMMAND_PULSE  ='0'; //心跳指令
    public static final byte COMMAND_MANAGE = 1;  //管理指令
    public static final byte COMMAND_CONTRL = 2;  //控制指令
    public static final byte COMMAND_RESULT = 3;  //结果指令
    public static final byte COMMAND_VIDEO  = 4;  //视频指令
    public static final byte MAN_LOGIN      = 11; //管理-登录
    public static final byte CTL_LAMP       = 21; //控制-灯
    public static final byte CTL_GET        = 22; //控制-获取
    public static final byte CTL_IHome      = 23; //控制-IHome
    public static final byte CTL_VIDEO      = 24; //控制-视频
    public static final byte RES_LOGIN      = 32; //结果-登录
    public static final byte RES_LAMP       = 33; //结果-灯
    public static final byte RES_TEMP       = 34; //结果-温度
    public static final byte RES_HUMI       = 35; //结果-湿度
    public static final byte RES_IHome      = 36; //结果-IHome
    public static final byte RES_VIDEO      = 37; //结果-视频
    public static final byte VIDEO_START    = 41; //视频-开启
    public static final byte VIDEO_STOP     = 42; //视频-关闭
    public static final byte LOGIN_SUCCESS  = 1;  //登录成功
    public static final byte LOGIN_FAILED   = 2;  //登录失败
    public static final byte LAMP_ON        = 1;  //灯开启
    public static final byte LAMP_OFF       = 2;  //灯关闭
    public static final byte IHome_START    = 1;  //IHome模式开启
    public static final byte IHome_STOP     = 2;  //IHome模式关闭
    public static final byte VIDEO_OK       = 1;  //视频OK
    public static final byte VIDEO_ERROR    = 2;  //视频错误
    
    public static final byte COMMAND_SEPERATOR = 31;//31单元分隔符
    public static final byte COMMAND_END    = 30; //30,一个指令结束
}
