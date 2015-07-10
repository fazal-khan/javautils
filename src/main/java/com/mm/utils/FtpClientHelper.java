package com.mm.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.apache.commons.net.ftp.FTPFileFilters;

public class FtpClientHelper {

	private String		host;
	private int			port		= 21;
	private FTPClient	ftpClient	= null;

	public FtpClientHelper(String ftpHost) throws Exception {
		this.host = ftpHost;
	}

	public List<String> listFiles(String ftpPath, FTPFileFilter ff, boolean recurse) throws IOException {
		List<String> files = new ArrayList<>();
		ff = ff == null ? FTPFileFilters.ALL : ff;
		FTPFile[] listFiles = ftpClient.listFiles(ftpPath);
		for (FTPFile f : listFiles) {
			if (ff.accept(f) && f.isFile()) {
				files.add(ftpPath + "/" + f.getName());
			} else if (f.isDirectory() && !(f.getName().equals(".") || f.getName().equals(".."))) {
				files.addAll(listFiles(ftpPath + "/" + f.getName(), ff, recurse));
			}
		}
		return files;
	}

	

	public void downloadFiles(String ftpPath, FTPFileFilter ff, String outdir, boolean recurse) throws Exception {
		List<String> files = listFiles(ftpPath, ff, recurse);
		// client.enterLocalPassiveMode(); // uncomment if server is non local. 
		this.ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
		for (String f : files) {
			log("downloading file : " + f);
			String filename = f.substring(f.lastIndexOf("/") + 1);
			String dir 		= f.substring(0, f.lastIndexOf("/") > 0 ? f.lastIndexOf("/") : 0);

			File file = new File(outdir.replaceAll("/", File.separator) + File.separator + filename);
			try (OutputStream outfile = new BufferedOutputStream(new FileOutputStream(file))) {
				this.ftpClient.changeWorkingDirectory(dir);
				this.ftpClient.retrieveFile(filename, outfile);
			}

		}
	}

	public void connect(String username, String password) throws Exception {
		FTPClient client = new FTPClient();
		for (int i = 0; i < 5; i++) { // try five times
			client.connect(host, port);
			if (client.isConnected()) {
				break;
			}
		}
		if(!client.isConnected()) throw new RuntimeException("Unable to connect to host :" + this.host);
		client.login(username, password);
		this.ftpClient = client;
		printStatistics();
	}

	public void disconnect() {
		try {
			if (this.ftpClient != null && this.ftpClient.isConnected()) {
				this.ftpClient.disconnect();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void printStatistics() {
		if (ftpClient == null)
			throw new NullPointerException("FTPClient cannot be null.");
		if (!ftpClient.isConnected())
			throw new RuntimeException("FTPClient should be connected to remote peer.");
		log(String.format("%-30s : %s", "isConnected", ftpClient.isConnected()));
		log(String.format("%-30s : %s", "isAvailable", ftpClient.isAvailable()));
		log(String.format("%-30s : %s", "isRemoteVerificationEnabled", ftpClient.isRemoteVerificationEnabled()));
		log(String.format("%-30s : %s", "isStrictMultilineParsing", ftpClient.isStrictMultilineParsing()));
		log(String.format("%-30s : %s", "isUseEPSVwithIPv4", ftpClient.isUseEPSVwithIPv4()));
	}

	private static void log(String msg) {
		System.out.println(msg);
	}

}
