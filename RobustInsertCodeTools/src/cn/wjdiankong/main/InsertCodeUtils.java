package cn.wjdiankong.main;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class InsertCodeUtils {

	public static byte[] operateClassByteCode(byte[] classByteAry) {
		// 使用全限定名，创建一个ClassReader对象
		ClassReader classReader = new ClassReader(classByteAry);
		// 构建一个ClassWriter对象，并设置让系统自动计算栈和本地变量大小
		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		ClassVisitor classAdapter = new IClassAdapter(classWriter, classReader.getClassName());
		classReader.accept(classAdapter, ClassReader.SKIP_DEBUG);
		return classWriter.toByteArray();
	}
	
	public static boolean operateClassByteCode(File classFile, File newClassFile){
		FileOutputStream fos = null;
		FileInputStream fis = null;
		ByteArrayOutputStream bos = null;
		try{
			fis = new FileInputStream(classFile);
			fos = new FileOutputStream(newClassFile);
			bos = new ByteArrayOutputStream();
			int len = -1;
			byte[] buffer = new byte[1024];
			while((len=fis.read(buffer)) != -1){
				bos.write(buffer, 0, len);
			}
			byte[] newAry = operateClassByteCode(bos.toByteArray());
			if(newAry == null){
				return false;
			}
			fos.write(newAry);
			newAry = null;
			return true;
		}catch(IOException e){
			System.out.println("add code to classfile error:"+e.toString());
		}finally{
			try{
				fos.close();
				bos.close();
				fis.close();
			}catch(Exception e){
			}
		}
		return false;
	}

	static class IClassAdapter extends ClassAdapter {
		
		private String className;

		public IClassAdapter(ClassVisitor cv, String className) {
			super(cv);
			RobustAsmUtils.addClassStaticField(cv, RobustAsmUtils.REDIRECTFIELD_NAME,
					RobustAsmUtils.REDIRECTCLASSNAME);
			this.className = className;
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, 
				String signature, String[] exceptions) {
			MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
			// 当是sayName方法是做对应的修改
			if (!name.equals("<clinit>") && !name.equals("<init>")) {
				boolean isStatic = false;
				if (Opcodes.ACC_STATIC == (access & Opcodes.ACC_STATIC)) {
					isStatic = true;
				}
				MethodVisitor newMv = new IMethodAdapter(mv, className, desc, isStatic);

				return newMv;
			} else {
				return mv;
			}
		}

		// 定义一个自己的方法访问类
		class IMethodAdapter extends MethodAdapter {

			private Type[] argsType;
			private Type returnType;
			private boolean isStatic;
			private String className;

			public IMethodAdapter(MethodVisitor mv, String className,String desc, boolean isStatic) {
				super(mv);
				this.argsType = Type.getArgumentTypes(desc);
				this.returnType = Type.getReturnType(desc);
				this.isStatic = isStatic;
				this.className = className;
			}

			// 在源方法前去修改方法内容,这部分的修改将加载源方法的字节码之前
			@Override
			public void visitCode() {
				List<String> argsList = new ArrayList<String>(argsType.length);
				for (Type type : argsType) {
					argsList.add(type.getDescriptor());
				}
				RobustAsmUtils.createInsertCode(mv, className.replace(".", "/"), argsList,
						returnType.getDescriptor(), isStatic);
			}
		}
	}

}
