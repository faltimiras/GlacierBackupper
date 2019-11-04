package cat.altimiras.glacier.backupper;

import java.util.Date;
import java.util.Objects;

class Job {

	private String jobId;
	private String archiveId;
	private Date creation;
	private String name;
	private String region;
	private String vault;
	private Date lastStatus;
	private boolean urgent;

	public Job() {
	}

	public Job(String jobId, String archiveId, String name, String region, String vault, boolean urgent) {
		this.jobId = jobId;
		this.archiveId = archiveId;
		this.name = name;
		this.creation = new Date();
		this.vault = vault;
		this.region = region;
		this.urgent = urgent;
	}

	public Date getLastStatus() {
		return lastStatus;
	}

	public void setLastStatus(Date lastStatus) {
		this.lastStatus = lastStatus;
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

	public String getName() {
		return name;
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
