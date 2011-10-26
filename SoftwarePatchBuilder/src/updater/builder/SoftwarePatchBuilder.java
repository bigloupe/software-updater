package updater.builder;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import updater.builder.patch.Creator;
import updater.builder.util.AESKey;
import updater.builder.util.KeyGenerator;
import updater.builder.util.RSAKey;
import updater.builder.util.Util;
import updater.patch.PatchLogWriter;
import updater.patch.Patcher;
import updater.patch.PatcherListener;
import updater.script.InvalidFormatException;
import watne.seis720.project.KeySize;
import watne.seis720.project.Mode;
import watne.seis720.project.Padding;
import watne.seis720.project.WatneAES_Implementer;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class SoftwarePatchBuilder {

    static {
        // set debug mode
        System.setProperty("SyntaxHighlighterDebugMode", "false");
    }

    protected SoftwarePatchBuilder() {
    }

    public static void main(String[] args) {
        Option keygen = OptionBuilder.hasArgs(2).withArgName("method key-len").withValueSeparator(' ').
                withDescription("AES|RSA for method, generate encryption key in XML format with specified key length in bits").create("keygen");
        Option output = OptionBuilder.hasArg().withArgName("file-path").withDescription("specify the file to output").withLongOpt("output").create("o");
        Option renewAESIV = OptionBuilder.hasArg().withArgName("file-path").withDescription("renew the IV in the AES key XML file").create("renew");
        Option extractPatchXML = OptionBuilder.hasArg().withArgName("file-path").
                withDescription("extract the xml file out from the patch").create("extract");
        Option catalog = OptionBuilder.hasArgs(2).withArgName("mode file-path").
                withDescription("e|d for mode, e means encrypt, d means decrypt, file-path is the input catalog file").withValueSeparator(' ').create("catalog");
        Option key = OptionBuilder.hasArg().withArgName("file-path").
                withDescription("specify the key file to use").withLongOpt("key").create("k");
        Option patch = OptionBuilder.hasArgs(2).withArgName("old new").
                withDescription("create a patch for upgrade from 'old' to 'new', 'old' is the directory to the old version of software, 'new' is to the new version").withValueSeparator(' ').create("patch");
        Option full = OptionBuilder.hasArg().withArgName("folder").
                withDescription("create a full patch that suitable for upgrade from all other version, folder if the directory of the software").create("full");
        Option sha256 = OptionBuilder.hasArg().withArgName("file-path").
                withDescription("Generate the SHA-256 checksum of specified file").create("sha256");
        Option doPatch = OptionBuilder.hasArgs(2).withArgName("folder patch").withValueSeparator(' ').
                withDescription("Apply the patch to the specified folder").create("do");
        Option from = OptionBuilder.hasArg().withArgName("version").
                withDescription("specify the version-from").withLongOpt("from").create("f");
        Option fromSubsequent = OptionBuilder.hasArg().withArgName("version").
                withDescription("specify the version-from-subsequent").withLongOpt("from-sub").create("fs");
        Option to = OptionBuilder.hasArg().withArgName("version").
                withDescription("specify the version-from").withLongOpt("to").create("t");
        Option help = new Option("h", "help", false, "print this message");
        Option version = new Option("v", "version", false, "show the version of the software");

        Options options = new Options();
        options.addOption(keygen);
        options.addOption(output);
        options.addOption(renewAESIV);
        options.addOption(extractPatchXML);
        options.addOption(catalog);
        options.addOption(key);
        options.addOption(patch);
        options.addOption(full);
        options.addOption(sha256);
        options.addOption(doPatch);
        options.addOption(from);
        options.addOption(fromSubsequent);
        options.addOption(to);
        options.addOption(help);
        options.addOption(version);

        CommandLineParser parser = new GnuParser();
        try {
            CommandLine line = parser.parse(options, args);
            if (line.hasOption("help")) {
                showHelp(options);
            } else if (line.hasOption("keygen")) {
                keygen(line, options);
            } else if (line.hasOption("renew")) {
                renew(line, options);
            } else if (line.hasOption("extract")) {
                extract(line, options);
            } else if (line.hasOption("catalog")) {
                catalog(line, options);
            } else if (line.hasOption("patch")) {
                patch(line, options);
            } else if (line.hasOption("full")) {
                full(line, options);
            } else if (line.hasOption("sha256")) {
                sha256(line, options);
            } else if (line.hasOption("do")) {
                doPatch(line, options);
            } else if (line.hasOption("version")) {
                version();
            } else {
                version();
                System.out.println();
                showHelp(options);
            }
        } catch (ParseException ex) {
            System.out.println(ex.getMessage());
            showHelp(options);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public static void renew(CommandLine line, Options options) throws ParseException, IOException {
        String renewArg = line.getOptionValue("renew");

        try {
            KeyGenerator.renewAESIV(new File(renewArg));
        } catch (IOException ex) {
            throw new IOException("Error occurred when reading/writing to " + renewArg);
        } catch (InvalidFormatException ex) {
            throw new IOException("The file specified is not a valid AES key XML file.");
        }

        System.out.println("AES IV renewal succeed.");
    }

    public static void keygen(CommandLine line, Options options) throws ParseException, IOException {
        if (!line.hasOption("output")) {
            throw new IOException("Please specify the path to output the key file using --output");
        }

        String[] keygenArgs = line.getOptionValues("keygen");
        String outputArg = line.getOptionValue("output");

        if (keygenArgs.length != 2) {
            throw new ParseException("Wrong arguments for 'keygen'.");
        }
        if (!keygenArgs[0].equals("AES") && !keygenArgs[0].equals("RSA")) {
            throw new ParseException("Key generation only support AES and RSA.");
        }

        int keySize = 0;
        try {
            keySize = Integer.parseInt(keygenArgs[1]);
            if (keySize % 8 != 0) {
                throw new ParseException("Key length should be a multiple of 8.");
            }
        } catch (NumberFormatException ex) {
            throw new ParseException("Key length should be a valid integer: " + keygenArgs[1]);
        }

        System.out.println("Method: " + keygenArgs[0]);
        System.out.println("Key size: " + keySize);
        System.out.println("Output path: " + outputArg);
        System.out.println();

        try {
            if (keygenArgs[0].equals("AES")) {
                KeyGenerator.generateAES(keySize, new File(outputArg));
            } else {
                KeyGenerator.generateRSA(keySize, new File(outputArg));
            }
        } catch (IOException ex) {
            throw new IOException("Error occurred when outputting to " + outputArg);
        }

        System.out.println("Key generated and saved to " + outputArg);
    }

    public static void extract(CommandLine line, Options options) throws ParseException, IOException {
        if (!line.hasOption("output")) {
            throw new IOException("Please specify the path to output the XML file using --output");
        }

        String extractArg = line.getOptionValue("extract");
        String outputArg = line.getOptionValue("output");

        System.out.println("Patch path: " + extractArg);
        System.out.println("Path to save the extracted XML file: " + outputArg);
        System.out.println();

        AESKey aesKey = null;
        if (line.hasOption("key")) {
            try {
                aesKey = AESKey.read(Util.readFile(new File(line.getOptionValue("key"))));
            } catch (InvalidFormatException ex) {
                throw new IOException("The file is not a valid AES key file: " + line.hasOption("key"));
            }
            if (aesKey.getKey().length != 32) {
                throw new IOException("Currently only support 256 bits AES key.");
            }
        }
        File patchFile = new File(extractArg);
        File decryptedPatchFile = new File("tmp/" + patchFile.getName() + ".decrypted");
        decryptedPatchFile.delete();
        decryptedPatchFile.deleteOnExit();

        try {
            Creator.extractXMLFromPatch(patchFile, new File(outputArg), aesKey, decryptedPatchFile);
        } catch (InvalidFormatException ex) {
            throw new IOException(ex);
        }

        System.out.println("Extract completed.");
    }

    public static void catalog(CommandLine line, Options options) throws ParseException, IOException {
        if (!line.hasOption("key")) {
            throw new IOException("Please specify the key file to use using --key");
        }
        if (!line.hasOption("output")) {
            throw new IOException("Please specify the path to output the XML file using --output");
        }

        String[] catalogArgs = line.getOptionValues("catalog");
        String keyArg = line.getOptionValue("key");
        String outputArg = line.getOptionValue("output");

        if (catalogArgs.length != 2) {
            throw new ParseException("Wrong arguments for 'catalog'.");
        }
        if (!catalogArgs[0].equals("e") && !catalogArgs[0].equals("d")) {
            throw new ParseException("Catalog mode should be either 'e' or 'd' but not " + catalogArgs[0]);
        }

        RSAKey rsaKey = null;
        try {
            rsaKey = RSAKey.read(Util.readFile(new File(keyArg)));
        } catch (InvalidFormatException ex) {
            throw new IOException("The file is not a valid RSA key file: " + catalogArgs[1]);
        }

        System.out.println("Mode: " + (catalogArgs[0].equals("e") ? "encrypt" : "decrypt"));
        System.out.println("Catalog file: " + catalogArgs[1]);
        System.out.println("Key file: " + keyArg);
        System.out.println("Output: " + outputArg);
        System.out.println();

        try {
            if (catalogArgs[0].equals("e")) {
                Creator.encryptCatalog(new File(catalogArgs[1]), new File(outputArg), new BigInteger(rsaKey.getModulus()), new BigInteger(rsaKey.getPrivateExponent()));
            } else {
                Creator.decryptCatalog(new File(catalogArgs[1]), new File(outputArg), new BigInteger(rsaKey.getModulus()), new BigInteger(rsaKey.getPublicExponent()));
            }
        } catch (IOException ex) {
            throw new IOException("Error occurred when reading from " + catalogArgs[1] + " or outputting to " + outputArg);
        }

        System.out.println("Manipulation succeed.");
    }

    public static void patch(CommandLine line, Options options) throws ParseException, IOException {
        if (!line.hasOption("output")) {
            throw new IOException("Please specify the path to output the patch using --output");
        }
        if (!line.hasOption("from")) {
            throw new IOException("Please specify the version number of the old version using --from");
        }
        if (!line.hasOption("to")) {
            throw new IOException("Please specify the version number of the new version using --to");
        }

        String[] patchArgs = line.getOptionValues("patch");
        String outputArg = line.getOptionValue("output");
        String fromArg = line.getOptionValue("from");
        String toArg = line.getOptionValue("to");

        if (patchArgs.length != 2) {
            throw new ParseException("Wrong arguments for 'patch'.");
        }

        System.out.println("Old software version: " + fromArg);
        System.out.println("Old software directory: " + patchArgs[0]);
        System.out.println("New software version: " + toArg);
        System.out.println("New software directory: " + patchArgs[1]);
        System.out.println("Path to save the generated patch: " + outputArg);
        if (line.hasOption("key")) {
            System.out.println("AES key file: " + line.getOptionValue("key"));
        }
        System.out.println();

        AESKey aesKey = null;
        if (line.hasOption("key")) {
            try {
                aesKey = AESKey.read(Util.readFile(new File(line.getOptionValue("key"))));
            } catch (InvalidFormatException ex) {
                throw new IOException("The file is not a valid AES key file: " + line.hasOption("key"));
            }
            if (aesKey.getKey().length != 32) {
                throw new IOException("Currently only support 256 bits AES key.");
            }
        }
        File patchFile = new File(outputArg);
        File encryptedPatchFile = new File("tmp/" + patchFile.getName() + ".encrypted");
        encryptedPatchFile.delete();
        encryptedPatchFile.deleteOnExit();

        try {
            Creator.createPatch(new File(patchArgs[0]), new File(patchArgs[1]), new File("tmp/"), patchFile, -1, fromArg, toArg, aesKey, encryptedPatchFile);
        } catch (IOException ex) {
            throw ex;
        }

        System.out.println("Patch created.");
    }

    public static void full(CommandLine line, Options options) throws ParseException, IOException {
        if (!line.hasOption("output")) {
            throw new IOException("Please specify the path to output the patch using --output");
        }
        if (!line.hasOption("from") && !line.hasOption("from-sub")) {
            throw new IOException("Please specify the version number of the old version --from or --from-sub");
        }
        if (!line.hasOption("to")) {
            throw new IOException("Please specify the version number of the new version using --to");
        }

        String fullArg = line.getOptionValue("full");
        String outputArg = line.getOptionValue("output");
        String fromArg = line.getOptionValue("from");
        String fromSubsequentArg = line.getOptionValue("from-sub");
        String toArg = line.getOptionValue("to");

        System.out.println("Software version: " + toArg);
        System.out.println("Software directory: " + fullArg);
        if (fromArg != null) {
            System.out.println("For software with version == " + fromArg);
        }
        if (fromSubsequentArg != null) {
            System.out.println("For software with version >= " + fromSubsequentArg);
        }
        System.out.println("Path to save the generated patch: " + outputArg);
        System.out.println();

        AESKey aesKey = null;
        if (line.hasOption("key")) {
            try {
                aesKey = AESKey.read(Util.readFile(new File(line.getOptionValue("key"))));
            } catch (InvalidFormatException ex) {
                throw new IOException("The file is not a valid AES key file: " + line.hasOption("key"));
            }
            if (aesKey.getKey().length != 32) {
                throw new IOException("Currently only support 256 bits AES key.");
            }
        }
        File patchFile = new File(outputArg);
        File encryptedPatchFile = new File("tmp/" + patchFile.getName() + ".encrypted");
        encryptedPatchFile.delete();
        encryptedPatchFile.deleteOnExit();

        File tempDir = new File("tmp/" + System.nanoTime());
        tempDir.mkdirs();

        Creator.createFullPatch(new File(fullArg), tempDir, new File(outputArg), -1, fromArg, fromSubsequentArg, toArg, aesKey, encryptedPatchFile);

        Util.truncateFolder(tempDir);
        tempDir.delete();

        System.out.println("Patch created.");
    }

    public static void sha256(CommandLine line, Options options) throws ParseException, IOException {
        String sha256Arg = line.getOptionValue("sha256");
        String outputArg = line.getOptionValue("output");

        System.out.println("File: " + sha256Arg);
        if (outputArg != null) {
            System.out.println("Output file: " + outputArg);
        }
        System.out.println();

        String sha256 = Util.getSHA256String(new File(sha256Arg));
        if (outputArg != null) {
            Util.writeFile(new File(outputArg), sha256);
        }
        System.out.println("Checksum: " + sha256);
    }

    public static void doPatch(CommandLine line, Options options) throws ParseException, IOException {
        String[] doArgs = line.getOptionValues("do");

        if (doArgs.length != 2) {
            throw new ParseException("Wrong arguments for 'do'.");
        }

        System.out.println("Target folder: " + doArgs[0]);
        System.out.println("Patch path: " + doArgs[1]);
        System.out.println();

        File patchFile = new File(doArgs[1]);

        AESKey aesKey = null;
        if (line.hasOption("key")) {
            try {
                aesKey = AESKey.read(Util.readFile(new File(line.getOptionValue("key"))));
            } catch (InvalidFormatException ex) {
                throw new IOException("The file is not a valid AES key file: " + line.hasOption("key"));
            }
            if (aesKey.getKey().length != 32) {
                throw new IOException("Currently only support 256 bits AES key.");
            }

            File decryptedPatchFile = new File("tmp/" + patchFile.getName() + ".decrypted");
            decryptedPatchFile.delete();
            decryptedPatchFile.deleteOnExit();

            try {
                WatneAES_Implementer aesCipher = new WatneAES_Implementer();
                aesCipher.setMode(Mode.CBC);
                aesCipher.setPadding(Padding.PKCS5PADDING);
                aesCipher.setKeySize(KeySize.BITS256);
                aesCipher.setKey(aesKey.getKey());
                aesCipher.setInitializationVector(aesKey.getIV());
                aesCipher.decryptFile(patchFile, decryptedPatchFile);
            } catch (Exception ex) {
                throw new IOException("Error occurred when encrypting the patch: " + ex.getMessage());
            }

            patchFile = decryptedPatchFile;
        }

        File tempDir = new File("tmp/" + System.nanoTime());
        tempDir.mkdirs();

        PatchLogWriter logger = new PatchLogWriter(new File(tempDir.getAbsolutePath() + "/action.log"));
        Patcher patcher = new Patcher(new PatcherListener() {

            @Override
            public void patchProgress(int percentage, String message) {
                System.out.println(percentage + "%, " + message);
            }

            @Override
            public void patchFinished(boolean succeed) {
                System.out.println("Patch result: " + (succeed ? "Succeed" : "Failed"));
            }

            @Override
            public void patchEnableCancel(boolean enable) {
            }
        }, logger, new File(doArgs[0]), tempDir);
        patcher.doPatch(patchFile, 0, 0);
        logger.close();

        Util.truncateFolder(tempDir);
        tempDir.delete();

        System.out.println();
        System.out.println("Patch applied successfully.");
    }

    public static void version() {
        System.out.println("Software Updater - Patch Builder\r\nversion: 0.9.0 beta");
    }

    public static void showHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("builder", options);
    }
}
