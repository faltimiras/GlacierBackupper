package cat.altimiras.glacier.backupper;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FileChunker {

	private static int ONE_MB = 1024*1024;


	public static PartIterator partitionate(Path file, int partSize) {
		return new PartIterator(file.toFile(), partSize);
	}

	public static class PartIterator implements Iterator<Chunk> {

		private long size;
		private File file;
		private int partSize;
		private long currentPos;

		public PartIterator(File file, int partSize) {
			this.file = file;
			this.partSize = partSize;
			this.size = file.length();
		}

		public long getExpectedChunks() {
			return Double.valueOf(Math.ceil(size / partSize)).longValue();
		}

		@Override
		public boolean hasNext() {
			return currentPos < size;
		}

		@Override
		public Chunk next() {

			try (FileInputStream fis = new FileInputStream(file)) {

				MessageDigest md = MessageDigest.getInstance("SHA-256");

				long pending = size - currentPos;
				long toRead = Math.min(pending, partSize);

				ByteBuffer bb = ByteBuffer.allocate((int) toRead);
				fis.getChannel().read(bb, currentPos);

				//calculate checksum
				//https://docs.aws.amazon.com/amazonglacier/latest/dev/checksum-calculations.html
				List<byte[]> checksums = new ArrayList<>();
				int processedChecksum = 0;
				while ( processedChecksum < toRead){

					long pendingBatchChecksum = toRead - processedChecksum;
					int checksumBatch = (int)Math.min(pendingBatchChecksum, ONE_MB);
					md.update(bb.array(), processedChecksum, checksumBatch);
					checksums.add( md.digest());
					processedChecksum+= checksumBatch;
				}

				Chunk chunk = new Chunk(currentPos, bb.array(), Hash.calculateTreeHash(checksums));

				currentPos += toRead;
				return chunk;
			} catch (Exception e) {
				return null;
			}
		}
	}

	public static class Chunk {

		private long start;
		private byte[] content;
		private byte[] checksum;

		public Chunk(long start, byte[] content, byte[] checksum) {
			this.start = start;
			this.content = content;
			this.checksum = checksum;
		}

		public byte[] getContent() {
			return content;
		}

		public byte[]  getChecksum() {
			return checksum;
		}

		public long getStart() {
			return start;
		}
	}
}