package com.smartglassesmanager.androidsmartphone.smartglassescommunicators.gatt

import kotlin.math.ceil
import kotlin.math.min

object BLEDataUtil {
    const val BLE_MTU = 23
    private const val MAX_SIZE = BLE_MTU - 4 // MTU - 4
    private const val START_BYTE: Byte = 0x01
    private const val CONTINUE_BYTE: Byte = 0x02
    private const val END_BYTE: Byte = 0x00

    fun encode(strData: String): Array<ByteArray>? {
        try {

            val originData = strData.toByteArray(charset("UTF-8"))
            val size = ceil(originData.size / (MAX_SIZE * 1.0)).toInt()
            val data = Array(size) { ByteArray(MAX_SIZE + 1) }
            var start = 0
            var end: Int
            var index = 0
            while (index < size) {
                index++
                when (index) {
                    size -> data[index - 1][0] = END_BYTE
                    1 -> data[0][0] = START_BYTE
                    else -> data[index - 1][0] = CONTINUE_BYTE
                }
                end = min(start + MAX_SIZE, originData.size)
                System.arraycopy(originData, start, data[index - 1], 1, end - start)
                start = end
            }
            return data
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun decode(data: ByteArray, result: ByteArray?): ByteArray {
        var tempResult: ByteArray
        if (result == null || isStart(data)) {
            tempResult = ByteArray(data.size - 1)
            System.arraycopy(data, 1, tempResult, 0, data.size - 1)
        } else {
            tempResult = ByteArray(data.size - 1 + result.size)
            System.arraycopy(result, 0, tempResult, 0, result.size)
            System.arraycopy(data, 1, tempResult, result.size, data.size - 1)
        }
        if (isEnd(data)) {
            //去掉结尾的0x00字符，避免转换成字符串乱码
            val zeroByte: Byte = 0x00
            val length = tempResult.size
            for (i in 0 until length) {
                if (tempResult[length - i - 1] == zeroByte) {
                    continue
                }
                val realResult = ByteArray(length - i)
                System.arraycopy(tempResult, 0, realResult, 0, length - i)
                tempResult = realResult
                break
            }
        }
        return tempResult
    }

    private fun isStart(data: ByteArray): Boolean {
        return data[0] == START_BYTE
    }

    fun isEnd(data: ByteArray): Boolean {
        return data[0] == END_BYTE
    }

}