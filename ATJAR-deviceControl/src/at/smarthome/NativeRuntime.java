package at.smarthome;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;

public class NativeRuntime {

	private static NativeRuntime theInstance = null;

	private NativeRuntime() {

	}
	
	public static NativeRuntime getInstance() {
		if (theInstance == null)
			theInstance = new NativeRuntime();
		return theInstance;
	}

	/**
	 * RunExecutable ����һ�������е�lib*.so�ļ�
	 * @date 2016-1-18 ����8:22:28
	 * @param pacaageName
	 * @param filename
	 * @param alias ����
	 * @param args ����
	 * @return
	 */
	public String RunExecutable(String pacaageName, String filename, String alias, String args) {
		String path = "/data/data/" + pacaageName;
		String cmd1 = path + "/lib/" + filename;
		String cmd2 = path + "/" + alias;
		String cmd2_a1 = path + "/" + alias + " " + args;
		String cmd3 = "chmod 777 " + cmd2;
		String cmd4 = "dd if=" + cmd1 + " of=" + cmd2;
		StringBuffer sb_result = new StringBuffer();

		if (!new File("/data/data/" + alias).exists()) {
			RunLocalUserCommand(pacaageName, cmd4, sb_result); // ����lib/libtest.so����һ��Ŀ¼,ͬʱ����Ϊtest.
			sb_result.append(";");
		}
		RunLocalUserCommand(pacaageName, cmd3, sb_result); // �ı�test������,�����Ϊ��ִ��
		sb_result.append(";");
		RunLocalUserCommand(pacaageName, cmd2_a1, sb_result); // ִ��test����.
		sb_result.append(";");
		return sb_result.toString();
	}

	/**
	 * ִ�б����û�����
	 * @date 2016-1-18 ����8:23:01
	 * @param pacaageName
	 * @param command
	 * @param sb_out_Result
	 * @return
	 */
	public boolean RunLocalUserCommand(String pacaageName, String command, StringBuffer sb_out_Result) {
		Process process = null;
		try {
			process = Runtime.getRuntime().exec("sh"); // ���shell����
			DataInputStream inputStream = new DataInputStream(process.getInputStream());
			DataOutputStream outputStream = new DataOutputStream(process.getOutputStream());
			outputStream.writeBytes("cd /data/data/" + pacaageName + "\n"); // ��֤��command���Լ�������Ŀ¼��ִ��,����Ȩ��д�ļ�����ǰĿ¼
			outputStream.writeBytes(command + " &\n"); // �ó����ں�̨���У�ǰ̨���Ϸ���
			outputStream.writeBytes("exit\n");
			outputStream.flush();
			process.waitFor();
			byte[] buffer = new byte[inputStream.available()];
			inputStream.read(buffer);
			String s = new String(buffer);
			if (sb_out_Result != null)
				sb_out_Result.append("CMD Result:\n" + s);
		} catch (Exception e) {
			if (sb_out_Result != null)
				sb_out_Result.append("Exception:" + e.getMessage());
			return false;
		}
		return true;
	}

	public native void startActivity(String compname);

	public native String stringFromJNI();

	public native void startService(String srvname, String sdpath);

	public native int findProcess(String packname);

	public native int stopService();

	static {
		try {
			System.loadLibrary("helper"); // ����so��
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
