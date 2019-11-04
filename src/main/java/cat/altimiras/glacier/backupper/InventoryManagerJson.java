package cat.altimiras.glacier.backupper;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.Optional;

class InventoryManagerJson implements InventoryManager {

	final private ObjectMapper objectMapper;
	final private Path path;
	private Inventory inventory;

	InventoryManagerJson(Path path) throws Exception {
		this.objectMapper = new ObjectMapper();
		this.objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
		this.objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
		this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		this.path = path;

		this.inventory = read();
	}

	public void addItem(Item i) throws Exception {
		this.inventory.getItems().add(i);
		store();
	}

	public void removeItem(Item i) throws Exception {
		this.inventory.getItems().remove(i);
		store();
	}

	public void addJob(Job j) throws Exception {
		this.inventory.getJobs().add(j);
		store();
	}

	public void removeJob(Job j) throws Exception {
		this.inventory.getJobs().remove(j);
		store();
	}

	public void markJobChecked(Job job) {
		findJobByName(job.getName()).ifPresent( j -> j.setLastStatus(new Date()));
	}

	public Iterable<Item> getItems() {
		return this.inventory.getItems();
	}

	public Iterable<Job> getJobs() {
		return this.inventory.getJobs();
	}

	public Optional<Item> findItemByName(String name) {
		for (Item i : this.inventory.getItems()) {
			if (i.getName().equals(name)) {
				return Optional.of(i);
			}
		}
		return Optional.empty();
	}

	public Optional<Item> findItemByChecksum(String checksum) {
		for (Item i : this.inventory.getItems()) {
			if (i.getChecksum().equals(checksum)) {
				return Optional.of(i);
			}
		}
		return Optional.empty();
	}

	public Optional<Job> findJobByName(String name) {
		for (Job j : this.inventory.getJobs()) {
			if (j.getName().equals(name)) {
				return Optional.of(j);
			}
		}
		return Optional.empty();
	}

	private Inventory read() throws Exception {
		if (Files.exists(path)) {
			return objectMapper.readValue(Files.readAllBytes(path), Inventory.class);
		} else {
			return new Inventory();
		}
	}

	private void store() throws Exception {
		byte[] content = objectMapper.writeValueAsBytes(inventory);
		Files.write(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	}
}