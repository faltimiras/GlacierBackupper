package cat.altimiras.glacier.backupper;

import java.util.ArrayList;
import java.util.List;

class Inventory {

	private List<Item> items = new ArrayList<>();
	private List<Job> jobs = new ArrayList<>();

	public List<Item> getItems() {
		return items;
	}

	public List<Job> getJobs() {
		return jobs;
	}
}
