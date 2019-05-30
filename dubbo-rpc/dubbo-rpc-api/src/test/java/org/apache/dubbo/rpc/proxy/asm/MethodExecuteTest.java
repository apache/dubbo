package org.apache.dubbo.rpc.proxy.asm;

public class MethodExecuteTest {

	
	static class NotReturn extends AbstractMethodExecute<TestAsmProxy>{

		public NotReturn(TestAsmProxy object) {
			super(object);
		}

		@Override
		public <T> T execute(Object[] arguments) throws Throwable {
			this.getObject().notReturn();
			return null;
		}
		
	}
	
	static class ReturnInt extends AbstractMethodExecute<TestAsmProxy>{

		public ReturnInt(TestAsmProxy object) {
			super(object);
		}

		@Override
		public <T> T execute(Object[] arguments) throws Throwable {
			Integer in  = this.getObject().returnInt();
			if(in == null) {
				return (T)Integer.valueOf(0);
			}
			return (T)in;
		}
	}
	
	static class ReturnLong extends AbstractMethodExecute<TestAsmProxy>{

		public ReturnLong(TestAsmProxy object) {
			super(object);
		}

		@Override
		public <T> T execute(Object[] arguments) throws Throwable {
			Long in  = this.getObject().returnLong();
			if(in == null) {
				return (T)Long.valueOf(0);
			}
			return (T)in;
		}
	}
	
	static class ReturnObject extends AbstractMethodExecute<TestAsmProxy>{

		public ReturnObject(TestAsmProxy object) {
			super(object);
		}

		@Override
		public <T> T execute(Object[] arguments) throws Throwable {
			return (T) this.getObject().returnObject();
		}
		
	}
}
