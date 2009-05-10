package com.othersonline.kv.distributed.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.othersonline.kv.distributed.HashAlgorithm;

public class MD5HashAlgorithm implements HashAlgorithm {

	public long hash(final String key) {
		byte[] bytes = md5(key);
		long l = 0;
		for (int i = 8; i < 16; i++) {
			l <<= 8;
			l ^= (long) bytes[i] & 0xFF;
		}
		return l;
	}

	private byte[] md5(final String key) {
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.reset();
			md5.update(key.getBytes());
			byte[] bytes = md5.digest();
			return bytes;
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Cannot find MD5 message digest!");
		}

	}

}