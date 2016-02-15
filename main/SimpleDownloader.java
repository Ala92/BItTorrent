package main;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.download.DownloadManagerStats;
import org.gudy.azureus2.core3.global.GlobalManager;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreException;
import com.aelitis.azureus.core.AzureusCoreFactory;

public class SimpleDownloader {
	private static AzureusCore core;

	public static void main(String[] args) throws Exception {
		if (args.length != 3) {
			System.out
					.println("Usage : TwoTorrentsDownloader torrent  upload_limit_kb/s download_limit_kb/s");
			System.exit(0);
		}
		core = AzureusCoreFactory.create();
		core.start();

		// System.out.println("Attempting to download torrent at : " + url);
		File downloadedTorrentFile = new File(args[0]);
		File downloadDirectory = new File("downloads"); // Destination directory
		if (downloadDirectory.exists() == false)
			downloadDirectory.mkdir();
		GlobalManager globalManager = core.getGlobalManager();
		DownloadManager manager = globalManager.addDownloadManager(
				downloadedTorrentFile.getAbsolutePath(),
				downloadDirectory.getAbsolutePath());
		DownloadManagerListener listener = new DownloadStateListener();
		manager.addListener(listener);

		COConfigurationManager.setParameter("Max Upload Speed KBs",
				Integer.parseInt(args[1]));
		COConfigurationManager.setParameter("Max LAN Upload Speed KBs",
				Integer.parseInt(args[1]));

		COConfigurationManager.setParameter("Max Download Speed KBs",
				Integer.parseInt(args[2]));
		COConfigurationManager.setParameter("Max LAN Download Speed KBs",
				Integer.parseInt(args[2]));

		globalManager.startAllDownloads();
	}

	private static class DownloadStateListener implements
			DownloadManagerListener {
		static void outputToFile() {
			   String ip="";
			    try {
			        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			        while (interfaces.hasMoreElements()) {
			            NetworkInterface iface = interfaces.nextElement();
			            if (iface.isLoopback() || !iface.isUp())
			                continue;
			            Enumeration<InetAddress> addresses = iface.getInetAddresses();
			            while(addresses.hasMoreElements()) {
			                InetAddress addr = addresses.nextElement();
			                ip = addr.getHostAddress();			            }
			        }
			    } catch (SocketException e) {
			        throw new RuntimeException(e);
			    }		
			    try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(
						ip + ".txt",
						true));
				DownloadManager man = null;
				DownloadManagerStats stats = null;
				AzureusCore core = AzureusCoreFactory.getSingleton();
				ArrayList<DownloadManager> managers = (ArrayList<DownloadManager>) core
						.getGlobalManager().getDownloadManagers();
				try {
					man = ((java.util.List<DownloadManager>) managers).get(0);
					stats = man.getStats();
				} catch (Exception e) {
					return;
				}
				writer.write(System.currentTimeMillis()
						+ "\t"
						+ stats.getElapsedTime()

						+ "\t"
						+ man.getTorrentFileName().substring(
								man.getTorrentFileName().lastIndexOf('/') + 1,
								man.getTorrentFileName().length())
						+ "\t"
						// time in seconds (String)
						+ stats.getCompleted() / 10.0 + "%\t"
						+ stats.getDataSendRate() + "\t"
						+ stats.getDataReceiveRate() + "\t"
						+ stats.getTotalGoodDataBytesReceived() + "\t"
						+ stats.getTotalDataBytesSent() + "\t"
						+ stats.getShareRatio() + "\n");
				writer.flush();
			} catch (Exception ex) {
				// report
				ex.printStackTrace();
				return;
			}
		}

		public void stateChanged(DownloadManager manager, int state) {

			switch (state) {
			case DownloadManager.STATE_CHECKING:
				// outputToFile("Checking....");
				break;
			case DownloadManager.STATE_FINISHING:
				outputToFile();
				break;
			case DownloadManager.STATE_SEEDING:
				outputToFile();
				break;
			case DownloadManager.STATE_STOPPED:
				System.out.println("Download Stopped.");
				// outputToFile("Download Stopped.");
				
				break;
			case DownloadManager.STATE_DOWNLOADING:
				Runnable checkAndPrintProgress = new Runnable() {
					public void run() {

						try {
							boolean downloadCompleted = false;
							while (!downloadCompleted) {

								AzureusCore core = AzureusCoreFactory
										.getSingleton();

								ArrayList<DownloadManager> managers = (ArrayList<DownloadManager>) core
										.getGlobalManager()
										.getDownloadManagers();
								DownloadManager man;
								DownloadManagerStats stats = null;
								try {
									man = ((java.util.List<DownloadManager>) managers)
											.get(0);
									stats = man.getStats();
								} catch (Exception e) {
									return;
								}
								   String ip="";
								    try {
								        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
								        while (interfaces.hasMoreElements()) {
								            NetworkInterface iface = interfaces.nextElement();
								            if (iface.isLoopback() || !iface.isUp())
								                continue;
								            Enumeration<InetAddress> addresses = iface.getInetAddresses();
								            while(addresses.hasMoreElements()) {
								                InetAddress addr = addresses.nextElement();
								                ip = addr.getHostAddress();			            }
								        }
								    } catch (SocketException e) {
								        throw new RuntimeException(e);
								    }		
								    try {

									BufferedWriter writer = new BufferedWriter(new FileWriter(
											ip + ".txt",
											true));
									writer.write(System.currentTimeMillis()
											+ "\t"
											+ stats.getElapsedTime()

											+ "\t"
											+ man.getTorrentFileName()
													.substring(
															man.getTorrentFileName()
																	.lastIndexOf(
																			'/') + 1,
															man.getTorrentFileName()
																	.length())
											+ "\t"
											// time in seconds (String)
											+ stats.getCompleted()
											/ 10.0
											+ "%\t"
											+ stats.getDataSendRate()
											+ "\t"
											+ stats.getDataReceiveRate()
											+ "\t"
											+ stats.getTotalGoodDataBytesReceived()
											+ "\t"
											+ stats.getTotalDataBytesSent()
											+ "\t" + stats.getShareRatio()
											+ "\n");
									writer.flush();
								} catch (IOException ex) {
									// report
									ex.printStackTrace();
								}

								System.out.println(System.currentTimeMillis()
										+ "\t"
										+ stats.getElapsedTime()
										+ "\t" // time in seconds (String)
										+ stats.getCompleted() / 10.0 + "%\t"
										+ stats.getDataSendRate() + "\t"
										+ stats.getDataReceiveRate() + "\t"
										+ stats.getTotalGoodDataBytesReceived()
										+ "\t" + stats.getTotalDataBytesSent()
										+ "\t" + stats.getShareRatio());

								Thread.sleep(1000);
							}
						} catch (Exception e) {
							throw new RuntimeException(e);
						}

					}
				};

				Thread progressChecker = new Thread(checkAndPrintProgress);
				progressChecker.setDaemon(true);
				progressChecker.start();
				break;
			}
		}

		public void downloadComplete(DownloadManager manager) {
			outputToFile();
			System.out.println("Download Completed - Exiting.....");
			AzureusCore core = null;
			try {
				core = AzureusCoreFactory.getSingleton();
				core.requestStop();
			} catch (AzureusCoreException aze) {
				System.out
						.println("Could not end Azureus session gracefully - "
								+ "forcing exit.....");
				core.stop();
			}
		}

		public void completionChanged(DownloadManager arg0, boolean arg1) {
			// TODO Auto-generated method stub

		}

		public void filePriorityChanged(DownloadManager arg0,
				DiskManagerFileInfo arg1) {
			// TODO Auto-generated method stub

		}

		public void positionChanged(DownloadManager arg0, int arg1, int arg2) {
			// TODO Auto-generated method stub

		}

	}
}