package cat.altimiras.glacier.backupper;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
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

class GlacierManager {

	final private AwsCredentialsProvider awsCredentialsProvider;

	final private int defaultChunkSize = 1024*1024;//4194304; // 50 * 1024* 1024; 50MB

	public GlacierManager(String awsKey, String awsSecret) {
		AwsBasicCredentials credentials = AwsBasicCredentials.create(awsKey, awsSecret);
		this.awsCredentialsProvider = StaticCredentialsProvider.create(credentials);
	}
	//https://github.com/aws/aws-sdk-java/blob/master/aws-java-sdk-glacier/src/main/java/com/amazonaws/services/glacier/transfer/ArchiveTransferManager.java

	public String upload(String name, Path file, String region, String vault, int chunkSize) throws Exception {

		chunkSize = chunkSize(chunkSize);
		GlacierClient glacier = getClient(region);

		InitiateMultipartUploadRequest initiateMultipartUploadRequest = InitiateMultipartUploadRequest.builder()
				//.accountId(account)
				.vaultName(vault)
				.partSize(String.valueOf(chunkSize))
				.archiveDescription(name)
				.build();

		InitiateMultipartUploadResponse initiateMultipartUploadResponse = glacier.initiateMultipartUpload(initiateMultipartUploadRequest);
		String uploadId = initiateMultipartUploadResponse.uploadId();

		List<byte[]> partChecksums = new ArrayList<>();

		FileChunker.PartIterator it = FileChunker.partitionate(file, chunkSize);
		while (it.hasNext()) {

			FileChunker.Chunk chunk = it.next();

			String checksum = BinaryUtils.toHex(chunk.getChecksum());
			partChecksums.add(chunk.getChecksum());

			UploadMultipartPartRequest uploadMultipartPartRequest = UploadMultipartPartRequest.builder()
					.uploadId(uploadId)
					//.accountId(account)
					.vaultName(vault)
					.range(String.format("bytes %s-%s/*", chunk.getStart(), chunk.getStart() + chunk.getContent().length - 1))
					.checksum(checksum)
					.build();

			glacier.uploadMultipartPart(uploadMultipartPartRequest, RequestBody.fromBytes(chunk.getContent()));
			System.out.println("Chunk uploaded");
		}

		CompleteMultipartUploadRequest completeMultipartUploadRequest = CompleteMultipartUploadRequest.builder()
				.uploadId(uploadId)
				//.accountId(account)
				.vaultName(vault)
				.archiveSize(String.valueOf(file.toFile().length()))
				.checksum(Hash.calculateTreeHash(partChecksums))
				.build();

		CompleteMultipartUploadResponse completeMultipartUploadResponse = glacier.completeMultipartUpload(completeMultipartUploadRequest);
		System.out.println("Uploaded completed");
		return completeMultipartUploadResponse.archiveId();
	}


	public String askToDownload(Item item) throws Exception {

		GlacierClient glacier = getClient(item.getRegion());

		JobParameters jobParameters = JobParameters.builder()
				.archiveId(item.getArchiveId())
				.type("archive-retrieval")
				.build();

		InitiateJobRequest initiateJobRequest = InitiateJobRequest.builder()
				.jobParameters(jobParameters)
				//.accountId(account)
				.vaultName(item.getVault())
				.build();

		InitiateJobResponse initiateJobResponse = glacier.initiateJob(initiateJobRequest);
		return initiateJobResponse.jobId();
	}


	public void download(Job job, Path target, int chunkSize) throws Exception {

		chunkSize = chunkSize(chunkSize);
		GlacierClient glacier = getClient(job.getRegion());

		DescribeJobRequest describeJobRequest = DescribeJobRequest.builder()
				.jobId(job.getJobId())
				//	.accountId(account)
				.vaultName(job.getVault())
				.build();

		DescribeJobResponse describeJobResponse = glacier.describeJob(describeJobRequest);
		long size = describeJobResponse.archiveSizeInBytes();

		RandomAccessFile output = new RandomAccessFile(target.toFile(), "rw");

		long currentPos = 0;
		while (currentPos < size) {
			long pending = size - currentPos;
			long toRead = Math.min(pending, chunkSize);


			GetJobOutputRequest getJobOutputRequest = GetJobOutputRequest.builder()
					.jobId(job.getJobId())
				//	.accountId(account)
					.vaultName(job.getVault())
					.range("bytes=" + currentPos + "-" + (currentPos + toRead - 1))
					.build();

			ResponseInputStream<GetJobOutputResponse> getJobOutputResponse = glacier.getJobOutput(getJobOutputRequest);
			appendToFile(output, getJobOutputResponse);
			System.out.println("Download part");

			currentPos += toRead;
		}

		System.out.println("Whole file has been downloaded into:" + target);
	}


	public boolean isReadyDownload(Job job) throws Exception {

		GlacierClient glacier = getClient(job.getRegion());

		DescribeJobRequest describeJobRequest = DescribeJobRequest.builder()
				.jobId(job.getJobId())
				//.accountId(account)
				.vaultName(job.getVault())
				.build();

		DescribeJobResponse describeJobResponse = glacier.describeJob(describeJobRequest);
		return describeJobResponse.statusCode() == StatusCode.SUCCEEDED;
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

	private int chunkSize(int chunkSize){
		return chunkSize == 0 ? defaultChunkSize : chunkSize;
	}
}
