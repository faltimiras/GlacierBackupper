package cat.altimiras.glacier.backupper;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Iterator;

public class FileChunker {


	public static PartIterator partitionate (Path file, int partSize) throws Exception {
		return new PartIterator(file.toFile(), partSize);
	}

	public static class PartIterator implements Iterator<Chunk> {

		private long size;
		private File file;
		private  int partSize;
		private long currentPos;

		public PartIterator(File file, int partSize) throws Exception{
			this.file = file;
			this.partSize = partSize;
			this.size = file.length();
		}

		@Override
		public boolean hasNext() {
			return currentPos < size;
		}

		@Override
		public Chunk next()  {

			try(FileInputStream fis = new FileInputStream(file)){

				MessageDigest md = MessageDigest.getInstance("SHA-256");

				long pending = size - currentPos;
				long toRead = Math.min(pending, partSize);

				ByteBuffer bb = ByteBuffer.allocate((int)toRead);
				fis.getChannel().read(bb, currentPos);

				//calculate checksum
				md.update(bb.array(), 0, (int)toRead);

				Chunk chunk = new Chunk(currentPos, bb.array(), md.digest());

				currentPos+= toRead;
				return chunk;
			}
			catch (Exception e) {
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

		public byte[] getChecksum() {
			return checksum;
		}

		public long getStart() {
			return start;
		}
	}
}
