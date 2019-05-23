package org.apache.dubbo.rpc.proxy.asm;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.proxy.asm.MethodStatement.ParameterSteaement;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ReflectUtils extends ClassLoader implements Opcodes {
	
	private final static String  HAVE_PARAMETER = "(Lorg/apache/dubbo/rpc/proxy/asm/MethodStatement;[Ljava/lang/Object;)Ljava/lang/Object;";
	
	private final static String  NOT_PARAMETER = "(Lorg/apache/dubbo/rpc/proxy/asm/MethodStatement;)Ljava/lang/Object;";

	private final static AtomicInteger CLASS_NAME_ATOMIC = new AtomicInteger();

	private final static Map<Class<?>, String[]> BEASE_TO_PACKAGING = new HashMap<>();

	private final static Map<Type, String> TYPE_TO_ASM_TYPE = new ConcurrentHashMap<>();

	static {
		BEASE_TO_PACKAGING.put(boolean.class, new String[] { "java/lang/Boolean", "(Z)Ljava/lang/Boolean;" });
		BEASE_TO_PACKAGING.put(char.class, new String[] { "java/lang/Character", "(C)Ljava/lang/Character;" });
		BEASE_TO_PACKAGING.put(byte.class, new String[] { "java/lang/Byte", "(B)Ljava/lang/Byte;" });
		BEASE_TO_PACKAGING.put(short.class, new String[] { "java/lang/Short", "(S)Ljava/lang/Short;" });
		BEASE_TO_PACKAGING.put(int.class, new String[] { "java/lang/Integer", "(I)Ljava/lang/Integer;" });
		BEASE_TO_PACKAGING.put(long.class, new String[] { "java/lang/Long", "(J)Ljava/lang/Long;" });
		BEASE_TO_PACKAGING.put(float.class, new String[] { "java/lang/Float", "(D)Ljava/lang/Float;" });
		BEASE_TO_PACKAGING.put(double.class, new String[] { "java/lang/Double", "(F)Ljava/lang/Double;" });

		TYPE_TO_ASM_TYPE.put(void.class, "V");
		TYPE_TO_ASM_TYPE.put(boolean.class, "Z");
		TYPE_TO_ASM_TYPE.put(char.class, "C");
		TYPE_TO_ASM_TYPE.put(byte.class, "B");
		TYPE_TO_ASM_TYPE.put(short.class, "S");
		TYPE_TO_ASM_TYPE.put(int.class, "I");
		TYPE_TO_ASM_TYPE.put(long.class, "J");
		TYPE_TO_ASM_TYPE.put(float.class, "D");
		TYPE_TO_ASM_TYPE.put(double.class, "F");

		TYPE_TO_ASM_TYPE.put(Boolean.class, "Ljava/lang/Boolean;");
		TYPE_TO_ASM_TYPE.put(Character.class, "Ljava/lang/Character;");
		TYPE_TO_ASM_TYPE.put(Byte.class, "Ljava/lang/Byte;");
		TYPE_TO_ASM_TYPE.put(Short.class, "Ljava/lang/Short;");
		TYPE_TO_ASM_TYPE.put(Integer.class, "Ljava/lang/Integer;");
		TYPE_TO_ASM_TYPE.put(Long.class, "Ljava/lang/Integer;");
		TYPE_TO_ASM_TYPE.put(Float.class, "Ljava/lang/Float;");
		TYPE_TO_ASM_TYPE.put(Double.class, "Ljava/lang/Double;");

		TYPE_TO_ASM_TYPE.put(List.class, "Ljava/util/List;");
		TYPE_TO_ASM_TYPE.put(Map.class, "Ljava/util/Map;");
	}

	@SuppressWarnings("unchecked")
	public <T> T getProxy(Class<?>[] types, Invoker<?> handler) {
		try {
			Class<?> clazz = getProxyClass(types);
			return (T) clazz.getConstructor(Invoker.class).newInstance(handler);
		} catch (Throwable e) {
			throw new RuntimeException(e);

		}
	}

	public Class<?> getProxyClass(Class<?>[] types) {
		String className = getProxyName(types);
		ClassWriter cw = new ClassWriter(0);
		cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, className, null, "org/apache/dubbo/rpc/proxy/asm/AbstractAsmProxy",
				getClassName(types));
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
			List<MethodStatement> msList = analysisMethod(clazz);
			for (MethodStatement ms : msList) {
				{
					cw.visitField(Opcodes.ACC_PRIVATE, ms.getMethod() + "_statement",
							"Lorg/apache/dubbo/rpc/proxy/asm/MethodStatement;", null, null).visitEnd();
				}
				MethodVisitor mw = cw.visitMethod(Opcodes.ACC_PUBLIC, ms.getMethod(),
						getMethodStatement(ms.getParameterTypes(), ms.getReturnType()), null, getClassName(ms.getAbnormalTypes()));
				List<ParameterSteaement> parameter = ms.getParameterTypes();
				mw.visitVarInsn(Opcodes.ALOAD, 0);
				mw.visitVarInsn(Opcodes.ALOAD, 0);
				mw.visitFieldInsn(Opcodes.GETFIELD, className, ms.getMethod() + "_statement",
						"Lorg/apache/dubbo/rpc/proxy/asm/MethodStatement;");
				int maxStack = 2,maxLocals = 1;
				boolean is64Type = true;
				String desc = NOT_PARAMETER;
				if (parameter != null && parameter.size() != 0) {
					desc = HAVE_PARAMETER;
					maxStack = 6;
					maxLocals = maxLocals + parameter.size();
					if (parameter.size() < 7) {
						mw.visitInsn(3 + parameter.size());
					} else {
						mw.visitIincInsn(Opcodes.BIPUSH, parameter.size() + 1);
					}
					mw.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
					mw.visitInsn(Opcodes.DUP);
					for (int i = 0; i < parameter.size(); i++) {
						if (parameter.size() < 7) {
							mw.visitInsn(3 + i);
						} else {
							mw.visitIincInsn(Opcodes.BIPUSH, i + 1);
						}
						Class<?> parameterClass = parameter.get(i).getClass();
						if (parameterClass.isPrimitive()) {// 判断是基本类型
							if(is64Type == true &&(parameterClass.equals(long.class) ||parameterClass.equals(double.class) )) {
								is64Type = false;
								maxStack = maxStack+1;
							}
							String[] amsStrArray = BEASE_TO_PACKAGING.get(parameter.get(i).getClass());
							mw.visitMethodInsn(Opcodes.INVOKESTATIC, amsStrArray[0], "valueOf", amsStrArray[1], false);
						} else {
							mw.visitVarInsn(Opcodes.ALOAD, 1);
						}
						mw.visitInsn(Opcodes.AASTORE);
					}
				}
				mw.visitMethodInsn(Opcodes.INVOKESPECIAL, "org/apache/dubbo/rpc/proxy/asm/AbstractAsmProxy", "invoke",desc,false);
				if(void.class.equals(ms.getReturnType())) {
					mw.visitInsn(Opcodes.RETURN);
				}else {
					mw.visitTypeInsn(Opcodes.CHECKCAST, getClassName(ms.getReturnType()));
					mw.visitInsn(Opcodes.ARETURN);
				}
				mw.visitMaxs( maxStack, maxLocals);
				mw.visitEnd();
			}
		}
		cw.visitEnd();
		byte[] data = cw.toByteArray();
		return this.defineClass(className, data, 0, data.length);
	}

	public static List<MethodStatement> analysisMethod(Class<?> type) {
		Method[] methods = type.getMethods();
		List<MethodStatement> msList = new ArrayList<MethodStatement>();
		for (Method method : methods) {
			msList.add(analysisMethod(method));
		}
		return msList;
	}

	public static MethodStatement analysisMethod(Method method) {
		MethodStatement ms = new MethodStatement();
		ms.setMethod(method.getName());
		ms.setAbnormalTypes(method.getExceptionTypes());
		ms.setReturnType(method.getReturnType());
		Type returnType = method.getGenericReturnType();// 获取返回值类型
		if (returnType instanceof ParameterizedType) { // 判断获取的类型是否是参数类型
			ms.setReturnGeneric(((ParameterizedType) returnType).getActualTypeArguments());
		}
		ms.setParameterTypes(analysisParameterized(method));
		return ms;
	}

	public static List<ParameterSteaement> analysisParameterized(Method method) {
		Type[] types = method.getGenericParameterTypes();// 获取参数，可能是多个，所以是数组
		List<ParameterSteaement> psList = new ArrayList<>(types.length);
		;
		for (Type type2 : types) {
			ParameterSteaement ps = new ParameterSteaement();
			psList.add(ps);
			ps.setType(type2);
			if (type2 instanceof ParameterizedType) {// 判断获取的类型是否是参数类型
				ps.setGenericTypes(((ParameterizedType) type2).getActualTypeArguments());
			}
		}
		return psList;
	}

	public static String[] getClassName(Type[] types) {
		if (types == null || types.length == 0) {
			return null;
		}
		String[] strArray = new String[types.length];
		int i = 0;
		for (Type type : types) {
			strArray[i++] = ((Class<?>) type).getName().replace('.', '/');
		}
		return strArray;
	}

	public static String getClassName(Type type) {
		return type == null ? null : ((Class<?>) type).getName().replace('.', '/');
	}

	public static String getProxyName(Class<?>[] types) {
		StringBuffer sb = new StringBuffer();
		for (Class<?> type : types) {
			String name = type.getName();
			sb.append(name.substring(name.lastIndexOf(".") + 1));
			sb.append('_');
		}
		sb.append(CLASS_NAME_ATOMIC.incrementAndGet());
		return sb.toString();
	}

	public static String getMethodStatement(List<ParameterSteaement> types, Type type) {
		StringBuffer sb = new StringBuffer();
		sb.append("(");
		if (types != null && types.size() != 0) {
			for (ParameterSteaement t : types) {
				sb.append(getStatementName( t.getType() ));
			}
		}
		sb.append(")");
		sb.append(getStatementName(type));
		return sb.toString();

	}

	public static String getStatementName(Type type) {
		String typeName = TYPE_TO_ASM_TYPE.get(type);
		if (typeName == null) {
			if(((Class<?>)type).isArray()) {
				typeName = getClassName(type);
			}else {
				typeName = "L" + getClassName(type) + ";";
			}
			TYPE_TO_ASM_TYPE.put(type, typeName);
		}
		return typeName;

	}
}
