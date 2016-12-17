package cn.wjdiankong.main;

import java.util.List;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import cn.wjdiankong.patch.ChangeQuickRedirect;
import cn.wjdiankong.robust.utils.PatchProxy;

public final class RobustAsmUtils {
	
	public final static String REDIRECTFIELD_NAME = "changeQuickRedirect";
	public final static String REDIRECTCLASSNAME = Type.getDescriptor(ChangeQuickRedirect.class);
	public final static String PROXYCLASSNAME = PatchProxy.class.getName().replace(".", "/");
	
	public static void addClassStaticField(ClassVisitor cv, String fieldName, Class<?> typeClass){
		cv.visitField(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC, fieldName,
				Type.getDescriptor(typeClass), null, null); 
	}
	
	public static void addClassStaticField(ClassVisitor cv, String fieldName, String typeClass){
		cv.visitField(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC, fieldName, typeClass, null, null); 
	}
	
	public static void setClassStaticFieldValue(MethodVisitor mv, String className, String fieldName, String typeSign){
		mv.visitFieldInsn(Opcodes.PUTSTATIC, className, fieldName, typeSign);
	}
	
	public static void getClassStaticFieldValue(MethodVisitor mv, String className, String fieldName, String typeSign){
		mv.visitFieldInsn(Opcodes.GETSTATIC, className, fieldName, typeSign);
	}
	
	/**
	 * 插入代码
	 * @param mv
	 * @param className
	 * @param paramsTypeClass
	 * @param returnTypeStr
	 * @param isStatic
	 */
	public static void createInsertCode(MethodVisitor mv, String className, List<String> paramsTypeClass, String returnTypeStr, boolean isStatic){
		//获取changeQuickRedirect静态变量
		mv.visitFieldInsn(Opcodes.GETSTATIC, 
				className, 
				REDIRECTFIELD_NAME, 
				REDIRECTCLASSNAME);
		Label l1 = new Label();
		mv.visitJumpInsn(Opcodes.IFNULL, l1);

		/**
		 * 调用isSupport方法
		 */
		//第一个参数：new Object[]{...};,如果方法没有参数直接传入new Object[0]
		if(paramsTypeClass.size() == 0){
			mv.visitInsn(Opcodes.ICONST_0);
			mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
		}else{
			createObjectArray(mv, paramsTypeClass, isStatic);
		}
		
		//第二个参数：this,如果方法是static的话就直接传入null
		if(isStatic){
			mv.visitInsn(Opcodes.ACONST_NULL);
		}else{
			mv.visitVarInsn(Opcodes.ALOAD, 0);
		}
		
		//第三个参数：changeQuickRedirect
		mv.visitFieldInsn(Opcodes.GETSTATIC, 
				className, 
				REDIRECTFIELD_NAME, 
				REDIRECTCLASSNAME);
		
		//第四个参数：false,标志是否为static
		mv.visitInsn(isStatic ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
		
		//开始调用
		mv.visitMethodInsn(Opcodes.GETSTATIC, 
				PROXYCLASSNAME, 
				"isSupport", 
				"([Ljava/lang/Object;Ljava/lang/Object;"+REDIRECTCLASSNAME+"Z)Z");
		mv.visitJumpInsn(Opcodes.IFEQ, l1);
		
		/**
		 * 调用accessDispatch方法
		 */
		//第一个参数：new Object[]{...};,如果方法没有参数直接传入new Object[0]
		if(paramsTypeClass.size() == 0){
			mv.visitInsn(Opcodes.ICONST_0);
			mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
		}else{
			createObjectArray(mv, paramsTypeClass, isStatic);
		}		
		
		//第二个参数：this,如果方法是static的话就直接传入null
		if(isStatic){
			mv.visitInsn(Opcodes.ACONST_NULL);
		}else{
			mv.visitVarInsn(Opcodes.ALOAD, 0);
		}		
		
		//第三个参数:changeQuickRedirect
		mv.visitFieldInsn(Opcodes.GETSTATIC,
				className, 
				REDIRECTFIELD_NAME, 
				REDIRECTCLASSNAME);
		//第四个参数：false,标志是否为static
		mv.visitInsn(isStatic ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
		
		//开始调用
		mv.visitMethodInsn(Opcodes.INVOKESTATIC,
				PROXYCLASSNAME, 
				"accessDispatch", 
				"([Ljava/lang/Object;Ljava/lang/Object;"+REDIRECTCLASSNAME+"Z)Ljava/lang/Object;");
		
		//判断是否有返回值，代码不同
		if("V".equals(returnTypeStr)){
			mv.visitInsn(Opcodes.POP);
			mv.visitInsn(Opcodes.RETURN);
		}else{
			//强制转化类型
			if(!castPrimateToObj(mv, returnTypeStr)){
				//这里需要注意，如果是数组类型的直接使用即可，如果非数组类型，就得去除前缀了,还有最终是没有结束符;
				//比如：Ljava/lang/String; ==》 java/lang/String
				String newTypeStr = null;
				int len = returnTypeStr.length();
				if(returnTypeStr.startsWith("[")){
					newTypeStr = returnTypeStr.substring(0, len);
				}else{
					newTypeStr = returnTypeStr.substring(1, len-1);
				}
				mv.visitTypeInsn(Opcodes.CHECKCAST, newTypeStr);
			}
			
			//这里还需要做返回类型不同返回指令也不同
			mv.visitInsn(getReturnTypeCode(returnTypeStr));
		}
		
		mv.visitLabel(l1);
	}
	
	/**
	 * 创建局部参数代码
	 * @param mv
	 * @param paramsTypeClass
	 * @param isStatic
	 */
	private static void createObjectArray(MethodVisitor mv, List<String> paramsTypeClass, boolean isStatic){
		//Opcodes.ICONST_0 ~ Opcodes.ICONST_5 这个指令范围
		int argsCount = paramsTypeClass.size();
		//声明 Object[argsCount];
		if(argsCount >= 6){
			mv.visitIntInsn(Opcodes.BIPUSH, argsCount);
		}else{
			mv.visitInsn(Opcodes.ICONST_0+argsCount);
		}
		mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
		
		//如果是static方法，没有this隐含参数
		int loadIndex = (isStatic ? 0 : 1);
		
		//填充数组数据
		for(int i=0;i<argsCount;i++){
			mv.visitInsn(Opcodes.DUP);
			if(i <= 5){
				mv.visitInsn(Opcodes.ICONST_0+i);
			}else{
				mv.visitIntInsn(Opcodes.BIPUSH, i);
			}
			
			//这里又要做特殊处理，在实践过程中发现个问题：public void xxx(long a, boolean b, double c,int d)
			//当一个参数的前面一个参数是long或者是double类型的话，后面参数在使用LOAD指令，加载数据索引值要+1
			//个人猜想是和long，double是8个字节的问题有关系。这里做了处理
			//比如这里的参数：[a=LLOAD 1] [b=ILOAD 3] [c=DLOAD 4] [d=ILOAD 6];
			if(i >= 1){
				//这里需要判断当前参数的前面一个参数的类型是什么
				if("J".equals(paramsTypeClass.get(i-1)) || "D".equals(paramsTypeClass.get(i-1))){
					//如果前面一个参数是long，double类型，load指令索引就要增加1
					loadIndex ++;
				}
			}
			if(!createPrimateTypeObj(mv, loadIndex, paramsTypeClass.get(i))){
				mv.visitVarInsn(Opcodes.ALOAD, loadIndex);
				mv.visitInsn(Opcodes.AASTORE);
			}
			loadIndex ++;
		}
	}
	
	private static void createBooleanObj(MethodVisitor mv, int argsPostion){
		mv.visitTypeInsn(Opcodes.NEW, "java/lang/Byte");
		mv.visitInsn(Opcodes.DUP);
		mv.visitVarInsn(Opcodes.ILOAD, argsPostion);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Byte", "<init>", "(B)V");
		mv.visitInsn(Opcodes.AASTORE);
	}
	
	private static void createShortObj(MethodVisitor mv, int argsPostion){
		mv.visitTypeInsn(Opcodes.NEW, "java/lang/Short");
		mv.visitInsn(Opcodes.DUP);
		mv.visitVarInsn(Opcodes.ILOAD, argsPostion);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Short", "<init>", "(S)V");
		mv.visitInsn(Opcodes.AASTORE);
	}
	
	private static void createCharObj(MethodVisitor mv, int argsPostion){
		mv.visitTypeInsn(Opcodes.NEW, "java/lang/Character");
		mv.visitInsn(Opcodes.DUP);
		mv.visitVarInsn(Opcodes.ILOAD, argsPostion);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Character", "<init>", "(C)V");
		mv.visitInsn(Opcodes.AASTORE);
	}
	
	private static void createIntegerObj(MethodVisitor mv, int argsPostion){
		mv.visitTypeInsn(Opcodes.NEW, "java/lang/Integer");
		mv.visitInsn(Opcodes.DUP);
		mv.visitVarInsn(Opcodes.ILOAD, argsPostion);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V");
		mv.visitInsn(Opcodes.AASTORE);
	}
	
	private static void createFloatObj(MethodVisitor mv, int argsPostion){
		mv.visitTypeInsn(Opcodes.NEW, "java/lang/Float");
		mv.visitInsn(Opcodes.DUP);
		mv.visitVarInsn(Opcodes.FLOAD, argsPostion);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Float", "<init>", "(F)V");
		mv.visitInsn(Opcodes.AASTORE);
	}
	
	private static void createDoubleObj(MethodVisitor mv, int argsPostion){
		mv.visitTypeInsn(Opcodes.NEW, "java/lang/Double");
		mv.visitInsn(Opcodes.DUP);
		mv.visitVarInsn(Opcodes.DLOAD, argsPostion);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Double", "<init>", "(D)V");
		mv.visitInsn(Opcodes.AASTORE);
	}
	
	private static void createLongObj(MethodVisitor mv, int argsPostion){
		mv.visitTypeInsn(Opcodes.NEW, "java/lang/Long");
		mv.visitInsn(Opcodes.DUP);
		mv.visitVarInsn(Opcodes.LLOAD, argsPostion);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Long", "<init>", "(J)V");
		mv.visitInsn(Opcodes.AASTORE);
	}
	
	/**
	 * 创建基本类型对应的对象
	 * @param mv
	 * @param argsPostion
	 * @param typeS
	 * @return
	 */
	private static boolean createPrimateTypeObj(MethodVisitor mv, int argsPostion, String typeS){
		if("Z".equals(typeS)){
			createBooleanObj(mv, argsPostion);
			return true;
		}
		if("B".equals(typeS)){
			createBooleanObj(mv, argsPostion);
			return true;
		}
		if("C".equals(typeS)){
			createCharObj(mv, argsPostion);
			return true;
		}
		if("S".equals(typeS)){
			createShortObj(mv, argsPostion);
			return true;
		}
		if("I".equals(typeS)){
			createIntegerObj(mv, argsPostion);
			return true;
		}
		if("F".equals(typeS)){
			createFloatObj(mv, argsPostion);
			return true;
		}
		if("D".equals(typeS)){
			createDoubleObj(mv, argsPostion);
			return true;
		}
		if("J".equals(typeS)){
			createLongObj(mv, argsPostion);
			return true;
		}
		return false;
	}
	
	/**
	 * 基本类型需要做对象类型分装
	 * @param mv
	 * @param typeS
	 * @return
	 */
	private static boolean castPrimateToObj(MethodVisitor mv, String typeS){
		if("Z".equals(typeS)){
			mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Boolean");//强制转化类型
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z");
			return true;
		}
		if("B".equals(typeS)){
			mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Byte");//强制转化类型
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B");
			return true;
		}
		if("C".equals(typeS)){
			mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Character");//强制转化类型
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "intValue", "()C");
			return true;
		}
		if("S".equals(typeS)){
			mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Short");//强制转化类型
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S");
			return true;
		}
		if("I".equals(typeS)){
			mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer");//强制转化类型
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I");
			return true;
		}
		if("F".equals(typeS)){
			mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Float");//强制转化类型
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F");
			return true;
		}
		if("D".equals(typeS)){
			mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Double");//强制转化类型
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D");
			return true;
		}
		if("J".equals(typeS)){
			mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Long");//强制转化类型
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J");
			return true;
		}
		return false;
	}
	
	/**
	 * 针对不同类型返回指令不一样
	 * @param typeS
	 * @return
	 */
	private static int getReturnTypeCode(String typeS){
		if("Z".equals(typeS)){
			return Opcodes.IRETURN;
		}
		if("B".equals(typeS)){
			return Opcodes.IRETURN;
		}
		if("C".equals(typeS)){
			return Opcodes.IRETURN;
		}
		if("S".equals(typeS)){
			return Opcodes.IRETURN;
		}
		if("I".equals(typeS)){
			return Opcodes.IRETURN;
		}
		if("F".equals(typeS)){
			return Opcodes.FRETURN;
		}
		if("D".equals(typeS)){
			return Opcodes.DRETURN;
		}
		if("J".equals(typeS)){
			return Opcodes.LRETURN;
		}
		return Opcodes.ARETURN;
	}
	
}
