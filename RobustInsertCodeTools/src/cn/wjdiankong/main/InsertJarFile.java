package cn.wjdiankong.main;

import java.io.File;

/**
 * 可以操作jar文件
 * @author jiangwei1-g
 *
 */
public class InsertJarFile {
	
	public static void main(String[] args){
		
		if(args.length <= 0){
			System.out.println("please input jarfile!");
			return;
		}
		
		File jarFile = new File(new File(args[0]).getAbsolutePath());
		if(!jarFile.exists()){
			System.out.println("jarFile "+jarFile.getAbsolutePath()+" not exist!!");
			return;
		}
		
		File unZipDir = new File(jarFile.getParentFile().getAbsolutePath()+File.separator+"unzip"+File.separator);
		if(!unZipDir.exists()){
			unZipDir.mkdirs();
		}
		
		try {
			Utils.decompressZipFile(jarFile.getAbsolutePath(), unZipDir.getAbsolutePath());
			File batFile = new File("jarbattools.bat");
			String cmd = batFile.getAbsolutePath() + " " + unZipDir.getAbsolutePath();
			Utils.execCmd(cmd, false);
			
			Utils.fileCopy(unZipDir+File.separator+"classes.jar", jarFile.getAbsolutePath()+".jar");
			
		} catch (Exception e) {
			System.out.println("operate error:"+e.toString());
		}
		
	}

}
