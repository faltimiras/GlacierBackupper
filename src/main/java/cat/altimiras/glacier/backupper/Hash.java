package cat.altimiras.glacier.backupper;

import software.amazon.awssdk.utils.BinaryUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

class Hash {

	static String calculateTreeHash(List<byte[]> checksums) throws Exception {

		/*
		 * The tree hash algorithm involves concatenating adjacent pairs of
		 * individual checksums, then taking the checksum of the resulting bytes
		 * and storing it, then recursing on this new list until there is only
		 * one element. Any final odd-numbered parts at each step are carried
		 * over to the next iteration as-is.
		 */
		List<byte[]> hashes = new ArrayList<byte[]>();
		hashes.addAll(checksums);
		while (hashes.size() > 1) {
			List<byte[]> treeHashes = new ArrayList<byte[]>();
			for (int i = 0; i < hashes.size() / 2; i++) {
				byte[] firstPart = hashes.get(2 * i);
				byte[] secondPart = hashes.get(2 * i + 1);
				byte[] concatenation = new byte[firstPart.length + secondPart.length];
				System.arraycopy(firstPart, 0, concatenation, 0, firstPart.length);
				System.arraycopy(secondPart, 0, concatenation, firstPart.length, secondPart.length);
				try {
					treeHashes.add(computeSHA256Hash(concatenation));
				} catch (Exception e) {
					throw new Exception("Unable to compute hash", e);
				}
			}
			if (hashes.size() % 2 == 1) {
				treeHashes.add(hashes.get(hashes.size() - 1));
			}
			hashes = treeHashes;
		}

		return BinaryUtils.toHex(hashes.get(0));
	}

	static byte[] computeSHA256Hash(byte[] data) throws NoSuchAlgorithmException, IOException {
		BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(data));
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			byte[] buffer = new byte[16384];
			int bytesRead = -1;
			while ((bytesRead = bis.read(buffer, 0, buffer.length)) != -1) {
				messageDigest.update(buffer, 0, bytesRead);
			}
			return messageDigest.digest();
		} finally {
			try {
				bis.close();
			} catch (Exception e) {
			}
		}
	}
}
