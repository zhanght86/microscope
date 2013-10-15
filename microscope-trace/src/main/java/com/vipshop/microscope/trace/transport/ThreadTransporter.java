package com.vipshop.microscope.trace.transport;

import java.util.ArrayList;
import java.util.List;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vipshop.microscope.thrift.LogEntry;
import com.vipshop.microscope.trace.Constant;
import com.vipshop.microscope.trace.encode.Encoder;
import com.vipshop.microscope.trace.queue.MessageQueue;
import com.vipshop.microscope.trace.thrift.ThriftClient;

/**
 * Use a {@code Thread} transport message to zipkin collector.
 * 
 * @author Xu Fei
 * @version 1.0
 */
public class ThreadTransporter implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(ThreadTransporter.class);
	
	private final ThriftClient transporter = new ThriftClient();
	
	private final List<LogEntry> logEntries = new ArrayList<LogEntry>();
	private final Encoder encode = new Encoder();
	
	private final int MAX_BATCH_SIZE = Constant.MAX_BATCH_SIZE;
	private final int MAX_EMPTY_SIZE = Constant.MAX_EMPTY_SIZE;
	
	@Override
	public void run() {
		
		int emptySize = 0;
		
		while (ThriftClient.isConnect()) {
			Object object = MessageQueue.poll();
			if (object == null)
				emptySize++;
			else {
				try {
					logEntries.add(encode.encodeToLogEntry(object));
				} catch (TException e) {
					logger.error("encode Span to LogEntry error, program will ingnore this span");
				}
			}
			
			boolean emptySizeFlag = emptySize >= MAX_EMPTY_SIZE && !logEntries.isEmpty();
			boolean batchSizeFlag = logEntries.size() >= MAX_BATCH_SIZE;
			
			if (emptySizeFlag || batchSizeFlag) {
				transporter.send(logEntries);
				logEntries.clear();
				emptySize = 0;
			}
		}
		
	}
	
}