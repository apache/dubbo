package com.alibaba.dubbo.cache.exception;

public class CacheException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3841008255052305024L;

	public CacheException(Exception e) {
		super(e);
	}

	public CacheException(String message) {
		super(message);
	}

}
