package cn.wjdiankong.main;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Utils {
	
	/** 
	 * 文件拷贝的方法 
	 */  
	public static boolean fileCopy(String src, String des) {  
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {  
			fis = new FileInputStream(src);
			fos = new FileOutputStream(des);
			int len = 0;
			byte[] buffer = new byte[10*1024];
			while((len=fis.read(buffer)) > 0){  
				fos.write(buffer, 0, len);
			}  
		} catch (Exception e) {  
			System.out.println("copy failed:"+e.toString());
			return false;
		}finally{  
			try {  
				if(fis!=null)  fis.close();  
				if(fos!=null)  fos.close();  
			} catch (Exception e) {  
				System.out.println("copy failed:"+e.toString());
				return false;
			} 
		}
		return true;
	}    
	
	public static boolean execCmd(String cmd, boolean isOutputLog){
		BufferedReader br = null;
		try {
			Process p = Runtime.getRuntime().exec(cmd);
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = null;
			while ((line = br.readLine()) != null) {
				if(isOutputLog)
					System.out.println(line);
			}
		} catch (Exception e) {
			System.out.println("cmd error:"+e.toString());
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return true;
	}

	public static void decompressZipFile(String zipPath, String targetPath) throws IOException  {      
		File file = new File(zipPath);    
		if (!file.isFile()) {    
			throw new FileNotFoundException("file not exist!");    
		}    
		if (targetPath == null || "".equals(targetPath)) {    
			targetPath = file.getParent();    
		}      
		ZipFile zipFile = new ZipFile(file);    
		Enumeration<? extends ZipEntry> files = zipFile.entries();    
		ZipEntry entry = null;    
		File outFile = null;    
		BufferedInputStream bin = null;    
		BufferedOutputStream bout = null;   
		ByteArrayOutputStream bos = null;
		while (files.hasMoreElements()) {    
			entry = files.nextElement();    
			outFile = new File(targetPath + File.separator + entry.getName());   
			// 如果条目为目录，则跳向下一个     
			if(entry.isDirectory()){  
				outFile.mkdirs();    
				continue;    
			}    
			// 创建目录    
			if (!outFile.getParentFile().exists()) {    
				outFile.getParentFile().mkdirs();    
			}    
			// 创建新文件    
			outFile.createNewFile();    
			// 如果不可写，则跳向下一个条目    
			if (!outFile.canWrite()) {    
				continue;    
			}    
			try {    
				bos = new ByteArrayOutputStream();
				bin = new BufferedInputStream(zipFile.getInputStream(entry));    
				bout = new BufferedOutputStream(new FileOutputStream(outFile));    
				byte[] buffer = new byte[1024];    
				int readCount = -1;    
				while ((readCount = bin.read(buffer)) != -1) {    
					bos.write(buffer, 0, readCount);    
				}    
				//过滤v4包中的数据
				if(entry.getName().startsWith("android/support/")){
					bout.write(bos.toByteArray());
				}else{
					byte[] maxByteAry = InsertCodeUtils.operateClassByteCode(bos.toByteArray());
					bout.write(maxByteAry);
					maxByteAry = null;
				}
			} finally {    
				try {    
					bin.close();    
					bout.close();   
					bos.close();
				} catch (Exception e) {}    
			}    
		}    
	} 

}
