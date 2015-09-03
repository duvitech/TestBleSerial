package com.example.george.testbleserial;

import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by George on 9/3/2015.
 */

public class SerialProtocol {

    private static final String TAG = "SerialProtocol";

    public enum FrameTypes {
        STRING, BINARY, ACKNOWLEDGE
    }

    private FrameTypes mData_type;
    private long mFrame_id;
    private long mData_len;
    private int mTotal_packets;
    private char mTimeout;
    private char mReg_Config;        // bit 5-7 unused bit 4 is ack on  bit 0-3 is retry count
    private long mData_crc;


    private int mCurrentPacket;

    public SerialProtocol(FrameTypes frame_type, int data_length) {
        if (data_length < 1 || data_length > 1245165) {
            throw new ExceptionInInitializerError();
        }

        mData_type = frame_type;

        if (mData_type == FrameTypes.ACKNOWLEDGE) {
            mData_len = data_length;
            mTotal_packets = 0;
            mReg_Config = 0x00;
            mTimeout = 0; // infinite timeout
            mData_crc = 0;
        } else {
            mData_len = data_length;
            mTotal_packets = (short) (data_length / 19);
            if (data_length % 19 > 0) {
                mTotal_packets++;
            }

            mReg_Config = 0x00;
            mTimeout = 0; // infinite timeout
            mData_crc = 0;
        }

        mCurrentPacket = -1;
    }

    public boolean hasNextPacket(){
        if(mCurrentPacket < mTotal_packets)
            return true;
        else
            return false;
    }

    byte[] getNextPacket(byte[] data){
        if(mCurrentPacket == -1){
            mCurrentPacket = 0;
            return getPacketHeader();
        }
        else if(mCurrentPacket < mTotal_packets){
            if(data.length != mData_len) {
                Log.e(TAG, "Data provided doesn't match length of data used ot generate protocol information");
                return null;
            }

            int startIndex = mCurrentPacket * 19;
            int endIndex = startIndex + 19;
            int len = endIndex - startIndex;

            if(endIndex >= mData_len) {
                endIndex = (int) mData_len - 1;
                len = endIndex - startIndex + 1;
            }

            ByteBuffer temp = ByteBuffer.allocate(20);
            temp.put(0,(byte)len);
            for(int x = 0; x < len; x++) {
                temp.put(x+1, data[startIndex + x]);
            }

            mCurrentPacket++;
            return temp.array();

        }
        else
        {
            // no more packets
            return null;
        }

    }

    public long get_Data_crc() {
        return mData_crc;
    }

    public void set_Data_crc(byte[] data) {
        // calculate crc
        this.mData_crc = 0;
    }

    public char get_Reg_Config() {
        return mReg_Config;
    }

    public int get_Retry_Count(){
        return mReg_Config & 0x0F;
    }

    public void set_Retry_Count(int trys){
        if(trys >15)
            trys = 15;
        if(trys<0)
            trys = 0;
        mReg_Config &= 0xF0; // clear retry bits
        mReg_Config |= (0x0F & (byte)trys) ;
    }

    public void set_Acknowledge(boolean b_on) {
        if(b_on){
            mReg_Config |= 0x10;
        }
        else
        {
            mReg_Config &= 0xEF;
        }

    }

    public char getmTimeout() {
        return mTimeout;
    }

    public void setmTimeout(char mTimeout) {
        this.mTimeout = mTimeout;
    }

    public int getmTotal_packets() {
        return mTotal_packets;
    }

    public void setmTotal_packets(short mTotal_packets) {
        this.mTotal_packets = mTotal_packets;
    }

    public long getmData_len() {
        return mData_len;
    }

    public void setmData_len(int mData_len) {
        this.mData_len = mData_len;
    }

    public long getmFrame_id() {
        return mFrame_id;
    }

    public void setmFrame_id(int mFrame_id) {
        this.mFrame_id = mFrame_id;
    }

    public FrameTypes getmData_type() {
        return mData_type;
    }

    public void setmData_type(FrameTypes mData_type) {
        this.mData_type = mData_type;
    }

    public byte[] getPacketHeader() {
        ByteBuffer startPacket = ByteBuffer.allocate(20);
        startPacket.put(0,(byte)19);

        // data type
        startPacket.put(1,(byte)0xAB);
        if (mData_type == FrameTypes.ACKNOWLEDGE) {
            startPacket.put(2,(byte)0xA5);
        }
        else if (mData_type == FrameTypes.STRING)
        {
            startPacket.put(2,(byte)0xAD);
        }
        else
        {
            startPacket.put(2,(byte)0xDA);
        }

        // frame Id
        startPacket.put(3, (byte) 0xCC);
        startPacket.put(4,(byte)0xCC);
        startPacket.put(5,(byte)0xBB);
        startPacket.put(6, (byte) 0xBB);

        // data length
        startPacket.putInt(7, (int) mData_len);

        // total packets 11
        startPacket.putShort(11, (short) mTotal_packets);

        // timeout  13
        startPacket.putChar(13, mTimeout);

        // timeout  14
        startPacket.putChar(14, mReg_Config);

        // reserved  15
        startPacket.put(15, (byte) 0x00);

        // crc 16
        startPacket.putInt(16, (int)mData_crc);

        return startPacket.array();
    }
}
