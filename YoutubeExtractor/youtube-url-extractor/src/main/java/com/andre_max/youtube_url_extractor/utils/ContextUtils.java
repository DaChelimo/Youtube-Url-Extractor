package com.andre_max.youtube_url_extractor.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

public class ContextUtils 
{
	public static Context context;
	public static void init(Context c){
		context=c;
	}
	public static  void CopytoClip(String x){

        ((ClipboardManager)context.getSystemService(context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("clipboard", x));    

		Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show(); 





	}
	
}
