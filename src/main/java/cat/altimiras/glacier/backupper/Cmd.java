package cat.altimiras.glacier.backupper;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Cmd {

	final static Logger logger = LoggerFactory.getLogger(GlacierBackupper.class);

	private static Options options = new Options();

	static {
		options.addRequiredOption("i", "inventory", true, "Path to inventory.json");
		options.addOption("k", "aws-key", true, "AWS key");
		options.addOption("s", "aws-secret", true, "AWS secret");
		options.addOption("x", "verbose", false, "Verbose mode");

		OptionGroup operations = new OptionGroup();
		operations.addOption(new Option("u", "upload", false, "Upload command"));
		operations.addOption(new Option("d", "download", false, "Download command"));
		operations.addOption(new Option("rd", "request-download", false, "Request download command"));
		operations.addOption(new Option("ps", "job-status", false, "Job status command"));
		operations.addOption(new Option("ls", "list", false, "List items command"));
		operations.addOption(new Option("rm", "remove", false, "Remove item command"));
		operations.setRequired(true);
		options.addOptionGroup(operations);

		options.addOption("f", "file", true, "File to upload");
		options.addOption("v", "vault", true, "AWS vault");
		options.addOption("r", "region", true, "AWS region");
		options.addOption("n", "name", true, "Name");
		options.addOption("c", "chunk-size", true, "Chunk size in MB must be power of 2: 2,4,8,16 ...");
		options.addOption("t", "target", true, "Target download");
		options.addOption("nr", "no-remove", false, "Keep job into the inventory");
		options.addOption("u", "urgent", false, "Work in 'Standard' Glacier mode. 'Bulk' is default");
	}

	public static void main(String... args) throws Exception {


		CommandLine line = parseLine(args);

		boolean verbose = line.hasOption("x");

		try {
			Path inventoryPath = Paths.get(line.getOptionValue("i"));
			String awsKey = line.getOptionValue("k");
			String awsSecret = line.getOptionValue("s");

			GlacierBackupper glacierBackupper = new GlacierBackupper(inventoryPath, awsKey, awsSecret);

			if (line.hasOption("u")) { //upload
				validateUpload(line);
				glacierBackupper.upload(
						line.getOptionValue("n"),
						line.getOptionValue("v"),
						line.getOptionValue("r"),
						Paths.get(line.getOptionValue("f")),
						Integer.valueOf(line.getOptionValue("c", "0")) * 1024 * 1024
				);

			} else if (line.hasOption("d")) { //download
				validateDownload(line);
				glacierBackupper.download(
						line.getOptionValue("n"),
						Paths.get(line.getOptionValue("t")),
						Integer.valueOf(line.getOptionValue("c", "0")) * 1024 * 1024,
						line.hasOption("nr")
				);

			} else if (line.hasOption("rd")) { //request download
				validateName(line);
				glacierBackupper.createDownloadJob(
						line.getOptionValue("n"),
						line.hasOption("u")
				);
			} else if (line.hasOption("ps")) { //status jobs
				glacierBackupper.jobStatus();
			} else if (line.hasOption("ls")) { // list items
				glacierBackupper.list();
			} else if (line.hasOption("rm")) {
				validateName(line);
				glacierBackupper.remove(
						line.getOptionValue("n")
				);
			}
		} catch (Exception e) {
			if (verbose) {
				logger.error("Error {}", e.getMessage(), e);
			} else {
				logger.error("Error {}", e.getMessage());
			}
		}
	}

	private static CommandLine parseLine( String... args) {
		try {
			CommandLineParser parser = new DefaultParser();
			return parser.parse(options, args, true);
		} catch (ParseException exp) {
			logger.error(exp.getMessage());
			HelpFormatter helpFormatter = new HelpFormatter();
			helpFormatter.printHelp("java -jar GlacierBackupper-1.0.jar ", options, true);
			System.exit(1);
		}
		return null;
	}

	private static void validateUpload(CommandLine line){
		if (!line.hasOption("f") || !line.hasOption("v") || !line.hasOption("r") ) {
			logger.error("-f <file to upload> -r <aws region> -v <vault> are required");
			System.exit(1);
		}

	}

	private static void validateDownload(CommandLine line){
		if (!line.hasOption("n") || !line.hasOption("t")) {
			logger.error("-n <file to download> -t <target file>");
			System.exit(1);
		}
	}

	private static void validateName(CommandLine line){
		if (!line.hasOption("n") ) {
			logger.error("-n <file to download>");
			System.exit(1);
		}
	}
}