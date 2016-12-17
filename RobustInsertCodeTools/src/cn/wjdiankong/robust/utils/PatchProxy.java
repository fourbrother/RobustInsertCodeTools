package cn.wjdiankong.robust.utils;

import cn.wjdiankong.patch.ChangeQuickRedirect;

public class PatchProxy {
	
	public static boolean isSupport(Object obj, Object thisObj, ChangeQuickRedirect redirect, boolean isFlag){
		return redirect.isSupport("", new Object[]{});
		
	}
	
	public static Object accessDispatch(Object obj, Object thisObj, ChangeQuickRedirect redirect, boolean isFlag){
		return redirect.accessDispatch("", new String[]{});
	}
	
}
