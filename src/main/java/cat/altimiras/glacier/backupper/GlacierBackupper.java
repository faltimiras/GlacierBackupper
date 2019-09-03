package cat.altimiras.glacier.backupper;

import java.nio.file.Path;
import java.util.Objects;

class GlacierBackupper {

	final private InventoryManager inventoryManager;
	final private Inventory inventory;
	final private GlacierManager glacierManager;

	GlacierBackupper(Path inventory, String awsKey, String awsSecret) throws Exception {
		this.inventoryManager = new InventoryManager(inventory);
		this.inventory = inventoryManager.read();

		String key = get("AWS key", awsKey, this.inventory.getAwsKey());
		String secret = get("AWS secret", awsSecret, this.inventory.getAwsSecret());

		this.glacierManager = new GlacierManager(key, secret);
	}

	void upload(String name, String vault, String region, Path path, int chunkSize) throws Exception {
		Objects.requireNonNull(path);

		name = get("File Name", name, path.getFileName().toString());
		region = get("AWS Region", region, this.inventory.getDefaultRegion());
		vault = get("Vault", vault, this.inventory.getDefaultVault());

		Item exist = this.inventory.findItemByName(name);
		if (exist == null) {
			String archiveId = glacierManager.upload(name, path, region, vault, chunkSize);
			System.out.println("File: " + path + " uploaded successfully with id:" + archiveId);
			Item item = new Item(name, archiveId, path.toFile().length(), vault, region);
			this.inventory.addItem(item);
			this.inventoryManager.store(inventory);
			System.out.println("Inventory updated");
		} else {
			System.out.println("File with this name has been already uploaded: " + exist);
		}
	}

	void createDownloadJob(String name) throws Exception {
		Objects.requireNonNull(name);

		Item item = this.inventory.findItemByName(name);
		if (item == null) {
			System.out.println("Any item with name =" + name);
		} else {
			String jobId = glacierManager.askToDownload(item);
			this.inventory.addJob(new Job(jobId, item.getArchiveId(), item.getName(), item.getRegion(), item.getVault()));
			this.inventoryManager.store(inventory);
			System.out.println("Job to download " + item.getName() + " created successfully");
		}
	}

	void download(String name, Path target, int chunkSize) throws Exception {
		Objects.requireNonNull(name);
		Job job = this.inventory.findJobByName(name);

		if (job == null){
			System.out.println("Creating a job to a future download. Glacier is not a 'live' tool, it can take a day");
		}
		else {
			if (glacierManager.isReadyDownload(job)) {
				glacierManager.download(job, target, chunkSize);
			}
			else {
				System.out.println("Download is not still available");
			}
		}
	}

	void configure(String awsKey, String awsSecret, String region, String vault){

		if (awsKey != null && !awsKey.isEmpty()){
			this.inventory.setAwsKey(awsKey);
		}
		if (awsSecret != null && !awsSecret.isEmpty()){
			this.inventory.setAwsSecret(awsSecret);
		}
		if (region != null && !region.isEmpty()){
			this.inventory.setDefaultRegion(region);
		}
		if (vault != null && !vault.isEmpty()){
			this.inventory.setDefaultVault(vault);
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
}