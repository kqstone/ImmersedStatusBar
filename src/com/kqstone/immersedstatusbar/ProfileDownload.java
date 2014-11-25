package com.kqstone.immersedstatusbar;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.once.ZipUtils;

import android.os.Environment;

public class ProfileDownload {
	public final static String sDir = "/isb/";
	private static String sTempPath;
	private static String sTempExtPath; 
	private static String sTempProfilePath;
	private static String sTempImgPath;
	private static String sTempFile;
	private static String sDestProfilePath;
	private static String sDestImgPath;
	
	static {
		File StorageDir = Environment.getExternalStorageDirectory();
		sTempPath = StorageDir + sDir + "temp/";
		sTempExtPath = StorageDir + sDir + "tempext/";
		sTempProfilePath = sTempExtPath + "ISBpreferences-master/profile/"; 
		sTempImgPath = sTempExtPath + "ISBpreferences-master/img/";
		sTempFile = sTempPath + "temp.zip";
		sDestProfilePath = StorageDir + sDir + "profile/";
		sDestImgPath = StorageDir + sDir + "img/";
	}
	
	public static boolean downloadZip() {
		String urlStr = Constant.PROFILE_URL;
		OutputStream output = null;
		try {
//			URL url = new URL(urlStr);
//			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//			conn.connect();
			HttpGet httpRequest = new HttpGet(urlStr); 
			HttpClient httpclient = new DefaultHttpClient(); 
			HttpResponse httpResponse = httpclient.execute(httpRequest);  
			
			if(httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			
			String dirName = sTempPath;
			File dir = new File(dirName);
			if (!dir.exists())
				dir.mkdirs();
			String fileName = sTempFile;// 文件存储路径

			File file = new File(fileName);
			if (file.exists())
				file.delete();
			HttpEntity httpEntity = httpResponse.getEntity();
			InputStream input = new BufferedInputStream(httpEntity.getContent());
//			InputStream input = conn.getInputStream();
			if (file.exists())
				file.delete();
			file.createNewFile();// 新建文件
			output = new FileOutputStream(file);
			// 读取大文件
			byte[] buffer = new byte[1024];
			int size = 0; 
			while ((size = input.read(buffer)) != -1) {
				output.write(buffer, 0 , size);
			}
			output.close();
			input.close();
			return true;
			}
			return false;

		} catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean unzip() {
		String zipPath = sTempFile;
		File zipFile = new File(zipPath);
		if (!zipFile.exists())
			return false;
		String destPath = sTempExtPath;
		File destPathDir = new File(destPath);
		if (destPathDir.exists())
			Utils.deleteDirectory(destPath);
		try {
			ZipUtils.upZipFile(zipFile, destPath);
			return true;
		} catch (ZipException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean copyToDest() {
		String origProfile = sTempProfilePath;
		String destProfile = sDestProfilePath;
		String origImg = sTempImgPath;
		String destImg = sDestImgPath;
		try {
			File destProfileDir = new File(destProfile);
			if (destProfileDir.exists())
				destProfileDir.delete();
			Utils.copyDirectiory(origProfile, destProfile);
			File destImgDir = new File(destImg);
			if (destImgDir.exists())
				destImgDir.delete();
			Utils.copyDirectiory(origImg, destImg);
			Utils.deleteDirectory(sTempExtPath);
			return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

}
