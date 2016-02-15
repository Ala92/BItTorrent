package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

// Usage : Downloader.java url_torrent1 url_torrent2 upload_speed download_speed
public class Downloader {
	private static AzureusCore core;

	public static void main(String[] args) throws Exception {
		if (args.length != 6) {
			System.out.println(
					"Usage : Downloader torrent_to_download torrent_to_bundle t_begin_bundle delta upload_limit_kb/s download_limit_kb/s");
			System.exit(0);
		}
		core = AzureusCoreFactory.create();
		core.start();
		File torrentToDownload = new File(args[0]);
		File downloadDirectory = new File("downloads"); // Destination directory
		if (downloadDirectory.exists() == false)
			downloadDirectory.mkdir();
		GlobalManager globalManager = core.getGlobalManager();
		// DownloadManager manager2 =
		// globalManager.addDownloadManager(TorrentToBundle.getAbsolutePath(),
		// downloadDirectory.getAbsolutePath());
		// DownloadManagerListener listener2 = new DownloadStateListener(1);
		// manager2.addListener(listener2);
		DownloadManager manager1 = globalManager.addDownloadManager(torrentToDownload.getAbsolutePath(),
				downloadDirectory.getAbsolutePath());
		DownloadManagerListener listener1 = new DownloadStateListener(0, Double.parseDouble(args[2]),
				Double.parseDouble(args[3]), args[1]);
		manager1.addListener(listener1);

		COConfigurationManager.setParameter("Max Upload Speed KBs", Integer.parseInt(args[4]));
		COConfigurationManager.setParameter("Max LAN Upload Speed KBs", Integer.parseInt(args[4]));
		COConfigurationManager.setParameter("Max Download Speed KBs", Integer.parseInt(args[5]));
		COConfigurationManager.setParameter("Max LAN Download Speed KBs", Integer.parseInt(args[5]));

		globalManager.startAllDownloads();
	}

	private static class DownloadStateListener implements DownloadManagerListener {
		// i specifies the type/id of the DownloadStateListener
		// 0= download state listener for the first torrent
		// 1= download state listener for the bundled torrent
		int i;
		// time to begin bundling
		double t_begin;
		// download percentage of the bundled torrent required to stop
		double delta;

		String torrentToBundleName;
		boolean bundlingStarted;

		public DownloadStateListener(int i, double t_begin, double delta, String torrentToBundleName) {
			this.i = i;
			this.t_begin = t_begin;
			this.delta = delta;
			this.torrentToBundleName = torrentToBundleName;
			bundlingStarted = false;
		}

		public void outputToFile() {
			String ip = "";
			try {
				Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
				while (interfaces.hasMoreElements()) {
					NetworkInterface iface = interfaces.nextElement();
					if (iface.isLoopback() || !iface.isUp())
						continue;
					Enumeration<InetAddress> addresses = iface.getInetAddresses();
					while (addresses.hasMoreElements()) {
						InetAddress addr = addresses.nextElement();
						ip = addr.getHostAddress();
					}
				}
			} catch (SocketException e) {
				throw new RuntimeException(e);
			}
			try {

				BufferedWriter writer = new BufferedWriter(new FileWriter(ip + ".txt", true));
				DownloadManager man = null;
				DownloadManagerStats stats = null;
				AzureusCore core = AzureusCoreFactory.getSingleton();
				ArrayList<DownloadManager> managers = (ArrayList<DownloadManager>) core.getGlobalManager()
						.getDownloadManagers();
				try {
					man = ((java.util.List<DownloadManager>) managers).get(i);
					stats = man.getStats();
				} catch (Exception e) {

				}
				writer.write(System.currentTimeMillis() + "\t" + stats.getElapsedTime()

						+ "\t" + man.getTorrentFileName().substring(man.getTorrentFileName().lastIndexOf('/') + 1,
								man.getTorrentFileName().length())
						+ "\t"
						// time in seconds (String)
						+ stats.getCompleted() / 10.0 + "%\t" + stats.getDataSendRate() + "\t"
						+ stats.getDataReceiveRate() + "\t" + stats.getTotalGoodDataBytesReceived() + "\t"
						+ stats.getTotalDataBytesSent() + "\t" + stats.getShareRatio() + "\n");
				writer.flush();
			} catch (Exception ex) {
				// report
				ex.printStackTrace();
				return;
			}
		}

		public void stateChanged(DownloadManager manager, int state) {
			AzureusCore core = null;
			switch (state) {

			case DownloadManager.STATE_CHECKING:
				// outputToFile("Checking....");
				break;

			case DownloadManager.STATE_FINISHING:
				outputToFile();
				break;

			case DownloadManager.STATE_SEEDING:
				// outputToFile("Download Complete - Seeding for other
				// users...");
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

								AzureusCore core = AzureusCoreFactory.getSingleton();

								ArrayList<DownloadManager> managers = (ArrayList<DownloadManager>) core
										.getGlobalManager().getDownloadManagers();
								DownloadManager man;
								DownloadManagerStats stats = null;
								try {
									man = ((java.util.List<DownloadManager>) managers).get(i);
									stats = man.getStats();
								} catch (Exception e) {
									return;
								}
								outputToFile();
								double elapsedTime = Double.parseDouble(
										stats.getElapsedTime().substring(0, stats.getElapsedTime().indexOf("s")));

								// If it's time to begin bundling, start
								// downloading the 2nd torrent
								if (elapsedTime >= t_begin && t_begin >= 0 && !bundlingStarted) {
									bundlingStarted = true;
									File torrentToBundle = new File(torrentToBundleName);
									GlobalManager globalManager = core.getGlobalManager();
									File downloadDirectory = new File("downloads"); // Destination
																					// directory
									DownloadManager manager2 = globalManager.addDownloadManager(
											torrentToBundle.getAbsolutePath(), downloadDirectory.getAbsolutePath());
									// Start a download listener for the torrent
									// to bundle, with t_delai=-1
									DownloadManagerListener listener2 = new DownloadStateListener(1, -1, 0, null);
									manager2.addListener(listener2);
								}

								// if finished downloading the first torrent and
								// reached delta per cent of the second torrent
								if (bundlingStarted) {
									DownloadManager man2 = ((java.util.List<DownloadManager>) managers).get(1);
									stats = man2.getStats();
									double percentageCompleted = stats.getCompleted() / 10.0;
									if (percentageCompleted >= delta) {
										try {
											core = AzureusCoreFactory.getSingleton();
											core.requestStop();
										} catch (AzureusCoreException aze) {
											System.out.println("Could not end Azureus session gracefully - "
													+ "forcing exit.....");
											core.stop();
										}
									}

								}

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
			System.out.println("Download completed ");
			// AzureusCore core = AzureusCoreFactory.getSingleton();
			// ArrayList<DownloadManager> managers =
			// (ArrayList<DownloadManager>) core.getGlobalManager()
			// .getDownloadManagers();
			// DownloadManager man1 = ((java.util.List<DownloadManager>)
			// managers).get(0);
			// DownloadManager man2 = ((java.util.List<DownloadManager>)
			// managers).get(1);
			// if (man1.getAssumedComplete() && man2.getAssumedComplete()) {
			// try {
			// core = AzureusCoreFactory.getSingleton();
			// core.requestStop();
			// } catch (AzureusCoreException aze) {
			// System.out.println("Could not end Azureus session gracefully - "
			// + "forcing exit.....");
			// core.stop();
			// }
			// }
		}

		public void completionChanged(DownloadManager arg0, boolean arg1) {
			// TODO Auto-generated method stub

		}

		public void filePriorityChanged(DownloadManager arg0, DiskManagerFileInfo arg1) {
			// TODO Auto-generated method stub

		}

		public void positionChanged(DownloadManager arg0, int arg1, int arg2) {
			// TODO Auto-generated method stub

		}
	}
}