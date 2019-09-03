package cat.altimiras.glacier.backupper;

import java.util.Date;
import java.util.Objects;

class Job {

	final private String jobId;
	final private String archiveId;
	final private Date creation;
	final private String fileName;
	final private String region;
	final private String vault;

	public Job(String jobId, String archiveId, String fileName, String region, String vault) {
		this.jobId = jobId;
		this.archiveId = archiveId;
		this.fileName = fileName;
		this.creation = new Date();
		this.vault = vault;
		this.region = region;
	}

	public String getJobId() {
		return jobId;
	}

	public String getArchiveId() {
		return archiveId;
	}

	public Date getCreation() {
		return creation;
	}

	public String getFileName() {
		return fileName;
	}

	public String getRegion() {
		return region;
	}

	public String getVault() {
		return vault;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Job job = (Job) o;
		return Objects.equals(jobId, job.jobId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(jobId);
	}
}
