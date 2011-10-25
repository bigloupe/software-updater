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
import updater.builder.patch.Creater;
import updater.builder.util.AESKey;
import updater.builder.util.KeyGenerator;
import updater.builder.util.RSAKey;
import updater.builder.util.Util;
import updater.script.InvalidFormatException;
import watne.seis720.project.KeySize;
import watne.seis720.project.Mode;
import watne.seis720.project.Padding;
import watne.seis720.project.WatneAES_Implementer;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class SoftwarePatchBuilder {

    protected SoftwarePatchBuilder() {
    }

    public static void main(String[] args) {
        Option keygen = OptionBuilder.hasArgs(2).withArgName("method key-len").withValueSeparator(' ').
                withDescription("AES|RSA for method, generate encryption key in XML format with specified key length in bits").create("keygen");
        Option output = OptionBuilder.hasArg().withArgName("file-path").withDescription("specify the file to output").create("output");
        Option renewAESIV = OptionBuilder.hasArg().withArgName("file-path").withDescription("renew the IV in the AES key XML file").create("renew");
        Option extractPatchXML = OptionBuilder.hasArg().withArgName("file-path").
                withDescription("extract the xml file out from the patch").create("extract");
        Option catalog = OptionBuilder.hasArgs(2).withArgName("mode file-path").
                withDescription("e|d for mode, e means encrypt, d means decrypt, file-path is the input catalog file").withValueSeparator(' ').create("catalog");
        Option key = OptionBuilder.hasArg().withArgName("file-path").
                withDescription("specify the key file to use").create("key");
        Option patch = OptionBuilder.hasArgs(2).withArgName("old new").
                withDescription("create a patch for upgrade from 'old' to 'new', 'old' is the directory to the old version of software, 'new' is to the new version").withValueSeparator(' ').create("patch");
        Option full = OptionBuilder.hasArg().withArgName("folder").
                withDescription("create a full patch that suitable for upgrade from all other version, folder if the directory of the software").create("full");
        // add - add patch to catalog
        Option from = new Option("from", "Specify the version-from");
        Option to = new Option("to", "Specify the version-to");
        Option help = new Option("h", "help", false, "print this message");
        Option version = new Option("v", "version", false, "show the version of the software");

        Options options = new Options();
        options.addOption(keygen);
        options.addOption(output);
        options.addOption(renewAESIV);
//        options.addOption(extractPatchXML);
        options.addOption(catalog);
        options.addOption(key);
        options.addOption(patch);
        options.addOption(full);
        options.addOption(from);
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
            throw new IOException("Please specify the path to output the key file using -output");
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
            throw new IOException("Please specify the path to output the XML file using -output");
        }

        String extractArg = line.getOptionValue("extract");
        String outputArg = line.getOptionValue("output");

        System.out.println("Patch path: " + extractArg);
        System.out.println("Path to save the extracted XML file: " + outputArg);
        System.out.println();

        // process

        System.out.println("Not implemented yet.");
    }

    public static void catalog(CommandLine line, Options options) throws ParseException, IOException {
        if (!line.hasOption("key")) {
            throw new IOException("Please specify the key file to use using --key");
        }
        if (!line.hasOption("output")) {
            throw new IOException("Please specify the path to output the XML file using -output");
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
                Creater.encryptCatalog(new File(catalogArgs[1]), new File(outputArg), new BigInteger(rsaKey.getModulus()), new BigInteger(rsaKey.getPrivateExponent()));
            } else {
                Creater.decryptCatalog(new File(catalogArgs[1]), new File(outputArg), new BigInteger(rsaKey.getModulus()), new BigInteger(rsaKey.getPublicExponent()));
            }
        } catch (IOException ex) {
            throw new IOException("Error occurred when reading from " + catalogArgs[1] + " or outputting to " + outputArg);
        }

        System.out.println("Manipulation succeed.");
    }

    public static void patch(CommandLine line, Options options) throws ParseException, IOException {
        if (!line.hasOption("output")) {
            throw new IOException("Please specify the path to output the patch using -output");
        }
        if (!line.hasOption("from")) {
            throw new IOException("Please specify the version number of the old version using -from");
        }
        if (!line.hasOption("to")) {
            throw new IOException("Please specify the version number of the new version using -to");
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

        try {
            Creater.createPatch(new File(patchArgs[0]), new File(patchArgs[1]), new File("tmp/"), new File(outputArg), 1, fromArg, toArg);
        } catch (IOException ex) {
            throw ex;
        }

        if (line.hasOption("key")) {
            AESKey aesKey = null;
            try {
                aesKey = AESKey.read(Util.readFile(new File(line.getOptionValue("key"))));
            } catch (InvalidFormatException ex) {
                throw new IOException("The file is not a valid AES key file: " + line.hasOption("key"));
            }
            if (aesKey.getKey().length != 32) {
                throw new IOException("Currently only support 256 bits AES key.");
            }

            File patchFile = new File(outputArg);
            File encryptedPatchFile = new File(outputArg + ".encrypted");
            encryptedPatchFile.delete();

            try {
                WatneAES_Implementer aesCipher = new WatneAES_Implementer();
                aesCipher.setMode(Mode.CBC);
                aesCipher.setPadding(Padding.PKCS5PADDING);
                aesCipher.setKeySize(KeySize.BITS256);
                aesCipher.setKey(aesKey.getKey());
                aesCipher.setInitializationVector(aesKey.getIV());
                aesCipher.encryptFile(patchFile, encryptedPatchFile);
            } catch (Exception ex) {
                throw new IOException("Error occurred when encrypting the patch: " + ex.getMessage());
            }

            patchFile.delete();
            encryptedPatchFile.renameTo(patchFile);
        }

        System.out.println("Patch created.");
    }

    public static void full(CommandLine line, Options options) throws ParseException, IOException {
        if (!line.hasOption("output")) {
            throw new IOException("Please specify the path to output the patch using -output");
        }
        if (!line.hasOption("from")) {
            throw new IOException("Please specify the least version number for old version using -from");
        }
        if (!line.hasOption("to")) {
            throw new IOException("Please specify the version number of the new version using -to");
        }

        String fullArg = line.getOptionValue("full");
        String outputArg = line.getOptionValue("output");
        String fromArg = line.getOptionValue("from");
        String toArg = line.getOptionValue("to");

        System.out.println("Software version: " + toArg);
        System.out.println("Software directory: " + fullArg);
        System.out.println("For software with version >= " + fromArg);
        System.out.println("Path to save the generated patch: " + outputArg);
        System.out.println();

        // process

        System.out.println("Not implemented yet.");
    }

    public static void version() {
        System.out.println("Software Updater - Patch Builder\r\nversion: 1.0.0");
    }

    public static void showHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("builder", options);
    }
}
