package com.alibaba.dubbo.rpc.protocol.springmvc.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.alibaba.com.caucho.hessian.io.Hessian2Input;
import com.alibaba.com.caucho.hessian.io.Hessian2Output;

public class HessainHttpMessageConverter extends AbstractHttpMessageConverter<Object> {


	public HessainHttpMessageConverter() {
		super(new MediaType("application", "hessain2"),
				new MediaType("application", "*+hessain2"));
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		return true;
	}

	@Override
	protected Object readInternal(Class<? extends Object> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {
		Hessian2Input  in = new Hessian2Input(inputMessage.getBody());
		Object readObject = in.readObject(clazz);
		in.close();
		return readObject;
	}

	@Override
	protected void writeInternal(Object obj, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		OutputStream body = outputMessage.getBody();
		ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
		Hessian2Output out = new Hessian2Output(byteArrayOut);
		out.writeObject(obj);
		out.close();
		byteArrayOut.close();
		body.write(byteArrayOut.toByteArray());
		
	}

}
