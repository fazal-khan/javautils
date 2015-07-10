package com.mm.utils;

import java.io.Closeable;
import java.io.FileInputStream;
import java.util.List;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class SftpUtil {
	
	public static void test() throws Exception{
		Session session = SftpUtil.getSession("127.0.0.1", "demo", "demo");
		session.connect();
		try{
			List<LsEntry> files = SftpUtil.getFiles(getChannelSftp(session), "/test", "c:/test");
			for(LsEntry  file : files){
				log(file.getFilename());
			}
		}finally{
			session.disconnect();
		}
	}
	
	// can be used to filter the files, not implemented here.
	public interface LsEntryFilter{
		boolean accept(LsEntry e);	
	}

	public static Session getSession(String hostIp, String username, String password) throws Exception {
		log("SFTPUtil : entering getSession....");
		JSch jsch = new JSch();
		Session session = jsch.getSession(username, hostIp);
		session.setPassword(password);
		session.setConfig("StrictHostKeyChecking", "no");
		log("SFTPUtil : exiting getSession....");
		return session;
	}
	
	public static ChannelSftp getChannelSftp(Session session) throws Exception {
		Channel channel = session.openChannel("sftp");
		channel.setInputStream(System.in);
		channel.setOutputStream(System.out);
		channel.connect();

		ChannelSftp sftpChannel = (ChannelSftp) channel;
		return sftpChannel;
	}

	
	// updloads the src file to remote server.
	public static void uploadFile(Session session, String srcFile, String destFile) throws Exception {
		log("SFTPUtil : Entering uploadFile.....");

		ChannelSftp sftpChannel = getChannelSftp(session);
		FileInputStream is = new FileInputStream(srcFile);

		try {
			sftpChannel.put(is, destFile);
		} finally {
			close(is);
			if (sftpChannel != null)
				sftpChannel.disconnect();
		}
		log("SFTPUtil : Exiting uploadFile.....");
	}

	public static void downloadFile(Session session, String srcDir, String destDir, String fileName) throws Exception {
		ChannelSftp sftpChannel = getChannelSftp(session);
		List<ChannelSftp.LsEntry> list = getFiles(sftpChannel, srcDir, destDir);
		try {
			log("SFTPUtil : ls command output is :" + list);
			for (ChannelSftp.LsEntry file : list) {
				if (!file.getAttrs().isDir() && file.getFilename().equals(fileName)) {
					log("SFTPUtil : downloading file to local working dir, filename is : [" + file.getFilename() + "]");
					sftpChannel.get(file.getFilename(), file.getFilename());
				}
			}
		} finally {
			if (sftpChannel != null)
				sftpChannel.disconnect();
		}
	}

	
	public static List<ChannelSftp.LsEntry> getFiles(ChannelSftp sftpChannel, String srcDir, String destDir)
			throws Exception {

		sftpChannel.lcd(destDir);
		log("SFTPUtil : local working dir: " + sftpChannel.lpwd());

		sftpChannel.cd(srcDir);
		log("SFTPUtil : remote working dir: " + sftpChannel.pwd());

		// Get a listing of the remote directory
		@SuppressWarnings("unchecked")
		List<ChannelSftp.LsEntry> list = sftpChannel.ls(".");
		log("SFTPUtil : running command 'ls .' on remote server : ");

		return list;
	}

	

	private static void log(String msg) {
		System.out.println(msg);
	}

	private static void close(Closeable is) {
		try {
			if (is != null)
				is.close();
		} catch (Exception e) {
		}
	}

}
