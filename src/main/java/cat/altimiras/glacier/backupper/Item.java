package cat.altimiras.glacier.backupper;

import java.util.Date;
import java.util.Objects;

class Item {

	final private String name;
	final private Date uploadDate;
	final private String archiveId;
	final private long size;
	final private String vault;
	final private String region;


	public Item(String name, String archiveId,  long size, String vault, String region) {
		this.name = name;
		this.uploadDate = new Date();
		this.size = size;
		this.vault = vault;
		this.region = region;
		this.archiveId = archiveId;
	}

	public String getName() {
		return name;
	}

	public Date getUploadDate() {
		return uploadDate;
	}

	public String getArchiveId() {
		return archiveId;
	}

	public long getSize() {
		return size;
	}

	public String getVault() {
		return vault;
	}

	public String getRegion() {
		return region;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Item item = (Item) o;
		return Objects.equals(archiveId, item.archiveId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(archiveId);
	}

	@Override
	public String toString() {
		return "Item{" +
				"name='" + name + '\'' +
				", uploadDate=" + uploadDate +
				", archiveId='" + archiveId + '\'' +
				", size=" + size +
				", vault='" + vault + '\'' +
				", region='" + region + '\'' +
				'}';
	}
}
