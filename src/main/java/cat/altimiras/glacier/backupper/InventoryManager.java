package cat.altimiras.glacier.backupper;

import java.util.Optional;

interface InventoryManager {

	void addItem(Item i) throws Exception;

	void removeItem(Item i) throws Exception;

	void addJob(Job j) throws Exception;

	void removeJob(Job j) throws Exception;

	void markJobChecked(Job job);

	Iterable<Item> getItems();

	Iterable<Job> getJobs();

	Optional<Item> findItemByName(String name);

	Optional<Item> findItemByChecksum(String checksum);

	Optional<Job> findJobByName(String name);
}