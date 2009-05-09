package com.othersonline.kv.distributed;

public interface ContextSerializer {

	public byte[] addContext(byte[] objectData);

	public ExtractedContext<byte[]> extractContext(Node source, byte[] rawData);
}