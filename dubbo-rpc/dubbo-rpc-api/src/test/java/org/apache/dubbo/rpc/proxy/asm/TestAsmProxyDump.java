package org.apache.dubbo.rpc.proxy.asm;

import java.util.*;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.objectweb.asm.*;

public class TestAsmProxyDump extends ClassLoader implements Opcodes {

	public  void dump() throws Exception {

		ClassWriter cw = new ClassWriter(0);
		FieldVisitor fv;
		MethodVisitor mv;

		cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER, "AbstractAsmProxy2", null,
				"org/apache/dubbo/rpc/proxy/asm/AbstractAsmProxy",
				new String[] { "org/apache/dubbo/rpc/proxy/RemoteService" });

		cw.visitSource("TestAsmProxy.java", null);

		{
			fv = cw.visitField(ACC_PRIVATE, "sayHello_statement", "Lorg/apache/dubbo/rpc/proxy/asm/MethodStatement;",
					null, null);
			fv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(Lorg/apache/dubbo/rpc/Invoker;)V",
					null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitLineNumber(11, l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKESPECIAL, "org/apache/dubbo/rpc/proxy/asm/AbstractAsmProxy", "<init>",
					"(Lorg/apache/dubbo/rpc/Invoker;)V", false);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitTypeInsn(NEW, "org/apache/dubbo/rpc/proxy/asm/MethodStatement");
			mv.visitInsn(DUP);
			mv.visitMethodInsn(INVOKESPECIAL, "org/apache/dubbo/rpc/proxy/asm/MethodStatement", "<init>", "()V", false);
			mv.visitFieldInsn(PUTFIELD, "AbstractAsmProxy2", "sayHello_statement",
					"Lorg/apache/dubbo/rpc/proxy/asm/MethodStatement;");
			
			
			
			mv.visitInsn(RETURN);
			Label l3 = new Label();
			mv.visitLabel(l3);
			mv.visitLocalVariable("this", "Lorg/apache/dubbo/rpc/proxy/asm/TestAsmProxy;", null, l0, l3, 0);
			mv.visitLocalVariable("handler", "Lorg/apache/dubbo/rpc/Invoker;", "Lorg/apache/dubbo/rpc/Invoker<*>;", l0,
					l3, 1);
			mv.visitMaxs(3, 2);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC, "sayHello", "(Ljava/lang/String;)Ljava/lang/String;", null,
					new String[] { "java/rmi/RemoteException" });
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, "AbstractAsmProxy2", "sayHello_statement",
					"Lorg/apache/dubbo/rpc/proxy/asm/MethodStatement;");
			mv.visitInsn(ICONST_1);
			mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitInsn(AASTORE);
			mv.visitMethodInsn(INVOKESPECIAL, "org/apache/dubbo/rpc/proxy/asm/AbstractAsmProxy", "invoke",
					"(Lorg/apache/dubbo/rpc/proxy/asm/MethodStatement;[Ljava/lang/Object;)Ljava/lang/Object;", false);
			mv.visitTypeInsn(CHECKCAST, "java/lang/String");
			mv.visitInsn(ARETURN);
			mv.visitMaxs(6, 2);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC, "getThreadName", "()Ljava/lang/String;", null,
					new String[] { "java/rmi/RemoteException" });
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitLineNumber(21, l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, "AbstractAsmProxy2", "sayHello_statement",
					"Lorg/apache/dubbo/rpc/proxy/asm/MethodStatement;");
			mv.visitMethodInsn(INVOKESPECIAL, "org/apache/dubbo/rpc/proxy/asm/AbstractAsmProxy", "invoke",
					"(Lorg/apache/dubbo/rpc/proxy/asm/MethodStatement;)Ljava/lang/Object;", false);
			mv.visitTypeInsn(CHECKCAST, "java/lang/String");
			mv.visitInsn(ARETURN);
			Label l1 = new Label();
			mv.visitLabel(l1);
			mv.visitLocalVariable("this", "LAbstractAsmProxy2;", null, l0, l1, 0);
			mv.visitMaxs(2, 1);
			mv.visitEnd();
		}
		cw.visitEnd();

		byte[] data = cw.toByteArray();
		Class<?> clazz = this.defineClass("AbstractAsmProxy2", data, 0, data.length);
		Object object = clazz.getConstructor(Invoker.class).newInstance(new Invoker<String>() {

			@Override
			public URL getUrl() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public boolean isAvailable() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public void destroy() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public Class<String> getInterface() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Result invoke(Invocation invocation) throws RpcException {
				// TODO Auto-generated method stub
				return null;
			}});
		System.out.println(object.getClass().getName());
	}
}
