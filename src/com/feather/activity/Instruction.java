package com.feather.activity;

/** 
 * @CopyRight: ������ 2015~2025
 * @Author Feather Hunter(����)
 * @qq:975559549
 * @Version:1.1 
 * @Date:2016/1/10
 * @Description: IHome���Ƶ�ͨ��Э�顣��������������ָ�
 **/

public class Instruction {

  //char COMMAND_PULSE  '0'
    public static final byte COMMAND_MANAGE = 1;  //����ָ��
    public static final byte COMMAND_CONTRL = 2;  //����ָ��
    public static final byte COMMAND_RESULT = 3;  //���ָ��
    public static final byte COMMAND_VIDEO  = 4;  //��Ƶָ��
    public static final byte MAN_LOGIN      = 11; //����-��¼
    public static final byte CTL_LAMP       = 21; //����-��
    public static final byte CTL_GET        = 22; //����-��ȡ
    public static final byte CTL_IHome      = 23; //����-IHome
    public static final byte CTL_VIDEO      = 24; //����-��Ƶ
    public static final byte RES_LOGIN      = 32; //���-��¼
    public static final byte RES_LAMP       = 33; //���-��
    public static final byte RES_TEMP       = 34; //���-�¶�
    public static final byte RES_HUMI       = 35; //���-ʪ��
    public static final byte RES_IHome      = 36; //���-IHome
    public static final byte RES_VIDEO      = 37; //���-��Ƶ
    public static final byte VIDEO_START    = 41; //��Ƶ-����
    public static final byte VIDEO_STOP     = 42; //��Ƶ-�ر�
    public static final byte LOGIN_SUCCESS  = 1;  //��¼�ɹ�
    public static final byte LOGIN_FAILED   = 2;  //��¼ʧ��
    public static final byte LAMP_ON        = 1;  //�ƿ���
    public static final byte LAMP_OFF       = 2;  //�ƹر�
    public static final byte IHome_START    = 1;  //IHomeģʽ����
    public static final byte IHome_STOP     = 2;  //IHomeģʽ�ر�
    public static final byte VIDEO_OK       = 1;  //��ƵOK
    public static final byte VIDEO_ERROR    = 2;  //��Ƶ����
    
    public static final byte COMMAND_SEPERATOR = 31;//31��Ԫ�ָ���
    public static final byte COMMAND_END    = 30; //30,һ��ָ�����
}
