package cn.wjdiankong.main;

import java.io.File;

import cn.wjdiankong.example.Person;

/**
 * 可以操作单个class文件
 * @author jiangwei1-g
 *
 */
public class InsertClassFile {  
	
	private final static boolean isDebug = true;
	
	public static void main(String[] args) throws Exception{ 
		
		//测试Person.class专用
		if(isDebug){
			args = new String[]{Person.class.getResource("Person.class").getFile()};
		}
		
		if(args.length <=0 ){
			System.out.println("please input classfile!");
			return;
		}
		
		File srcClassFile = new File(new File(args[0]).getAbsolutePath());
		if(!srcClassFile.exists()){
			System.out.println("classfile:"+srcClassFile.getName()+" is not exist!!");
			return;
		}
		
		String tempClassFileStr = srcClassFile.getParentFile().getAbsolutePath() + File.separator + srcClassFile.getName()+"_tmp";
		if(isDebug){
			tempClassFileStr = "E:\\Person1.class";
		}
		File tempClassFile = new File(tempClassFileStr);
		if(!tempClassFile.exists()){
			tempClassFile.createNewFile();
		}
		
		boolean isSucc = InsertCodeUtils.operateClassByteCode(srcClassFile, tempClassFile);
		
		if(isDebug){
			return;
		}
		
		if(!isSucc){
			if(tempClassFile.exists()){
				tempClassFile.delete();
			}
			System.out.println("insert code fail,classname:"+srcClassFile.getName());
			return;
		}
		
		try{
			srcClassFile.delete();
			tempClassFile.renameTo(srcClassFile);
		}catch(Exception e){
		}
		
		System.out.println("insert code succ,classname:"+srcClassFile.getName());
		
	}  
	
}  
