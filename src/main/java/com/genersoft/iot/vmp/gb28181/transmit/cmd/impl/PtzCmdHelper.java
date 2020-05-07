package com.genersoft.iot.vmp.gb28181.transmit.cmd.impl;

public class PtzCmdHelper {
   /**
    *
    * @param leftRight  镜头左移右移 0:停止 1:左移 2:右移
    * @param upDown     镜头上移下移 0:停止 1:上移 2:下移
    * @param inOut      镜头放大缩小 0:停止 1:缩小 2:放大
    * @param moveSpeed  镜头移动速度 默认 0XFF (0-255)
    * @param zoomSpeed  镜头缩放速度 默认 0X1 (0-255)
    * @return
    */
	//云台控制发送了消息，相机会一直执行，直到其他命令或者发送了停止命令，切记要考虑这个机制
    public static String create(int leftRight, int upDown, int inOut, int moveSpeed, int zoomSpeed) {
        int cmdCode = 0;
        if (leftRight == 2) cmdCode|=0x01;      // 右移
        else if(leftRight == 1) cmdCode|=0x02;  // 左移
        if (upDown == 2) cmdCode|=0x04;         // 下移
        else if(upDown == 1) cmdCode|=0x08;     // 上移
        if (inOut == 2) cmdCode |= 0x10;        // 放大
        else if(inOut == 1) cmdCode |= 0x20;    // 缩小
        char[] szCmd = new char[16];
        String strTmp;
        szCmd[0] = 'A'; //字节1 A5
        szCmd[1] = '5';
        szCmd[2] = '0'; //字节2 0F
        szCmd[3] = 'F';
        szCmd[4] = '0'; //字节3 地址的低8位
        szCmd[5] = '1';
        strTmp = String.format("%02X", cmdCode);
        szCmd[6]  = strTmp.charAt(0); //字节4 控制码
        szCmd[7]  = strTmp.charAt(1);
        strTmp = String.format("%02X", moveSpeed);
        szCmd[8]  = strTmp.charAt(0); //字节5 水平控制速度
        szCmd[9]  = strTmp.charAt(1);
        szCmd[10] = strTmp.charAt(0); //字节6 垂直控制速度
        szCmd[11] = strTmp.charAt(1);
        strTmp = String.format("%X", zoomSpeed);
        szCmd[12] = strTmp.charAt(0); //字节7高4位 缩放控制速度
        szCmd[13] = '0';              //字节7低4位 地址的高4位
        //计算校验码
        int nCheck = (0XA5 + 0X0F + 0X01 + cmdCode + moveSpeed + moveSpeed + (zoomSpeed << 4 & 0XF0)) % 0X100;
        strTmp = String.format("%02X", nCheck);
        szCmd[14] = strTmp.charAt(0); //字节8 校验码
        szCmd[15] = strTmp.charAt(1);
        return String.valueOf(szCmd);
    }
}
