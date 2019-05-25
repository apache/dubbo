package org.apache.dubbo.rpc.proxy.asm;

import java.lang.reflect.Type;
import java.util.List;

public class MethodStatement {

	
	
	private String method;
	
	private Type returnType;
	
	private Type[] returnGeneric;
	
	//如果有泛型怎么办?
	private List<ParameterSteaement> parameterTypes;
	
	boolean futureReturnType;
	
	private Type[]  abnormalTypes;
	
	public String getMethod() {
		return method;
	}



	public void setMethod(String method) {
		this.method = method;
	}

	public Type getReturnType() {
		return returnType;
	}



	public void setReturnType(Type returnType) {
		this.returnType = returnType;
	}



	public Type[] getReturnGeneric() {
		return returnGeneric;
	}



	public void setReturnGeneric(Type[] returnGeneric) {
		this.returnGeneric = returnGeneric;
	}



	public List<ParameterSteaement> getParameterTypes() {
		return parameterTypes;
	}



	public void setParameterTypes(List<ParameterSteaement> parameterTypes) {
		this.parameterTypes = parameterTypes;
	}



	public boolean isFutureReturnType() {
		return futureReturnType;
	}



	public void setFutureReturnType(boolean futureReturnType) {
		this.futureReturnType = futureReturnType;
	}



	public Type[] getAbnormalTypes() {
		return abnormalTypes;
	}



	public void setAbnormalTypes(Type[] abnormalTypes) {
		this.abnormalTypes = abnormalTypes;
	}



	static class ParameterSteaement{
		
		private String parameterName;
		
		private Type clazz;
		
		private Type[]  genericTypes;
		
		
		public String getParameterName() {
			return parameterName;
		}

		public void setParameterName(String parameterName) {
			this.parameterName = parameterName;
		}

		public Type getType() {
			return clazz;
		}

		public void setType(Type clazz) {
			this.clazz = clazz;
		}

		public Type[] getGenericTypes() {
			return genericTypes;
		}

		public void setGenericTypes(Type[] genericTypes) {
			this.genericTypes = genericTypes;
		}
		
		
	}
}
