package org.apache.dubbo.rpc.proxy.asm;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.dubbo.common.compiler.support.ClassUtils;
import org.apache.dubbo.rpc.proxy.asm.MethodStatement.ParameterSteaement;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes ;

public class ReflectUtils {

	private final static Map<Class<?> , String[]> clazz_aaa = new HashMap<>();
	
	static {
		clazz_aaa.put(boolean.class, new String[] {"java/lang/Boolean","(I)Ljava/lang/Boolean;"});
		clazz_aaa.put(char.class, new String[] {"java/lang/Character","(I)Ljava/lang/Character;"});
		clazz_aaa.put(byte.class, new String[] {"java/lang/Byte","(I)Ljava/lang/Byte;"});
		clazz_aaa.put(short.class, new String[] {"java/lang/Short","(I)Ljava/lang/Short;"});
		clazz_aaa.put(int.class, new String[] {"java/lang/Integer","(I)Ljava/lang/Integer;"});
		clazz_aaa.put(long.class, new String[] {"java/lang/Long","(L)Ljava/lang/Long;"});
		clazz_aaa.put(float.class, new String[] {"java/lang/Float","(I)Ljava/lang/Float;"});
		clazz_aaa.put(double.class, new String[] {"java/lang/Double","(I)Ljava/lang/Double;"});
	}
	
	
	public Object getProxy(Class<?>[] types) {
		ClassWriter cw = new ClassWriter( 0 ) ;
		cw.visit( Opcodes.V1_8 , Opcodes.ACC_PUBLIC , "" , null , "org/apache/dubbo/rpc/proxy/asm/AbstractAsmProxy" , null ) ;
		MethodVisitor mw = cw.visitMethod( Opcodes.ACC_PUBLIC , "<init>" , "(Lorg/apache/dubbo/rpc/Invoker;)V" , null , null ) ;
		mw.visitVarInsn( Opcodes.ALOAD , 0 ) ;
		mw.visitVarInsn( Opcodes.ALOAD , 1 ) ;
		mw.visitMethodInsn( Opcodes.INVOKESPECIAL , "org/apache/dubbo/rpc/proxy/asm/AbstractAsmProxy" , "<init>" ,
				"(Lorg/apache/dubbo/rpc/Invoker;)V" , false ) ;

		mw.visitInsn( Opcodes.RETURN ) ;
		mw.visitMaxs( 2 , 2 ) ;
		mw.visitEnd( ) ;
		for(Class<?> clazz : types) {
			List<MethodStatement> msList = analysisMethod(clazz);
			for(MethodStatement ms : msList) {
				
				FieldVisitor field = cw.visitField(Opcodes.ACC_PRIVATE, ms.getMethod()+"_statement", "org/apache/dubbo/rpc/proxy/asm/MethodStatement", null, ms);
				field.visitEnd();
				mw = cw.visitMethod( Opcodes.ACC_PUBLIC , ms.getMethod() , "(L" + className + ";)Ljava/lang/String;" , null , ms.getAbnormalTypes() ) ;
				ParameterSteaement[] parameter = ms.getParameterTypes();
				int maxStack = 1 ;
				int maxLocals = 2;
				mw.visitVarInsn( Opcodes.ALOAD , 0 ) ;
				if(parameter != null) {
					mw.visitIincInsn(Opcodes.BIPUSH, parameter.length+1);
					/*if(parameter.length < 7) {
						mw.visitInsn(3+parameter.length );
					}else {
						mw.visitIincInsn(Opcodes.BIPUSH, parameter.length+1);
					}*/
					mw.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
					mw.visitInsn(Opcodes.DUP);
					for(int i = 0 ; i < parameter.length ; i++) {
						if(parameter.length < 7) {
							mw.visitInsn( 3+i );
						}else {
							mw.visitIincInsn(Opcodes.BIPUSH, i+1);
						}
						
						if(parameter[i].getClass().isPrimitive()) {//判断是基本类型
							String[] amsStrArray = clazz_aaa.get(parameter[i].getClass());
							mw.visitMethodInsn( Opcodes.INVOKESTATIC , amsStrArray[0] , "valueOf" ,amsStrArray[1] , false ) ;
						}else {
							mw.visitVarInsn( Opcodes.ALOAD , i+1 ) ;
						}
						mw.visitInsn(Opcodes.DUP);
						mw.visitInsn(Opcodes.AASTORE);
					}
				}
				mw.visitFieldInsn(Opcodes.GETSTATIC, "org/apache/dubbo/rpc/proxy/asm/", ms.getMethod()+"_statement", "Lorg/apache/dubbo/rpc/proxy/asm/MethodStatement;");
				mw.visitMethodInsn( Opcodes.INVOKEVIRTUAL , "org/apache/dubbo/rpc/proxy/asm/AbstractAsmProxy" , "invoke" , "(Lorg/apache/dubbo/rpc/proxy/asm/MethodStatement;[Ljava/lang/Object;)Ljava/lang/Object" ,
						false ) ;
				mw.visitInsn( Opcodes.ARETURN  ) ;
				mw.visitMaxs( maxStack , maxLocals ) ;
				mw.visitEnd( ) ;
			}
		}
		return null;
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

		return ms;
	}

	public static List<ParameterSteaement> analysisParameterized(Method method) {
		Type[] types = method.getGenericParameterTypes();// 获取参数，可能是多个，所以是数组
		List<ParameterSteaement> psList = new ArrayList<>(types.length);
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

}
