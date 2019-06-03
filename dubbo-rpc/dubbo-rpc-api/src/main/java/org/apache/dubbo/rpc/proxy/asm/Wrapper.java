package org.apache.dubbo.rpc.proxy.asm;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.proxy.asm.MethodStatement.ParameterSteaement;
import org.apache.dubbo.rpc.proxy.asm.ReflectUtils.BasicTypesOpcodes;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class Wrapper extends ClassLoader{

	private final static String HAVE_PARAMETER = "(Lorg/apache/dubbo/rpc/proxy/asm/MethodStatement;[Ljava/lang/Object;)Ljava/lang/Object;";

	private final static String NOT_PARAMETER = "(Lorg/apache/dubbo/rpc/proxy/asm/MethodStatement;)Ljava/lang/Object;";
	
	private Map<String/*alias*/ , MethodStatement>  aliasAndMethodStatement = new HashMap<String, MethodStatement>();
	
	
	public MethodStatement getMethodStatement(String alias) {
		return aliasAndMethodStatement.get(alias);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getProxy(Class<?>[] types, Invoker<?> handler) {
		try {
			Class<?> clazz = getProxyClass(types);
			Object object = clazz.getConstructor(Invoker.class).newInstance(handler);
			setField(object);
			return (T) object;
		}catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void setField(Object object) throws IllegalArgumentException, IllegalAccessException {
		Class<?> clazz = object.getClass();
		Field[] fields = clazz.getDeclaredFields();
		for(Field field : fields) {
			field.setAccessible(true);
			field.set(object, aliasAndMethodStatement.get(field.getName()));
		}
	}
	
	private Class<?> getProxyClass(Class<?>[] types) {
		String className = ReflectUtils.getProxyName(types);
		ClassWriter cw = new ClassWriter(0);
		cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, className, null, "org/apache/dubbo/rpc/proxy/asm/AbstractAsmProxy",
				ReflectUtils.getClassName(types));
		{
			MethodVisitor mw = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(Lorg/apache/dubbo/rpc/Invoker;)V",
					"(Lorg/apache/dubbo/rpc/Invoker<*>;)V", null);
			mw.visitVarInsn(Opcodes.ALOAD, 0);
			mw.visitVarInsn(Opcodes.ALOAD, 1);
			mw.visitMethodInsn(Opcodes.INVOKESPECIAL, "org/apache/dubbo/rpc/proxy/asm/AbstractAsmProxy", "<init>",
					"(Lorg/apache/dubbo/rpc/Invoker;)V", false);
			mw.visitInsn(Opcodes.RETURN);
			mw.visitMaxs(2, 2);
			mw.visitEnd();
		}
		for (Class<?> clazz : types) {
			List<MethodStatement> msList = ReflectUtils.analysisMethod(clazz);
			for (MethodStatement ms : msList) {
				String statement = ReflectUtils.getMethodStatement(ms.getParameterTypes(), ms.getReturnType());;
				aliasAndMethodStatement.put(ms.getAlias(), ms);
				{
					cw.visitField(Opcodes.ACC_PRIVATE, ms.getAlias(),"Lorg/apache/dubbo/rpc/proxy/asm/MethodStatement;", null, null).visitEnd();
				}
				MethodVisitor mw = cw.visitMethod(Opcodes.ACC_PUBLIC, ms.getMethod(), statement, null,
						ReflectUtils.getClassName(ms.getAbnormalTypes()));
				List<ParameterSteaement> parameter = ms.getParameterTypes();
				mw.visitVarInsn(Opcodes.ALOAD, 0);
				mw.visitVarInsn(Opcodes.ALOAD, 0);
				mw.visitFieldInsn(Opcodes.GETFIELD, className, ms.getAlias(),
						"Lorg/apache/dubbo/rpc/proxy/asm/MethodStatement;");
				int maxStack = 2, maxLocals = 1, loadIndex = 1;
				boolean is64Type = true;
				String desc = NOT_PARAMETER;
				if (parameter != null && parameter.size() != 0) {
					desc = HAVE_PARAMETER;
					maxStack = 6;
					maxLocals = maxLocals + parameter.size();
					if (parameter.size() < 6) {
						mw.visitInsn(3 + parameter.size());
					} else {
						mw.visitIincInsn(Opcodes.BIPUSH, parameter.size());
					}
					mw.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
					for (int i = 0; i < parameter.size(); i++) {
						mw.visitInsn(Opcodes.DUP);
						if (i < 7) {
							mw.visitInsn(3 + i);
						} else {
							mw.visitIincInsn(Opcodes.BIPUSH, i);
						}
						Class<?> parameterClass = (Class<?>) parameter.get(i).getType();
						if (parameterClass.isPrimitive()) {// 判断是基本类型
							BasicTypesOpcodes basicTypesOpcodes = ReflectUtils.getBasicTypesOpcodes(parameterClass);
							mw.visitVarInsn(basicTypesOpcodes.getLoadValue(), loadIndex++);
							mw.visitMethodInsn(Opcodes.INVOKESTATIC, basicTypesOpcodes.getPackingAsmTypeName(), "valueOf", basicTypesOpcodes.getBasieTurnName(), false);
							if (parameterClass.equals(long.class) || parameterClass.equals(double.class)) {
								maxLocals = maxLocals + 1;
								loadIndex++;
							}
							if (is64Type) {
								is64Type = false;
								maxStack = maxStack + 1;
							}
						} else {
							mw.visitVarInsn(Opcodes.ALOAD, loadIndex++);
						}
						mw.visitInsn(Opcodes.AASTORE);
					}
				}
				mw.visitMethodInsn(Opcodes.INVOKESPECIAL, "org/apache/dubbo/rpc/proxy/asm/AbstractAsmProxy", "invoke",
						desc, false);
				if (ms.getReturnType() == null || void.class.equals(ms.getReturnType())) {
					mw.visitInsn(Opcodes.POP);
					mw.visitInsn(Opcodes.RETURN);
				} else {
					Class<?> type = (Class<?>) ms.getReturnType();
					if (type.isPrimitive()) {
						BasicTypesOpcodes basicTypesOpcodes = ReflectUtils.getBasicTypesOpcodes(type);
						mw.visitTypeInsn(Opcodes.CHECKCAST, basicTypesOpcodes.getPackingAsmTypeName());
						mw.visitVarInsn(Opcodes.ASTORE, maxLocals);
						mw.visitVarInsn(Opcodes.ALOAD, maxLocals);
						Label l2 = new Label();
						mw.visitJumpInsn(Opcodes.IFNULL, l2);
						mw.visitVarInsn(Opcodes.ALOAD, maxLocals);
						mw.visitMethodInsn(Opcodes.INVOKEVIRTUAL, basicTypesOpcodes.getPackingAsmTypeName(), basicTypesOpcodes.getToBasieMethomName(), basicTypesOpcodes.getBasieStatement(), false);
						mw.visitInsn(basicTypesOpcodes.getReturnValue());
						mw.visitLabel(l2);
						mw.visitFrame(Opcodes.F_APPEND, 1, new Object[] { basicTypesOpcodes.getPackingAsmTypeName() }, 0, null);
						mw.visitInsn(basicTypesOpcodes.getConstValue());
						mw.visitInsn(basicTypesOpcodes.getReturnValue());
						maxLocals++;
					} else {
						mw.visitTypeInsn(Opcodes.CHECKCAST, ReflectUtils.getClassName(type));
						mw.visitInsn(Opcodes.ARETURN);
					}
				}
				mw.visitMaxs(maxStack, maxLocals);
				mw.visitEnd();
			}
		}
		cw.visitEnd();
		byte[] data = cw.toByteArray();
		return this.defineClass(className, data, 0, data.length);
	}

	public Map<String, MethodExecute<?>> getInvoke(Object proxy, Class<?> type) {
		try {
			Map<String, MethodExecute<?>> map = new HashMap<>();
			List<MethodStatement> methodStatementList = ReflectUtils.analysisMethod(type);
			String invokerClassName = ReflectUtils.getClassName(type);
			for (MethodStatement methodStatement : methodStatementList) {
				Class<?> clazz = doGetInvoke(methodStatement, invokerClassName);
				map.put(methodStatement.getAlias(),
						(MethodExecute<?>) (clazz.getConstructor(type).newInstance(proxy)));
				aliasAndMethodStatement.put(methodStatement.getAlias(), methodStatement);
			}
			return map;
		}catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Class<?> doGetInvoke(MethodStatement methodStatement, String invokerClassName) {
		ClassWriter cw = new ClassWriter(0);
		MethodVisitor mv;
		String invokerObjectName = methodStatement.getAlias() + "MethodExecute";
		cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, invokerObjectName,
				"Lorg/apache/dubbo/rpc/proxy/asm/AbstractMethodExecute<L" + invokerClassName + ";>;",
				"org/apache/dubbo/rpc/proxy/asm/AbstractMethodExecute", null);

		{
			mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(L" + invokerClassName + ";)V", null, null);
			mv.visitCode();
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitVarInsn(Opcodes.ALOAD, 1);
			mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "org/apache/dubbo/rpc/proxy/asm/AbstractMethodExecute", "<init>",
					"(Ljava/lang/Object;)V", false);
			mv.visitInsn(Opcodes.RETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "execute", "([Ljava/lang/Object;)Ljava/lang/Object;",
					"<T:Ljava/lang/Object;>([Ljava/lang/Object;)TT;", new String[] { "java/lang/Throwable" });
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			//mv.visitFieldInsn(GETFIELD, invokerObjectName, "object", "Ljava/lang/Object;");
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, invokerObjectName, "getObject", "()Ljava/lang/Object;", false);
			mv.visitTypeInsn(Opcodes.CHECKCAST, invokerClassName);
			int maxStack = 1;
			List<ParameterSteaement> parameterList = methodStatement.getParameterTypes();
			if (parameterList != null && !parameterList.isEmpty()) {
				maxStack = maxStack + 1 + parameterList.size();
				for (int i = 0; i < parameterList.size(); i++) {
					ParameterSteaement parameter = parameterList.get(i);
					mv.visitVarInsn(Opcodes.ALOAD, 1);
					if (i < 7) {
						mv.visitInsn(3 + i);
					} else {
						mv.visitIincInsn(Opcodes.BIPUSH, i + 1);
					}
					mv.visitInsn(Opcodes.AALOAD);
					Class<?> tpye = (Class<?>) parameter.getType();
					if (tpye.isPrimitive()) {
						maxStack = maxStack + 1;
						BasicTypesOpcodes basicTypesOpcodes = ReflectUtils.getBasicTypesOpcodes(tpye);
						mv.visitTypeInsn(Opcodes.CHECKCAST, basicTypesOpcodes.getPackingAsmTypeName());
						mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, basicTypesOpcodes.getPackingAsmTypeName(), basicTypesOpcodes.getToBasieMethomName(), basicTypesOpcodes.getBasieStatement(), false);
					} else {
						mv.visitTypeInsn(Opcodes.CHECKCAST, ReflectUtils.getClassName(parameter.getType()));
					}
				}
			}

			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, invokerClassName, methodStatement.getMethod(),
					ReflectUtils.getMethodStatement(parameterList, methodStatement.getReturnType()), false);
			if (void.class.equals(methodStatement.getReturnType())) {
				mv.visitInsn(Opcodes.ACONST_NULL);
			} else {
				Class<?> tpye = (Class<?>) methodStatement.getReturnType();
				if (tpye.isPrimitive()) {
					BasicTypesOpcodes basicTypesOpcodes = ReflectUtils.getBasicTypesOpcodes(tpye);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, basicTypesOpcodes.getPackingAsmTypeName(), "valueOf", basicTypesOpcodes.getBasieTurnName(), false);
					if (tpye.equals(long.class) || tpye.equals(double.class)) {
						maxStack = maxStack + 1;
					}
				}
			}
			mv.visitInsn(Opcodes.ARETURN);
			mv.visitMaxs(maxStack, 2);
			mv.visitEnd();
		}
		cw.visitEnd();
		byte[] data = cw.toByteArray();
		return this.defineClass(invokerObjectName, data, 0, data.length);
	}
}
