package cat.altimiras.glacier.backupper;

import java.util.Date;
import java.util.Objects;

class Item {

	private String name;
	private Date uploadDate;
	private String archiveId;
	private String checksum;
	private long size;
	private String vault;
	private String region;

	public Item() {
	}

	public Item(String name, String checksum, String archiveId, long size, String vault, String region) {
		this.name = name;
		this.uploadDate = new Date();
		this.size = size;
		this.vault = vault;
		this.region = region;
		this.archiveId = archiveId;
		this.checksum = checksum;
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

	public String getChecksum() {
		return checksum;
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
				", checksum='" + checksum + '\'' +
				", size=" + size +
				", vault='" + vault + '\'' +
				", region='" + region + '\'' +
				'}';
	}
}
