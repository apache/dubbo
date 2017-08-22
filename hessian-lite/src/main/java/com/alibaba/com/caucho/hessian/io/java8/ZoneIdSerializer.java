package com.alibaba.com.caucho.hessian.io.java8;

import java.io.IOException;
import java.time.ZoneId;

import com.alibaba.com.caucho.hessian.io.AbstractHessianOutput;
import com.alibaba.com.caucho.hessian.io.AbstractSerializer;

public class ZoneIdSerializer extends AbstractSerializer {

	private static final ZoneIdSerializer SERIALIZER = new ZoneIdSerializer();
	
	public static ZoneIdSerializer getInstance() {
		return SERIALIZER;
	}

	@Override
	public void writeObject(Object obj, AbstractHessianOutput out) throws IOException {
		if (obj == null) {
			out.writeNull();
		} else {
			ZoneId zoneId = (ZoneId) obj;
			out.writeObject(new ZoneIdHandle(zoneId));
		}
	}

}
