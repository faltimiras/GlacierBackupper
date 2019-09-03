package cat.altimiras.glacier.backupper;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

class InventoryManager {

	final private ObjectMapper objectMapper;
	final private Path path;

	public InventoryManager(Path path) {
		this.objectMapper = new ObjectMapper();
		this.objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
		this.objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
		this.path = path;
	}

	public Inventory read() throws Exception {
		if (Files.exists(path)) {
			return objectMapper.readValue(Files.readAllBytes(path), Inventory.class);
		}
		else {
			return new Inventory();
		}
	}

	public void store(Inventory inventory) throws Exception {
		byte[] content = objectMapper.writeValueAsBytes(inventory);
		Files.write(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	}

}
