package com.xuchongyang.mediacodecdemo;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import com.xuchongyang.mediacodecdemo.jlibrtp.DataFrame;
import com.xuchongyang.mediacodecdemo.jlibrtp.Participant;
import com.xuchongyang.mediacodecdemo.jlibrtp.RTPAppIntf;
import com.xuchongyang.mediacodecdemo.jlibrtp.RTPSession;

import java.io.IOException;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;

/**
 * InitSession
 */
public class InitSession implements RTPAppIntf {
	private static final String TAG = "InitSession";
	public RTPSession rtpSession = null;
	private Surface mSurface;
	public MediaCodec mDecoder;
	private ByteBuffer[] decoderInputBuffers;
	private ByteBuffer[] decoderOutputBuffers;

	private int mCount = 1;
	private static final int FRAME_RATE = 30;
	private byte[] buf;

	protected static String REMOTE_IP = "192.168.9.113";
	protected static int REMOTE_RTP_PORT = 8002;
	protected static int REMOTE_RTCP_PORT = 8003;

	public InitSession(Surface surface) {
		mSurface = surface;
		DatagramSocket rtpSocket = null;
		DatagramSocket rtcpSocket = null;

		try {
			rtpSocket = new DatagramSocket(8002);
			rtcpSocket = new DatagramSocket(8003);
		} catch (Exception e) {
			Log.e(TAG, "InitSession: " + "send init session exception:"+e);
		}

		//�����Ự
		rtpSession = new RTPSession(rtpSocket, rtcpSocket);
		rtpSession.RTPSessionRegister(this,null,null);
		//���ò����ߣ�Ŀ��IP��ַ��RTP�˿ڣ�RTCP�˿ڣ�
		Participant p = new Participant(REMOTE_IP, REMOTE_RTP_PORT, REMOTE_RTCP_PORT);
		rtpSession.addParticipant(p);

		initDecoder();
	}

	@Override
	public void receiveData(DataFrame frame, Participant p){
		if (buf == null){
			buf = frame.getConcatenatedData();
		} else {
			buf = Util.merge(buf, frame.getConcatenatedData());
		}
		// ����ð����λΪ true��˵��Ϊһ֡�����һ������ʼ���н������
		if (frame.marked()){
			decode(buf);
			buf = null;
		}
	}

	/**
	 * ��ʼ�� MediaCodec ������
	 */
	private void initDecoder() {
		try {
			mDecoder = MediaCodec.createDecoderByType("video/avc");
		} catch (IOException e) {
			e.printStackTrace();
		}
		MediaFormat format = MediaFormat.createVideoFormat("video/avc", 1280, 720);
		format.setInteger(MediaFormat.KEY_COLOR_FORMAT, 19);
		mDecoder.configure(format, mSurface, null, 0);
		mDecoder.start();
		decoderInputBuffers = mDecoder.getInputBuffers();
		decoderOutputBuffers = mDecoder.getOutputBuffers();
	}

	/**
	 * ��ʼ����
	 * @param data
	 */
	private void decode(byte[] data) {
		int decIndex = mDecoder.dequeueInputBuffer(-1);
		if (decIndex >= 0) {
			decoderInputBuffers[decIndex].clear();
			decoderInputBuffers[decIndex].put(data);
			int sampleSize = data.length;
			mDecoder.queueInputBuffer(decIndex, 0, sampleSize, mCount * 1000000 / FRAME_RATE, 0);
			mCount++;
		}

		MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
		int outputIndex = mDecoder.dequeueOutputBuffer(info, 0);
		while (outputIndex >= 0) {
			mDecoder.releaseOutputBuffer(outputIndex, true);
			outputIndex = mDecoder.dequeueOutputBuffer(info, 0);
		}
		if (outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
			decoderOutputBuffers = mDecoder.getOutputBuffers();
		}
	}

	public void userEvent(int type, Participant[] participant) {
	}

	public int frameSize(int payloadType) {
		return 1;
	}
}
