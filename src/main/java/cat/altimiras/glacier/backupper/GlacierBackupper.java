package cat.altimiras.glacier.backupper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.glacier.model.StatusCode;

import javax.xml.bind.DatatypeConverter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

class GlacierBackupper {

	final static Logger logger = LoggerFactory.getLogger(GlacierBackupper.class);

	private InventoryManager inventoryManager;
	private GlacierManager glacierManager;

	GlacierBackupper(Path inventoryPath, String awsKey, String awsSecret) throws Exception {
		Objects.requireNonNull(inventoryPath);

		inventoryManager = new InventoryManagerJson(inventoryPath);
		glacierManager = new GlacierManager(awsKey, awsSecret);
	}

	void upload(String name, String vault, String region, Path path, int chunkSize) throws Exception {
		Objects.requireNonNull(path);
		if (!Files.exists(path)) {
			logger.error("File do not exist!");
		}

		name = normalize(get("File Name", name, path.getFileName().toString()));

		String checksum = calculateChecksum(path);
		Optional<Item> exist = inventoryManager.findItemByChecksum(checksum);
		if (exist.isPresent()) {
			logger.info("File has been already uploaded previously: {}", exist.get());
		} else {
			String archiveId = glacierManager.upload(name, path, region, vault, chunkSize);
			logger.info("File: {} uploaded successfully with name: {} and id: {}", path, name, archiveId);
			inventoryManager.addItem(new Item(name, checksum, archiveId, path.toFile().length(), vault, region));
			logger.info("Inventory updated");
		}
	}

	void createDownloadJob(String name, boolean urgent) throws Exception {
		Objects.requireNonNull(name);

		Optional<Item> item = inventoryManager.findItemByName(name);
		if (item.isPresent()) {
			Optional<String> jobId = glacierManager.askToDownload(item.get(), urgent);
			if (jobId.isPresent()) {
				inventoryManager.addJob(new Job(jobId.get(), item.get().getArchiveId(), item.get().getName(), item.get().getRegion(), item.get().getVault(), urgent));
				logger.info("Job to download {} created successfully", item.get().getName());
			}
		} else {
			logger.info("Any item with name: {}", name);
		}
	}

	void download(String name, Path target, int chunkSize, boolean removeJob) throws Exception {
		Objects.requireNonNull(name);
		Optional<Job> job = inventoryManager.findJobByName(name);

		if (job.isPresent()) {
			Optional<Boolean> isReady = glacierManager.isReadyDownload(job.get());
			if (isReady.isPresent()) {
				if (isReady.get()) {
					glacierManager.download(job.get(), target, chunkSize);
					if (removeJob) {
						inventoryManager.removeJob(job.get());
					}
				} else {
					logger.info("Download is not still available");
				}
			} else {
				logger.info("Download job expired, Create a new job to a future download. They expire more or less after 1day after job is completed");
				inventoryManager.removeJob(job.get());
			}
		} else {
			logger.info("Create a job to a future download. Glacier is not a 'live' tool, it can take from 3 to 12h (urgent flag speeds the operation and increase the cost");
		}
	}

	void list() {
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		for (Item item : inventoryManager.getItems()) {
			logger.info("NAME: {}", item.getName());
			logger.info("UPLOAD DATE: {} SIZE: {}", sdf.format(item.getUploadDate()), item.getSize());
			logger.info("------");
		}
	}

	void jobStatus() throws Exception {
		for (Job job : inventoryManager.getJobs()) {

			Optional<StatusCode> statusCode = glacierManager.getJobStatus(job);
			if (statusCode.isPresent()) {
				logger.info("JOB FOR: {}", job.getName());
				logger.info("Created {} ago", diffDates(job.getCreation(), new Date()));
				if (job.getLastStatus() != null) {
					logger.info("Last status check {} ago", diffDates(job.getLastStatus(), new Date()));
				}
				logger.info("Status: {}", (statusCode.get() == StatusCode.SUCCEEDED ? "READY TO DOWNLOAD" : statusCode));
				inventoryManager.markJobChecked(job);
			} else {
				inventoryManager.removeJob(job);
			}
			logger.info("------");
		}
	}

	void remove(String name) throws Exception {
		Optional<Item> item = inventoryManager.findItemByName(name);
		if (item.isPresent()) {
			glacierManager.remove(item.get());
			inventoryManager.removeItem(item.get());
			logger.info("File {} has been removed from the vault {}", item.get().getName(), item.get().getArchiveId());
		}
	}

	private String get(String element, String... options) {
		for (String op : options) {
			if (op != null && !op.isEmpty()) {
				return op;
			}
		}
		throw new IllegalArgumentException(element + " has not value");
	}

	private String calculateChecksum(Path path) throws Exception {
		MessageDigest md = MessageDigest.getInstance("MD5");
		try (InputStream is = Files.newInputStream(path);
			 DigestInputStream dis = new DigestInputStream(is, md)) {
			byte[] buffer = new byte[4 * 1024 * 1024];
			int i = 0;
			while (dis.read(buffer) > -1 && i < 10) { //partitial checksum of max first 40mb
				i++;
			}
		}
		byte[] digest = md.digest();
		return DatatypeConverter.printHexBinary(digest).toUpperCase();
	}

	private String diffDates(Date d1, Date d2) {
		SimpleDateFormat sdf = new SimpleDateFormat("H'h' m'm' s's'");
		long diffInMillis = Math.abs(d1.getTime() - d2.getTime());
		Date d = new Date(diffInMillis);
		return sdf.format(d);
	}

	private  String normalize(String src) {
		return Normalizer
				.normalize(src, Normalizer.Form.NFD)
				.replaceAll("[^\\p{ASCII}]", "");
	}
}