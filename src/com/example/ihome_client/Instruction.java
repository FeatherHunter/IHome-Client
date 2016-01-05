package com.example.ihome_client;

/** 
 * @CopyRight: 王辰浩 2015~2025
 * @Author Feather Hunter(猎羽)
 * @qq:975559549
 * @Version:1.0 
 * @Date:2015/12/25
 * @Description: IHome自制的通信协议。包含了其中所有指令。
 **/

public class Instruction {

  //char COMMAND_PULSE  '0'
    public static final byte COMMAND_MANAGE = 1;
    public static final byte COMMAND_CONTRL = 2;
    public static final byte COMMAND_RESULT = 3;
    public static final byte MAN_LOGIN      = 11;
    public static final byte CTL_LAMP       = 21;
    public static final byte CTL_GET        = 22;
    public static final byte CTL_IHome      = 23;
    public static final byte RES_LOGIN      = 32;
    public static final byte RES_LAMP       = 33;
    public static final byte RES_TEMP       = 34;
    public static final byte RES_HUMI       = 35;
    public static final byte RES_IHome      = 36;
    public static final byte LOGIN_SUCCESS  = 1;
    public static final byte LOGIN_FAILED   = 2;
    public static final byte LAMP_ON        = 1;
    public static final byte LAMP_OFF       = 2;
    public static final byte IHome_START    = 1;
    public static final byte IHome_STOP     = 2;
    public static final byte COMMAND_SEPERATOR = 31;//31单元分隔符
    public static final byte COMMAND_END    = 30; //30,一个指令结束
}
