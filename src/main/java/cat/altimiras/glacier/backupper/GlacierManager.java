package cat.altimiras.glacier.backupper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.glacier.GlacierClient;
import software.amazon.awssdk.services.glacier.model.*;
import software.amazon.awssdk.utils.BinaryUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class GlacierManager {

	final static Logger logger = LoggerFactory.getLogger(GlacierManager.class);

	final private AwsCredentialsProvider awsCredentialsProvider;

	final private int defaultChunkSize = 1024 * 1024 *16;

	public GlacierManager(String awsKey, String awsSecret) {


		if (awsKey == null || awsSecret == null) {
			ProfileCredentialsProvider credentials = ProfileCredentialsProvider.create();
			this.awsCredentialsProvider = StaticCredentialsProvider.create(credentials.resolveCredentials());
		} else {
			AwsBasicCredentials credentials = AwsBasicCredentials.create(awsKey, awsSecret);
			this.awsCredentialsProvider = StaticCredentialsProvider.create(credentials);
		}
	}

	//https://github.com/aws/aws-sdk-java/blob/master/aws-java-sdk-glacier/src/main/java/com/amazonaws/services/glacier/transfer/ArchiveTransferManager.java
	public String upload(String name, Path file, String region, String vault, int chunkSize) throws Exception {

		chunkSize = chunkSize(chunkSize);
		GlacierClient glacier = getClient(region);

		InitiateMultipartUploadRequest initiateMultipartUploadRequest = InitiateMultipartUploadRequest.builder()
				.vaultName(vault)
				.partSize(String.valueOf(chunkSize))
				.archiveDescription(name)
				.build();

		InitiateMultipartUploadResponse initiateMultipartUploadResponse = glacier.initiateMultipartUpload(initiateMultipartUploadRequest);
		String uploadId = initiateMultipartUploadResponse.uploadId();

		List<byte[]> partChecksums = new ArrayList<>();

		FileChunker.PartIterator it = FileChunker.partitionate(file, chunkSize);
		long expectedChunks = it.getExpectedChunks() + 1;
		long currentChunks = 1;
		while (it.hasNext()) {

			FileChunker.Chunk chunk = it.next();

			String checksum = BinaryUtils.toHex(chunk.getChecksum());
			partChecksums.add(chunk.getChecksum());

			UploadMultipartPartRequest uploadMultipartPartRequest = UploadMultipartPartRequest.builder()
					.uploadId(uploadId)
					.vaultName(vault)
					.range(String.format("bytes %s-%s/*", chunk.getStart(), chunk.getStart() + chunk.getContent().length - 1))
					.checksum(checksum)
					.build();

			glacier.uploadMultipartPart(uploadMultipartPartRequest, RequestBody.fromBytes(chunk.getContent()));
			logger.info("Chunk uploaded {}/{}", currentChunks, expectedChunks);
			currentChunks++;
		}

		CompleteMultipartUploadRequest completeMultipartUploadRequest = CompleteMultipartUploadRequest.builder()
				.uploadId(uploadId)
				.vaultName(vault)
				.archiveSize(String.valueOf(file.toFile().length()))
				.checksum(Hash.calculateTreeHashStr(partChecksums))
				.build();

		CompleteMultipartUploadResponse completeMultipartUploadResponse = glacier.completeMultipartUpload(completeMultipartUploadRequest);
		logger.info("Uploaded completed");
		return completeMultipartUploadResponse.archiveId();
	}

	public Optional<String> askToDownload(Item item, boolean urgent) throws Exception {

		try {
		GlacierClient glacier = getClient(item.getRegion());

		JobParameters jobParameters = JobParameters.builder()
				.archiveId(item.getArchiveId())
				.type("archive-retrieval")
				.tier(urgent ? "Standard" : "Bulk")
				.build();

		InitiateJobRequest initiateJobRequest = InitiateJobRequest.builder()
				.jobParameters(jobParameters)
				.vaultName(item.getVault())
				.build();

		InitiateJobResponse initiateJobResponse = glacier.initiateJob(initiateJobRequest);
		return Optional.of(initiateJobResponse.jobId());
		} catch (ResourceNotFoundException e) {
			logger.info("Item is not available on Glacier. Can not be downloaded");
			return Optional.empty();
		}
	}


	public void download(Job job, Path target, int chunkSize) throws Exception {

		chunkSize = chunkSize(chunkSize);
		GlacierClient glacier = getClient(job.getRegion());

		DescribeJobRequest describeJobRequest = DescribeJobRequest.builder()
				.jobId(job.getJobId())
				.vaultName(job.getVault())
				.build();

		DescribeJobResponse describeJobResponse = glacier.describeJob(describeJobRequest);
		long size = describeJobResponse.archiveSizeInBytes();

		RandomAccessFile output = new RandomAccessFile(target.toFile(), "rw");

		long expectedChunks = Double.valueOf(Math.ceil(size / chunkSize)).longValue() + 1;
		long currentChunks = 1;
		long currentPos = 0;
		while (currentPos < size) {
			long pending = size - currentPos;
			long toRead = Math.min(pending, chunkSize);

			GetJobOutputRequest getJobOutputRequest = GetJobOutputRequest.builder()
					.jobId(job.getJobId())
					.vaultName(job.getVault())
					.range("bytes=" + currentPos + "-" + (currentPos + toRead - 1))
					.build();

			ResponseInputStream<GetJobOutputResponse> getJobOutputResponse = glacier.getJobOutput(getJobOutputRequest);
			appendToFile(output, getJobOutputResponse);
			logger.info("Downloaded part {}/{}", currentChunks, expectedChunks);

			currentPos += toRead;
			currentChunks++;
		}

		logger.info("Whole file has been downloaded into: {}", target);
	}


	public Optional<StatusCode> getJobStatus(Job job) throws Exception {

		try {
			GlacierClient glacier = getClient(job.getRegion());

			DescribeJobRequest describeJobRequest = DescribeJobRequest.builder()
					.jobId(job.getJobId())
					.vaultName(job.getVault())
					.build();

			DescribeJobResponse describeJobResponse = glacier.describeJob(describeJobRequest);
			return Optional.of(describeJobResponse.statusCode());
		} catch (ResourceNotFoundException e) {
			logger.info("Job is not available anymore. Create a new one");
			return Optional.empty();
		}
	}

	public Optional<Boolean> isReadyDownload(Job job) throws Exception {
		return getJobStatus(job).map( s -> s == StatusCode.SUCCEEDED);
	}

	public void remove(Item item) {

		try {
			GlacierClient glacier = getClient(item.getRegion());

			DeleteArchiveRequest deleteArchiveRequest = DeleteArchiveRequest.builder()
					.archiveId(item.getArchiveId())
					.vaultName(item.getVault()).build();

			glacier.deleteArchive(deleteArchiveRequest);
		} catch (ResourceNotFoundException e) {
			logger.info("Item {} it wasn't on Glacier", item.getName());
		}
	}

	private GlacierClient getClient(String region) {
		return GlacierClient.builder()
				.credentialsProvider(awsCredentialsProvider)
				.region(Region.of(region))
				.build();
	}

	/**
	 * Writes the data from the given input stream to the given output stream.
	 */
	private void appendToFile(RandomAccessFile output, InputStream input) throws IOException {
		byte[] buffer = new byte[1024 * 1024];
		int bytesRead = 0;
		do {
			bytesRead = input.read(buffer);
			if (bytesRead < 0) {
				break;
			}
			output.write(buffer, 0, bytesRead);
		} while (bytesRead > 0);
		return;
	}

	private int chunkSize(int chunkSize) {
		return chunkSize == 0 ? defaultChunkSize : chunkSize;
	}
}