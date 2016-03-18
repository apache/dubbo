package com.netease.learn;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class SemaphoreTest {

	public static void main(String[] args) throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(10);
		final Semaphore sem = new Semaphore(5, true);
		sem.acquire();
		for (int i =0; i < 10; i++){
			final int num = i;
			executor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						sem.acquire();
						int sec = 1000*new Random().nextInt(5);
						System.out.println("execute i =" + num + ", cost:" + sec);
						Thread.sleep(sec);
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						sem.release();
					}
				}
			});
		}
	}
	
}
