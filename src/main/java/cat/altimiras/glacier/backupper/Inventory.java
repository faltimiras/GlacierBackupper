package cat.altimiras.glacier.backupper;

import java.util.ArrayList;
import java.util.List;

class Inventory {

	private String awsKey;
	private String awsSecret;
	private String defaultRegion;
	private String defaultVault;

	private List<Item> items = new ArrayList<>();
	private List<Job> jobs = new ArrayList<>();

	public String getAwsKey() {
		return awsKey;
	}

	public void setAwsKey(String awsKey) {
		this.awsKey = awsKey;
	}

	public String getAwsSecret() {
		return awsSecret;
	}

	public void setAwsSecret(String awsSecret) {
		this.awsSecret = awsSecret;
	}

	public String getDefaultRegion() {
		return defaultRegion;
	}

	public void setDefaultRegion(String defaultRegion) {
		this.defaultRegion = defaultRegion;
	}

	public String getDefaultVault() {
		return defaultVault;
	}

	public void setDefaultVault(String defaultVault) {
		this.defaultVault = defaultVault;
	}

	public void addItem(Item i) {
		this.items.add(i);
	}

	public void removeItem(Item i) {
		this.items.remove(i);
	}

	public void addJob(Job j) {
		this.jobs.add(j);
	}

	public void removeJobs(Job j) {
		this.jobs.remove(j);
	}

	public Item findItemByName(String name) {
		for (Item i : items) {
			if (i.getName().equals(name)) {
				return i;
			}
		}
		return null;
	}

	public Job findJobByName(String name) {
		for (Job j : jobs) {
			if (j.getFileName().equals(name)) {
				return j;
			}
		}
		return null;
	}
}
