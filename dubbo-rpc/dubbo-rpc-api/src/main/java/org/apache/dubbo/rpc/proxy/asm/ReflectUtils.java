package org.apache.dubbo.rpc.proxy.asm;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.proxy.asm.MethodStatement.ParameterSteaement;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ReflectUtils extends ClassLoader implements Opcodes {

	private final static String HAVE_PARAMETER = "(Lorg/apache/dubbo/rpc/proxy/asm/MethodStatement;[Ljava/lang/Object;)Ljava/lang/Object;";

	private final static String NOT_PARAMETER = "(Lorg/apache/dubbo/rpc/proxy/asm/MethodStatement;)Ljava/lang/Object;";

	private final static AtomicInteger CLASS_NAME_ATOMIC = new AtomicInteger();

	private final static Map<Class<?>, BasicTypesOpcodes> BASIC_TYPES_OPCODES_MAP = new ConcurrentHashMap<>();
	
	private final static Map<Class<?>, String[]> BEASE_TO_PACKAGING = new HashMap<>();

	private final static Map<Type, String> TYPE_TO_ASM_TYPE = new ConcurrentHashMap<>();

	private final static Map<Type, Integer> TYPE_TO_RETURN = new ConcurrentHashMap<>();

	private final static Map<Type, Integer> TYPE_TO_CONST = new ConcurrentHashMap<>();

	private final static Map<Type, Integer> TYPE_TO_LOAD = new ConcurrentHashMap<>();

	static {
		BEASE_TO_PACKAGING.put(boolean.class,
				new String[] { "java/lang/Boolean", "(Z)Ljava/lang/Boolean;", "booleanValue", "()Z" });	
		BEASE_TO_PACKAGING.put(char.class,
				new String[] { "java/lang/Character", "(C)Ljava/lang/Character;", "charValue", "()C" });	
		BEASE_TO_PACKAGING.put(byte.class,
				new String[] { "java/lang/Byte", "(B)Ljava/lang/Byte;", "byteValue", "()B" });
		BEASE_TO_PACKAGING.put(short.class,
				new String[] { "java/lang/Short", "(S)Ljava/lang/Short;", "shortValue", "()S" });		
		BEASE_TO_PACKAGING.put(int.class,
				new String[] { "java/lang/Integer", "(I)Ljava/lang/Integer;", "intValue", "()I" });
		BEASE_TO_PACKAGING.put(long.class,
				new String[] { "java/lang/Long", "(J)Ljava/lang/Long;", "longValue", "()J" });		
		BEASE_TO_PACKAGING.put(float.class,
				new String[] { "java/lang/Float", "(D)Ljava/lang/Float;", "floatValue", "()D" });
		BEASE_TO_PACKAGING.put(double.class,
				new String[] { "java/lang/Double", "(F)Ljava/lang/Double;", "dubbleValue", "()F" });
		
		TYPE_TO_ASM_TYPE.put(void.class, "V");
		TYPE_TO_ASM_TYPE.put(boolean.class, "Z");
		TYPE_TO_ASM_TYPE.put(char.class, "C");
		TYPE_TO_ASM_TYPE.put(byte.class, "B");
		TYPE_TO_ASM_TYPE.put(short.class, "S");
		TYPE_TO_ASM_TYPE.put(int.class, "I");
		TYPE_TO_ASM_TYPE.put(long.class, "J");
		TYPE_TO_ASM_TYPE.put(float.class, "F");
		TYPE_TO_ASM_TYPE.put(double.class, "D");

		TYPE_TO_ASM_TYPE.put(Boolean.class, "Ljava/lang/Boolean;");
		TYPE_TO_ASM_TYPE.put(Character.class, "Ljava/lang/Character;");
		TYPE_TO_ASM_TYPE.put(Byte.class, "Ljava/lang/Byte;");
		TYPE_TO_ASM_TYPE.put(Short.class, "Ljava/lang/Short;");
		TYPE_TO_ASM_TYPE.put(Integer.class, "Ljava/lang/Integer;");
		TYPE_TO_ASM_TYPE.put(Long.class, "Ljava/lang/Long;");
		TYPE_TO_ASM_TYPE.put(Float.class, "Ljava/lang/Float;");
		TYPE_TO_ASM_TYPE.put(Double.class, "Ljava/lang/Double;");

		TYPE_TO_ASM_TYPE.put(List.class, "Ljava/util/List;");
		TYPE_TO_ASM_TYPE.put(Map.class, "Ljava/util/Map;");

		TYPE_TO_RETURN.put(void.class,   Opcodes.RETURN);
		TYPE_TO_RETURN.put(boolean.class, Opcodes.IRETURN);
		TYPE_TO_RETURN.put(char.class,   Opcodes.IRETURN);
		TYPE_TO_RETURN.put(byte.class,   Opcodes.IRETURN);
		TYPE_TO_RETURN.put(short.class,  Opcodes.IRETURN);
		TYPE_TO_RETURN.put(int.class,    Opcodes.IRETURN);
		TYPE_TO_RETURN.put(long.class,   Opcodes.LRETURN);
		TYPE_TO_RETURN.put(float.class,  Opcodes.FRETURN);
		TYPE_TO_RETURN.put(double.class, Opcodes.DRETURN);

		TYPE_TO_CONST.put(boolean.class, Opcodes.ICONST_0);
		TYPE_TO_CONST.put(char.class, Opcodes.ICONST_0);
		TYPE_TO_CONST.put(byte.class, Opcodes.ICONST_0);
		TYPE_TO_CONST.put(short.class, Opcodes.ICONST_0);
		TYPE_TO_CONST.put(int.class, Opcodes.ICONST_0);
		TYPE_TO_CONST.put(long.class, Opcodes.LCONST_0);
		TYPE_TO_CONST.put(float.class, Opcodes.FCONST_0);
		TYPE_TO_CONST.put(double.class, Opcodes.DCONST_0);

		TYPE_TO_LOAD.put(boolean.class, Opcodes.ILOAD);
		TYPE_TO_LOAD.put(char.class, Opcodes.ILOAD);
		TYPE_TO_LOAD.put(byte.class, Opcodes.ILOAD);
		TYPE_TO_LOAD.put(short.class, Opcodes.ILOAD);
		TYPE_TO_LOAD.put(int.class, Opcodes.ILOAD);
		TYPE_TO_LOAD.put(long.class, Opcodes.LLOAD);
		TYPE_TO_LOAD.put(float.class, Opcodes.FLOAD);
		TYPE_TO_LOAD.put(double.class, Opcodes.DLOAD);
		
		
		BasicTypesOpcodes basicTypesOpcodes = new BasicTypesOpcodes();
		basicTypesOpcodes.setBasieAsmTypeName("V");
		basicTypesOpcodes.setReturnValue(Opcodes.RETURN);
		BASIC_TYPES_OPCODES_MAP.put(void.class, basicTypesOpcodes);
		
		basicTypesOpcodes = new BasicTypesOpcodes();
		basicTypesOpcodes.setPackingAsmTypeName("java/lang/Boolean");
		basicTypesOpcodes.setBasieTurnName("(Z)Ljava/lang/Boolean;");
		basicTypesOpcodes.setToBasieMethomName("booleanValue");
		basicTypesOpcodes.setBasieStatement("()Z");
		basicTypesOpcodes.setBasieAsmTypeName("Z");
		basicTypesOpcodes.setReturnValue(Opcodes.IRETURN);
		basicTypesOpcodes.setConstValue(Opcodes.ICONST_0);
		basicTypesOpcodes.setLoadValue(Opcodes.ILOAD);
		BASIC_TYPES_OPCODES_MAP.put(boolean.class, basicTypesOpcodes);
		
		basicTypesOpcodes = new BasicTypesOpcodes();
		basicTypesOpcodes.setPackingAsmTypeName("java/lang/Character");
		basicTypesOpcodes.setBasieTurnName("(C)Ljava/lang/Character;");
		basicTypesOpcodes.setToBasieMethomName("charValue");
		basicTypesOpcodes.setBasieStatement("()C");
		basicTypesOpcodes.setBasieAsmTypeName("C");
		basicTypesOpcodes.setReturnValue(Opcodes.IRETURN);
		basicTypesOpcodes.setConstValue(Opcodes.ICONST_0);
		basicTypesOpcodes.setLoadValue(Opcodes.ILOAD);
		BASIC_TYPES_OPCODES_MAP.put(char.class, basicTypesOpcodes);
		
		basicTypesOpcodes = new BasicTypesOpcodes();
		basicTypesOpcodes.setPackingAsmTypeName("java/lang/Byte");
		basicTypesOpcodes.setBasieTurnName("(B)Ljava/lang/Byte;");
		basicTypesOpcodes.setToBasieMethomName("byteValue");
		basicTypesOpcodes.setBasieStatement("()B");
		basicTypesOpcodes.setBasieAsmTypeName("B");
		basicTypesOpcodes.setReturnValue(Opcodes.IRETURN);
		basicTypesOpcodes.setConstValue(Opcodes.ICONST_0);
		basicTypesOpcodes.setLoadValue(Opcodes.ILOAD);
		BASIC_TYPES_OPCODES_MAP.put(byte.class, basicTypesOpcodes);
		
		basicTypesOpcodes = new BasicTypesOpcodes();
		basicTypesOpcodes.setPackingAsmTypeName("java/lang/Short");
		basicTypesOpcodes.setBasieTurnName("(S)Ljava/lang/Short;");
		basicTypesOpcodes.setToBasieMethomName("shortValue");
		basicTypesOpcodes.setBasieStatement("()S");
		basicTypesOpcodes.setBasieAsmTypeName("S");
		basicTypesOpcodes.setReturnValue(Opcodes.IRETURN);
		basicTypesOpcodes.setConstValue(Opcodes.ICONST_0);
		basicTypesOpcodes.setLoadValue(Opcodes.ILOAD);
		BASIC_TYPES_OPCODES_MAP.put(short.class, basicTypesOpcodes);
		
		basicTypesOpcodes = new BasicTypesOpcodes();
		basicTypesOpcodes.setPackingAsmTypeName("java/lang/Integer");
		basicTypesOpcodes.setBasieTurnName("(I)Ljava/lang/Integer;");
		basicTypesOpcodes.setToBasieMethomName("intValue");
		basicTypesOpcodes.setBasieStatement("()I");
		basicTypesOpcodes.setBasieAsmTypeName("I");
		basicTypesOpcodes.setReturnValue(Opcodes.IRETURN);
		basicTypesOpcodes.setConstValue(Opcodes.ICONST_0);
		basicTypesOpcodes.setLoadValue(Opcodes.ILOAD);
		BASIC_TYPES_OPCODES_MAP.put(int.class, basicTypesOpcodes);
		
		basicTypesOpcodes = new BasicTypesOpcodes();
		basicTypesOpcodes.setPackingAsmTypeName("java/lang/Long");
		basicTypesOpcodes.setBasieTurnName("(J)Ljava/lang/Long;");
		basicTypesOpcodes.setToBasieMethomName("longValue");
		basicTypesOpcodes.setBasieStatement("()J");
		basicTypesOpcodes.setBasieAsmTypeName("J");
		basicTypesOpcodes.setReturnValue(Opcodes.LRETURN);
		basicTypesOpcodes.setConstValue(Opcodes.LCONST_0);
		basicTypesOpcodes.setLoadValue(Opcodes.LLOAD);
		BASIC_TYPES_OPCODES_MAP.put(long.class, basicTypesOpcodes);
		
		basicTypesOpcodes = new BasicTypesOpcodes();
		basicTypesOpcodes.setPackingAsmTypeName("java/lang/Float");
		basicTypesOpcodes.setBasieTurnName("(F)Ljava/lang/Float;");
		basicTypesOpcodes.setToBasieMethomName("floatValue");
		basicTypesOpcodes.setBasieStatement("()F");
		basicTypesOpcodes.setBasieAsmTypeName("F");
		basicTypesOpcodes.setReturnValue(Opcodes.FRETURN);
		basicTypesOpcodes.setConstValue(Opcodes.FCONST_0);
		basicTypesOpcodes.setLoadValue(Opcodes.FLOAD);
		BASIC_TYPES_OPCODES_MAP.put(float.class, basicTypesOpcodes);
		
		basicTypesOpcodes = new BasicTypesOpcodes();
		basicTypesOpcodes.setPackingAsmTypeName("java/lang/Double");
		basicTypesOpcodes.setBasieTurnName("(D)Ljava/lang/Double;");
		basicTypesOpcodes.setToBasieMethomName("doubleValue");
		basicTypesOpcodes.setBasieStatement("()D");
		basicTypesOpcodes.setBasieAsmTypeName("D");
		basicTypesOpcodes.setReturnValue(Opcodes.DRETURN);
		basicTypesOpcodes.setConstValue(Opcodes.DCONST_0);
		basicTypesOpcodes.setLoadValue(Opcodes.DLOAD);
		BASIC_TYPES_OPCODES_MAP.put(double.class, basicTypesOpcodes);
	}
	
	static class BasicTypesOpcodes{
		private String packingAsmTypeName;
		
		private String basieTurnName;
		
		private String toBasieMethomName;
		
		private String basieStatement;
		
		private String basieAsmTypeName;
		
		private String parameterTypeName;
		
		private Integer returnValue;
		
		private Integer constValue;
		
		private Integer loadValue;

		public String getPackingAsmTypeName() {
			return packingAsmTypeName;
		}

		public void setPackingAsmTypeName(String packingAsmTypeName) {
			this.packingAsmTypeName = packingAsmTypeName;
		}

		public String getBasieTurnName() {
			return basieTurnName;
		}

		public void setBasieTurnName(String basieTurnName) {
			this.basieTurnName = basieTurnName;
		}

		public String getToBasieMethomName() {
			return toBasieMethomName;
		}

		public void setToBasieMethomName(String toBasieMethomName) {
			this.toBasieMethomName = toBasieMethomName;
		}

		public String getBasieStatement() {
			return basieStatement;
		}

		public void setBasieStatement(String basieStatement) {
			this.basieStatement = basieStatement;
		}

		public String getBasieAsmTypeName() {
			return basieAsmTypeName;
		}

		public void setBasieAsmTypeName(String basieAsmTypeName) {
			this.basieAsmTypeName = basieAsmTypeName;
		}

		public String getParameterTypeName() {
			return parameterTypeName;
		}

		public void setParameterTypeName(String parameterTypeName) {
			this.parameterTypeName = parameterTypeName;
		}

		public Integer getReturnValue() {
			return returnValue;
		}

		public void setReturnValue(Integer returnValue) {
			this.returnValue = returnValue;
		}

		public Integer getConstValue() {
			return constValue;
		}

		public void setConstValue(Integer constValue) {
			this.constValue = constValue;
		}

		public Integer getLoadValue() {
			return loadValue;
		}

		public void setLoadValue(Integer loadValue) {
			this.loadValue = loadValue;
		}
	}
	
	
	static BasicTypesOpcodes getBasicTypesOpcodes(Class<?> clazz) {
		return BASIC_TYPES_OPCODES_MAP.get(clazz);
	}
	
	private Map<String/*alias*/ , MethodStatement>  aliasAndMethodStatement = new HashMap<String, MethodStatement>();
	
	public MethodStatement getMethodStatement(String alias) {
		return aliasAndMethodStatement.get(alias);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getProxy(Class<?>[] types, Invoker<?> handler) throws Exception {
		Class<?> clazz = getProxyClass(types);
		Object object = clazz.getConstructor(Invoker.class).newInstance(handler);
		setField(object);
		return (T) object;
	}

	private void setField(Object object) throws IllegalArgumentException, IllegalAccessException {
		Class<?> clazz = object.getClass();
		Field[] fields = clazz.getDeclaredFields();
		for(Field field : fields) {
			field.setAccessible(true);
			field.set(object, aliasAndMethodStatement.get(field.getName()));
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
				String statement = getMethodStatement(ms.getParameterTypes(), ms.getReturnType());;
				aliasAndMethodStatement.put(ms.getAlias(), ms);
				{
					cw.visitField(Opcodes.ACC_PRIVATE, ms.getAlias(),"Lorg/apache/dubbo/rpc/proxy/asm/MethodStatement;", null, null).visitEnd();
				}
				MethodVisitor mw = cw.visitMethod(Opcodes.ACC_PUBLIC, ms.getMethod(), statement, null,
						getClassName(ms.getAbnormalTypes()));
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
							mw.visitVarInsn(TYPE_TO_LOAD.get(parameterClass), loadIndex++);
							String[] amsStrArray = BEASE_TO_PACKAGING.get(parameterClass);
							mw.visitMethodInsn(Opcodes.INVOKESTATIC, amsStrArray[0], "valueOf", amsStrArray[1], false);
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
					mw.visitInsn(POP);
					mw.visitInsn(Opcodes.RETURN);
				} else {
					Class<?> type = (Class<?>) ms.getReturnType();
					if (type.isPrimitive()) {
						String[] amsStrArray = BEASE_TO_PACKAGING.get(type);
						mw.visitTypeInsn(CHECKCAST, amsStrArray[0]);
						mw.visitVarInsn(ASTORE, 1);
						mw.visitVarInsn(ALOAD, maxLocals);
						Label l2 = new Label();
						mw.visitJumpInsn(IFNULL, l2);
						mw.visitVarInsn(ALOAD, maxLocals);
						mw.visitMethodInsn(INVOKEVIRTUAL, amsStrArray[0], amsStrArray[2], amsStrArray[3], false);
						mw.visitInsn(TYPE_TO_RETURN.get(type));
						mw.visitLabel(l2);
						mw.visitFrame(Opcodes.F_APPEND, 1, new Object[] { amsStrArray[0] }, 0, null);
						mw.visitInsn(TYPE_TO_CONST.get(type));
						mw.visitInsn(TYPE_TO_RETURN.get(type));
						maxLocals++;
					} else {
						mw.visitTypeInsn(CHECKCAST, getClassName(type));
						mw.visitInsn(ARETURN);
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

	public Map<String, MethodExecute<?>> getInvoke(Object proxy, Class<?> type)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {
		Map<String, MethodExecute<?>> map = new HashMap<>();
		List<MethodStatement> methodStatementList = analysisMethod(type);
		String invokerClassName = getClassName(type);
		for (MethodStatement methodStatement : methodStatementList) {
			Class<?> clazz = doGetInvoke(methodStatement, invokerClassName);
			map.put(methodStatement.getAlias(),
					(MethodExecute<?>) (clazz.getConstructor(type).newInstance(proxy)));
		}

		return map;
	}

	public Class<?> doGetInvoke(MethodStatement methodStatement, String invokerClassName) {
		ClassWriter cw = new ClassWriter(0);
		MethodVisitor mv;
		String invokerObjectName = methodStatement.getAlias() + "MethodExecute";
		cw.visit(V1_8, ACC_PUBLIC, invokerObjectName,
				"Lorg/apache/dubbo/rpc/proxy/asm/AbstractMethodExecute<L" + invokerClassName + ";>;",
				"org/apache/dubbo/rpc/proxy/asm/AbstractMethodExecute", null);

		{
			mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(L" + invokerClassName + ";)V", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKESPECIAL, "org/apache/dubbo/rpc/proxy/asm/AbstractMethodExecute", "<init>",
					"(Ljava/lang/Object;)V", false);
			mv.visitInsn(RETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC, "execute", "([Ljava/lang/Object;)Ljava/lang/Object;",
					"<T:Ljava/lang/Object;>([Ljava/lang/Object;)TT;", new String[] { "java/lang/Throwable" });
			mv.visitVarInsn(ALOAD, 0);
			//mv.visitFieldInsn(GETFIELD, invokerObjectName, "object", "Ljava/lang/Object;");
			mv.visitMethodInsn(INVOKEVIRTUAL, invokerObjectName, "getObject", "()Ljava/lang/Object;", false);
			mv.visitTypeInsn(CHECKCAST, invokerClassName);
			int maxStack = 1;
			List<ParameterSteaement> parameterList = methodStatement.getParameterTypes();
			if (parameterList != null && !parameterList.isEmpty()) {
				maxStack = maxStack + 1 + parameterList.size();
				for (int i = 0; i < parameterList.size(); i++) {
					ParameterSteaement parameter = parameterList.get(i);
					mv.visitVarInsn(ALOAD, 1);
					if (i < 7) {
						mv.visitInsn(3 + i);
					} else {
						mv.visitIincInsn(Opcodes.BIPUSH, i + 1);
					}
					mv.visitInsn(AALOAD);
					Class<?> tpye = (Class<?>) parameter.getType();
					if (tpye.isPrimitive()) {
						maxStack = maxStack + 1;
						String[] amsStrArray = BEASE_TO_PACKAGING.get(tpye);
						mv.visitTypeInsn(CHECKCAST, amsStrArray[0]);
						mv.visitMethodInsn(INVOKEVIRTUAL, amsStrArray[0], amsStrArray[2], amsStrArray[3], false);
					} else {
						mv.visitTypeInsn(CHECKCAST, getClassName(parameter.getType()));
					}
				}
			}

			mv.visitMethodInsn(INVOKEVIRTUAL, invokerClassName, methodStatement.getMethod(),
					getMethodStatement(parameterList, methodStatement.getReturnType()), false);
			if (void.class.equals(methodStatement.getReturnType())) {
				mv.visitInsn(ACONST_NULL);
			} else {
				Class<?> tpye = (Class<?>) methodStatement.getReturnType();
				if (tpye.isPrimitive()) {
					String[] amsStrArray = BEASE_TO_PACKAGING.get(tpye);
					mv.visitMethodInsn(INVOKESTATIC, amsStrArray[0], "valueOf", amsStrArray[1], false);
					if (tpye.equals(long.class) || tpye.equals(double.class)) {
						maxStack = maxStack + 1;
					}
				}
			}
			mv.visitInsn(ARETURN);
			mv.visitMaxs(maxStack, 2);
			mv.visitEnd();
		}
		cw.visitEnd();
		byte[] data = cw.toByteArray();
		return this.defineClass(invokerObjectName, data, 0, data.length);
	}

	public static List<MethodStatement> analysisMethod(Class<?> type) {
		boolean boo = type.isInterface();
		Method[] methods = type.getDeclaredMethods();
		List<MethodStatement> msList = new ArrayList<MethodStatement>();
		for (Method method : methods) {
			if (boo || method.getModifiers() == 1) {
				MethodStatement ms = analysisMethod(method);
				msList.add(ms);
				ms.setAlias(ms.getMethod() + "_" + getAlias(ms.getParameterTypes()));
			}
		}
		return msList;
	}

	public static MethodStatement analysisMethod(Method method) {
		MethodStatement ms = new MethodStatement();
		ms.setMethod(method.getName());
		ms.setAbnormalTypes(method.getExceptionTypes());
		ms.setReturnType(method.getReturnType());
		ms.setFutureReturnType(CompletableFuture.class.isAssignableFrom( method.getReturnType() ));
		Type returnType = method.getGenericReturnType();// 获取返回值类型
		if (returnType instanceof ParameterizedType) { // 判断获取的类型是否是参数类型
			ms.setReturnGeneric(((ParameterizedType) returnType).getActualTypeArguments());
		}
		ms.setParameterTypes(analysisParameterized(method));
		return ms;
	}

	public static List<ParameterSteaement> analysisParameterized(Method method) {
		Type[] types = method.getParameterTypes();// method.getGenericParameterTypes();// 获取参数，可能是多个，所以是数组
		method.getGenericParameterTypes();
		method.getParameters();
		List<ParameterSteaement> psList = new ArrayList<>(types.length);
		for (Type type2 : types) {
			ParameterSteaement ps = new ParameterSteaement();
			psList.add(ps);
			ps.setType((Class<?>)type2);
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
				sb.append(getStatementName(t.getType()));
			}
		}
		sb.append(")");
		sb.append(getStatementName(type));
		return sb.toString();

	}

	public static String getStatementName(Type type) {
		String typeName = TYPE_TO_ASM_TYPE.get(type);
		if (typeName == null) {
			if (type instanceof Class) {
				if (((Class<?>) type).isArray()) {
					typeName = getClassName(type);
				} else {
					typeName = "L" + getClassName(type) + ";";
				}
				TYPE_TO_ASM_TYPE.put(type, typeName);
			}
		}
		return typeName;
	}
	
	public static String getAlias(List<ParameterSteaement> types) {
		StringBuffer sb = new StringBuffer();
		if (types != null && types.size() != 0) {
			for (ParameterSteaement t : types) {
				String className = ((Class<?>) t.getType()).getName();
				sb.append(className.replace("[", "_").replace(';', '_').replace('.','_'));
			}
		}
		return sb.toString();
	}
	
	public static String getAlias(String methodName, Class<?>[] parameterTypes) {
		StringBuffer sb = new StringBuffer();
		sb.append(methodName).append('_');
		if (parameterTypes != null && parameterTypes.length != 0) {
			for (Class<?> clazz : parameterTypes) {
				sb.append(clazz.getName().replace("[", "_").replace(';', '_').replace('.','_'));
			}
		}
		return sb.toString();
	}
}
