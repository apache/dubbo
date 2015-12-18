package com.alibaba.dubbo.examples.nospring;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class JStack implements Cmd{
	
	private String file;
	
	private String fullCmd = "";
	
	public JStack(int pid, String filePath, String... options) {
		DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss_SSS");
		filePath = filePath + File.separator;
		//e.g:/home/appops/20151204133343_819-8512.threadDump
		this.file = String.format(filePath + "%s-%s.threadDump", df.format(new Date(System.currentTimeMillis())), pid);
		
		StringBuilder cmdBuilder = new StringBuilder("jstack");
		for (String op : options){
			cmdBuilder.append(" ");
			cmdBuilder.append(op);
		}
		cmdBuilder.append(" ");
		cmdBuilder.append(pid);
		fullCmd = cmdBuilder.toString();
	}

	public void exec(){
		FileOutputStream fos = null;
		InputStream in = null;
		try {
			fos = new FileOutputStream(new File(file));
			Process process = Runtime.getRuntime().exec(fullCmd);
			in = process.getInputStream();
			byte[] buf = new byte[1024 * 4]; //4K
			int read = 0;
			while ((read = in.read(buf)) > 0){
				fos.write(buf, 0, read);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if(fos != null) fos.close();
				if(in != null) in.close();
			} catch (Exception ignoreEx) {
			}
		}
	}
	
	public String getDumpFile(){
		return this.file;
	}
}
