package com.wearableintelligencesystem.androidsmartglasses.comms.gatt;

import java.nio.charset.StandardCharsets;

public class DataUtil {

    public static final int BLE_MTU = 23;

    private static final int MAX_SIZE = BLE_MTU-4;
    private static final int START_BYTE = 0x01;
    private static final int CONTINUE_BYTE = 0x02;
    private static final int END_BYTE = 0x00;

    public static byte[][] encode(String strData) {
        try {
            byte[] originData = strData.getBytes(StandardCharsets.UTF_8);
            int size = (int) Math.ceil((double) originData.length / MAX_SIZE);
            byte[][] data = new byte[size][MAX_SIZE + 1];
            int start = 0;
            int end;
            int index = 0;
            while (index < size) {
                index++;
                if (index == size) {
                    data[index - 1][0] = END_BYTE;
                } else if (index == 1) {
                    data[0][0] = START_BYTE;
                } else {
                    data[index - 1][0] = CONTINUE_BYTE;
                }
                end = Math.min(start + MAX_SIZE, originData.length);
                System.arraycopy(originData, start, data[index - 1], 1, end - start);
                start = end;
            }
            return data;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] decode(byte[] data, byte[] result) {
        byte[] tempResult;
        if (result == null || isStart(data)) {
            tempResult = new byte[data.length - 1];
            System.arraycopy(data, 1, tempResult, 0, data.length - 1);
        } else {
            tempResult = new byte[data.length - 1 + result.length];
            System.arraycopy(result, 0, tempResult, 0, result.length);
            System.arraycopy(data, 1, tempResult, result.length, data.length - 1);
        }
        if (isEnd(data)) {
            byte zeroByte = 0x00;
            int length = tempResult.length;
            for (int i = 0; i < length; i++) {
                if (tempResult[length - i - 1] == zeroByte) {
                    continue;
                }
                byte[] realResult = new byte[length - i];
                System.arraycopy(tempResult, 0, realResult, 0, length - i);
                tempResult = realResult;
                break;
            }
        }
        return tempResult;
    }

    public static boolean isStart(byte[] data) {
        return data[0] == START_BYTE;
    }

    public static boolean isEnd(byte[] data) {
        return data[0] == END_BYTE;
    }

}
