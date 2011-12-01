package updater.builder;

import com.nothome.delta.Delta;
import com.nothome.delta.DiffWriter;
import com.nothome.delta.GDiffPatcher;
import com.nothome.delta.GDiffWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import updater.crypto.AESKey;
import updater.crypto.KeyGenerator;
import updater.crypto.RSAKey;
import updater.patch.PatchCreator;
import updater.patch.PatchExtractor;
import updater.patch.PatchPacker;
import updater.patch.Patcher;
import updater.patch.PatcherListener;
import updater.script.Client;
import updater.script.Patch;
import updater.util.CommonUtil;
import updater.util.XMLUtil;

/**
 * Tool that contain general functions to build the patch.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class SoftwarePatchBuilder {

    static {
        // set debug mode
        System.setProperty("SoftwareUpdaterDebugMode", "false");
    }
    /**
     * Indicate whether it is in debug mode or not.
     */
    protected final static boolean debug;

    static {
        String debugMode = System.getProperty("SoftwareUpdaterDebugMode");
        debug = debugMode == null || !debugMode.equals("true") ? false : true;
    }

    protected SoftwarePatchBuilder() {
    }

    public static void main(String[] args) {
        Options options = new Options();

        // utilities
        options.addOption(OptionBuilder.hasArg().withArgName("file").
                withDescription("generate SHA-256 checksum of the file").
                create("sha256"));
        // cipher key
        options.addOption(OptionBuilder.hasArgs(2).withArgName("method length").withValueSeparator(' ').
                withDescription("AES|RSA for 'method'; generate cipher key with specified key length in bits").
                create("genkey"));
        options.addOption(OptionBuilder.hasArg().withArgName("file").
                withDescription("renew the IV in the AES key file").
                create("renew"));
        // diff
        options.addOption(OptionBuilder.hasArgs(2).withArgName("old new").withValueSeparator(' ').
                withDescription("generate a binary diff file of 'new' from 'old'").
                create("diff"));
        options.addOption(OptionBuilder.hasArgs(2).withArgName("file patch").withValueSeparator(' ').
                withDescription("patch the 'file' with the 'patch'").
                create("diffpatch"));
        // compression
        options.addOption(OptionBuilder.hasArg().withArgName("file").
                withDescription("compress the 'file' using XZ/LZMA2").
                create("compress"));
        options.addOption(OptionBuilder.hasArg().withArgName("file").
                withDescription("decompress the 'file' using XZ/LZMA2").
                create("decompress"));

        // create & apply patch
        options.addOption(OptionBuilder.hasArgs(2).withArgName("folder patch").withValueSeparator(' ').
                withDescription("apply the patch to the specified folder").
                create("do"));
        options.addOption(OptionBuilder.hasArg().withArgName("folder").
                withDescription("create a full patch for upgrade from all version (unless specified)").
                create("full"));
        options.addOption(OptionBuilder.hasArgs(2).withArgName("old new").withValueSeparator(' ').
                withDescription("create a patch for upgrade from 'old' to 'new'; 'old' and 'new' are the directory of the two versions").
                create("patch"));

        // patch packer, extractor
        options.addOption(OptionBuilder.hasArgs(2).withArgName("file folder").withValueSeparator(' ').
                withDescription("extract the patch 'file' to the folder").
                create("extract"));
        options.addOption(OptionBuilder.hasArg().withArgName("folder").
                withDescription("pack the folder to a patch").
                create("pack"));

        // catalog
        options.addOption(OptionBuilder.hasArgs(2).withArgName("mode file").withValueSeparator(' ').
                withDescription("e|d for 'mode', e for encrypt, d for decrypt; 'file' is the catalog file").
                create("catalog"));

        // script validation
        options.addOption(OptionBuilder.hasArg().withArgName("file").
                withDescription("validate a XML script file").
                create("validate"));

        // subsidary options
        options.addOption(OptionBuilder.hasArg().withArgName("file").
                withDescription("specify output to which file").
                withLongOpt("output").create("o"));
        options.addOption(OptionBuilder.hasArg().withArgName("file").
                withDescription("specify the key file to use").
                withLongOpt("key").create("k"));
        options.addOption(OptionBuilder.hasArg().withArgName("version").
                withDescription("specify the version-from").
                withLongOpt("from").create("f"));
        options.addOption(OptionBuilder.hasArg().withArgName("version").
                withDescription("specify the version-from-subsequent").
                withLongOpt("from-sub").create("fs"));
        options.addOption(OptionBuilder.hasArg().withArgName("version").
                withDescription("specify the version-to").
                withLongOpt("to").create("t"));

        options.addOption(new Option("h", "help", false, "print this message"));
        options.addOption(new Option("v", "version", false, "show the version of this software"));
        options.addOption(new Option("vb", "verbose", false, "turn on verbose mode, output details when encounter error"));

        CommandLineParser parser = new GnuParser();
        CommandLine line = null;
        try {
            line = parser.parse(options, args);
            if (line.hasOption("sha256")) {
                sha256(line, options);
            } else if (line.hasOption("genkey")) {
                genkey(line, options);
            } else if (line.hasOption("renew")) {
                renew(line, options);
            } else if (line.hasOption("diff")) {
                diff(line, options);
            } else if (line.hasOption("diffpatch")) {
                diffpatch(line, options);
            } else if (line.hasOption("compress")) {
                compress(line, options);
            } else if (line.hasOption("decompress")) {
                decompress(line, options);
            } else if (line.hasOption("do")) {
                doPatch(line, options);
            } else if (line.hasOption("full")) {
                full(line, options);
            } else if (line.hasOption("patch")) {
                patch(line, options);
            } else if (line.hasOption("extract")) {
                extract(line, options);
            } else if (line.hasOption("pack")) {
                pack(line, options);
            } else if (line.hasOption("catalog")) {
                catalog(line, options);
            } else if (line.hasOption("validate")) {
                validate(line, options);
            } else if (line.hasOption("version")) {
                version();
            } else if (line.hasOption("help")) {
                showHelp(options);
            } else {
                version();
                System.out.println();
                showHelp(options);
            }
        } catch (ParseException ex) {
            System.out.println(ex.getMessage());
            showHelp(options);
        } catch (Exception ex) {
            if (line.hasOption("verbose")) {
                ex.printStackTrace(System.out);
            } else {
                System.out.println(ex.getMessage());
            }
        }
    }

    public static void sha256(CommandLine line, Options options) throws ParseException, Exception {
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

    public static void genkey(CommandLine line, Options options) throws ParseException, Exception {
        if (!line.hasOption("output")) {
            throw new Exception("Please specify the path to output the key file using --output or -o");
        }

        String[] genkeyArgs = line.getOptionValues("genkey");
        String outputArg = line.getOptionValue("output");

        if (genkeyArgs.length != 2) {
            throw new ParseException("Wrong arguments for 'genkey', expecting 2 arguments");
        }
        genkeyArgs[0] = genkeyArgs[0].toLowerCase();
        if (!genkeyArgs[0].equals("aes") && !genkeyArgs[0].equals("rsa")) {
            throw new ParseException("Key generation only support AES and RSA.");
        }

        int keySize = 0;
        try {
            keySize = Integer.parseInt(genkeyArgs[1]);
            if (genkeyArgs[0].equals("rsa") && keySize < 512) {
                throw new Exception("Key length should at least 512 bits for RSA.");
            }
            if (keySize % 8 != 0) {
                throw new ParseException("Key length should be a multiple of 8.");
            }
        } catch (NumberFormatException ex) {
            throw new ParseException("Key length should be a valid integer, your input: " + genkeyArgs[1]);
        }

        System.out.println("Method: " + genkeyArgs[0]);
        System.out.println("Key size: " + keySize);
        System.out.println("Output path: " + outputArg);
        System.out.println();

        if (genkeyArgs[0].equals("aes")) {
            KeyGenerator.generateAES(keySize, new File(outputArg));
        } else {
            KeyGenerator.generateRSA(keySize, new File(outputArg));
        }

        System.out.println("Key generated and saved to " + outputArg);
    }

    public static void renew(CommandLine line, Options options) throws ParseException, Exception {
        String renewArg = line.getOptionValue("renew");

        System.out.println("Key file: " + renewArg);
        System.out.println();

        KeyGenerator.renewAESIV(new File(renewArg));

        System.out.println("AES IV renewal succeed.");
    }

    public static void diff(CommandLine line, Options options) throws ParseException, Exception {
        if (!line.hasOption("output")) {
            throw new Exception("Please specify the path to output the diff file using --output or -o");
        }

        String[] diffArgs = line.getOptionValues("diff");
        String outputArg = line.getOptionValue("output");

        if (diffArgs.length != 2) {
            throw new ParseException("Wrong arguments for 'diff', expecting 2 arguments");
        }

        System.out.println("Old file: " + diffArgs[0]);
        System.out.println("New file: " + diffArgs[1]);
        System.out.println("Diff file: " + outputArg);
        System.out.println();

        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(new File(outputArg));
            DiffWriter diffOut = new GDiffWriter(fout);
            Delta delta = new Delta();
            delta.compute(new File(diffArgs[0]), new File(diffArgs[1]), diffOut);
        } finally {
            Util.closeQuietly(fout);
        }

        System.out.println("Diff file generated.");
    }

    public static void diffpatch(CommandLine line, Options options) throws ParseException, Exception {
        if (!line.hasOption("output")) {
            throw new Exception("Please specify the path to output the patched file using --output or -o");
        }

        String[] diffpatchArgs = line.getOptionValues("diffpatch");
        String outputArg = line.getOptionValue("output");

        if (diffpatchArgs.length != 2) {
            throw new ParseException("Wrong arguments for 'diffpatch', expecting 2 arguments");
        }

        System.out.println("File to apply patch to: " + diffpatchArgs[0]);
        System.out.println("Patch file: " + diffpatchArgs[1]);
        System.out.println("Output file: " + outputArg);
        System.out.println();

        GDiffPatcher diffPatcher = new GDiffPatcher();
        diffPatcher.patch(new File(diffpatchArgs[0]), new File(diffpatchArgs[1]), new File(outputArg));

        System.out.println("Patching completed.");
    }

    public static void compress(CommandLine line, Options options) throws ParseException, Exception {
        // file patch
        if (!line.hasOption("output")) {
            throw new Exception("Please specify the path to output the compressed file using --output or -o");
        }

        String compressArg = line.getOptionValue("compress");
        String outputArg = line.getOptionValue("output");

        System.out.println("File to compress: " + compressArg);
        System.out.println("Output file: " + outputArg);
        System.out.println();

        FileInputStream fin = null;
        FileOutputStream fout = null;
        try {
            File inFile = new File(compressArg);
            long inFileLength = inFile.length();

            fin = new FileInputStream(inFile);
            fout = new FileOutputStream(new File(outputArg));
            XZOutputStream xzOut = new XZOutputStream(fout, new LZMA2Options());

            int byteRead, cumulateByteRead = 0;
            byte[] b = new byte[32768];
            while ((byteRead = fin.read(b)) != -1) {
                xzOut.write(b, 0, byteRead);

                cumulateByteRead += byteRead;
                if (cumulateByteRead >= inFileLength) {
                    break;
                }
            }

            if (cumulateByteRead != inFileLength) {
                throw new Exception("Error occurred when reading the input file.");
            }

            xzOut.finish();
        } finally {
            Util.closeQuietly(fin);
            Util.closeQuietly(fout);
        }

        System.out.println("Compression completed.");
    }

    public static void decompress(CommandLine line, Options options) throws ParseException, Exception {
        // file patch
        if (!line.hasOption("output")) {
            throw new Exception("Please specify the path to output the decompressed file using --output or -o");
        }

        String decompressArg = line.getOptionValue("decompress");
        String outputArg = line.getOptionValue("output");

        System.out.println("File to decompress: " + decompressArg);
        System.out.println("Output file: " + outputArg);
        System.out.println();

        FileInputStream fin = null;
        FileOutputStream fout = null;
        try {
            File inFile = new File(decompressArg);
            long inFileLength = inFile.length();

            fin = new FileInputStream(inFile);
            XZInputStream xzIn = new XZInputStream(fin);
            fout = new FileOutputStream(new File(outputArg));

            int byteRead, cumulateByteRead = 0;
            byte[] b = new byte[32768];
            while ((byteRead = xzIn.read(b)) != -1) {
                fout.write(b, 0, byteRead);

                cumulateByteRead += byteRead;
                if (cumulateByteRead >= inFileLength) {
                    break;
                }
            }

            if (cumulateByteRead != inFileLength) {
                throw new Exception("Error occurred when reading the input file.");
            }
        } finally {
            Util.closeQuietly(fin);
            Util.closeQuietly(fout);
        }

        System.out.println("Decompression completed.");
    }

    public static void doPatch(CommandLine line, Options options) throws ParseException, Exception {
        String[] doArgs = line.getOptionValues("do");

        if (doArgs.length != 2) {
            throw new ParseException("Wrong arguments for 'do', expecting 2 arguments");
        }

        System.out.println("Target folder: " + doArgs[0]);
        System.out.println("Patch file: " + doArgs[1]);
        System.out.println();

        File patchFile = new File(doArgs[1]);

        File tempDir = new File("tmp/" + System.currentTimeMillis());
        tempDir.mkdirs();

        AESKey aesKey = null;
        File decryptedPatchFile = null;
        if (line.hasOption("key")) {
            aesKey = AESKey.read(Util.readFile(new File(line.getOptionValue("key"))));
            if (aesKey.getKey().length != 32) {
                throw new Exception("Currently only support 256 bits AES key.");
            }

            decryptedPatchFile = new File(tempDir.getAbsolutePath() + File.separator + patchFile.getName() + ".decrypted");
            decryptedPatchFile.delete();
            decryptedPatchFile.deleteOnExit();

            patchFile = decryptedPatchFile;
        }

        Patcher patcher = new Patcher(new File(tempDir.getAbsolutePath() + "/action.log"));
        patcher.doPatch(new PatcherListener() {

            @Override
            public void patchProgress(int percentage, String message) {
                System.out.println(percentage + "%, " + message);
            }

            @Override
            public void patchEnableCancel(boolean enable) {
            }
        }, patchFile, 0, aesKey, new File(doArgs[0]), tempDir, new HashMap<String, String>());

        System.out.println("Patch completed.");

        // preserve the log
//        Util.truncateFolder(tempDir);
//        tempDir.delete();

        System.out.println();
        System.out.println("Patch applied successfully.");
    }

    public static void full(CommandLine line, Options options) throws ParseException, Exception {
        if (!line.hasOption("output")) {
            throw new Exception("Please specify the path to output the patch using --output");
        }
        if (!line.hasOption("from") && !line.hasOption("from-sub")) {
            throw new Exception("Please specify the version number of the old version --from or --from-sub");
        }
        if (!line.hasOption("to")) {
            throw new Exception("Please specify the version number of the new version using --to");
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

        File tempDir = new File("tmp/" + System.currentTimeMillis());
        tempDir.mkdirs();

        AESKey aesKey = null;
        if (line.hasOption("key")) {
            aesKey = AESKey.read(Util.readFile(new File(line.getOptionValue("key"))));
            if (aesKey.getKey().length != 32) {
                throw new Exception("Currently only support 256 bits AES key.");
            }
        }
        File patchFile = new File(outputArg);
        File encryptedPatchFile = new File(tempDir.getAbsolutePath() + File.separator + patchFile.getName() + ".encrypted");
        encryptedPatchFile.delete();
        encryptedPatchFile.deleteOnExit();

        PatchCreator.createFullPatch(new File(fullArg), new File(outputArg), -1, fromArg, fromSubsequentArg, toArg, aesKey, encryptedPatchFile);

        Util.truncateFolder(tempDir);
        tempDir.delete();

        System.out.println("Patch created.");
    }

    public static void patch(CommandLine line, Options options) throws ParseException, Exception {
        if (!line.hasOption("output")) {
            throw new Exception("Please specify the path to output the patch using --output");
        }
        if (!line.hasOption("from")) {
            throw new Exception("Please specify the version number of the old version using --from");
        }
        if (!line.hasOption("to")) {
            throw new Exception("Please specify the version number of the new version using --to");
        }

        String[] patchArgs = line.getOptionValues("patch");
        String outputArg = line.getOptionValue("output");
        String fromArg = line.getOptionValue("from");
        String toArg = line.getOptionValue("to");

        if (patchArgs.length != 2) {
            throw new ParseException("Wrong arguments for 'patch', expecting 2 arguments");
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

        File tempDir = new File("tmp/" + System.currentTimeMillis());
        tempDir.mkdirs();

        AESKey aesKey = null;
        if (line.hasOption("key")) {
            aesKey = AESKey.read(Util.readFile(new File(line.getOptionValue("key"))));
            if (aesKey.getKey().length != 32) {
                throw new Exception("Currently only support 256 bits AES key.");
            }
        }
        File patchFile = new File(outputArg);
        File encryptedPatchFile = new File(tempDir.getAbsolutePath() + File.separator + patchFile.getName() + ".encrypted");
        encryptedPatchFile.delete();
        encryptedPatchFile.deleteOnExit();

        PatchCreator.createPatch(new File(patchArgs[0]), new File(patchArgs[1]), tempDir, patchFile, -1, fromArg, toArg, aesKey, encryptedPatchFile);

        Util.truncateFolder(tempDir);
        tempDir.delete();

        System.out.println("Patch created.");
    }

    public static void extract(CommandLine line, Options options) throws ParseException, Exception {
        // file folder
        String[] extractArgs = line.getOptionValues("extract");

        if (extractArgs.length != 2) {
            throw new ParseException("Wrong arguments for 'extract', expecting 2 arguments");
        }

        System.out.println("Patch path: " + extractArgs[0]);
        System.out.println("Path to save the extracted files: " + extractArgs[1]);
        System.out.println();

        File tempDir = new File("tmp/" + System.currentTimeMillis());
        tempDir.mkdirs();

        AESKey aesKey = null;
        if (line.hasOption("key")) {
            aesKey = AESKey.read(Util.readFile(new File(line.getOptionValue("key"))));
            if (aesKey.getKey().length != 32) {
                throw new Exception("Currently only support 256 bits AES key.");
            }
        }
        File patchFile = new File(extractArgs[0]);
        File decryptedPatchFile = new File(tempDir.getAbsolutePath() + File.separator + patchFile.getName() + ".decrypted");
        decryptedPatchFile.delete();
        decryptedPatchFile.deleteOnExit();

        PatchExtractor.extract(patchFile, new File(extractArgs[1]), aesKey, decryptedPatchFile);

        Util.truncateFolder(tempDir);
        tempDir.delete();

        System.out.println("Extraction completed.");
    }

    public static void pack(CommandLine line, Options options) throws ParseException, Exception {
        if (!line.hasOption("output")) {
            throw new Exception("Please specify the path to output the patch using --output");
        }

        String packArg = line.getOptionValue("pack");
        String outputArg = line.getOptionValue("output");

        System.out.println("Folder to pack: " + packArg);
        System.out.println("Path to save the packed file: " + outputArg);
        System.out.println();

        AESKey aesKey = null;

        if (line.hasOption("key")) {
            aesKey = AESKey.read(Util.readFile(new File(line.getOptionValue("key"))));
            if (aesKey.getKey().length != 32) {
                throw new Exception("Currently only support 256 bits AES key.");
            }
        }
        File sourceFolder = new File(packArg);
        File encryptedPatchFile = new File("tmp/" + sourceFolder.getName() + ".enrypted");
        encryptedPatchFile.delete();
        encryptedPatchFile.deleteOnExit();

        PatchPacker.pack(sourceFolder, new File(outputArg), aesKey, encryptedPatchFile);

        System.out.println("Packing completed.");
    }

    public static void catalog(CommandLine line, Options options) throws ParseException, Exception {
        if (!line.hasOption("key")) {
            throw new Exception("Please specify the key file to use using --key");
        }
        if (!line.hasOption("output")) {
            throw new Exception("Please specify the path to output the XML file using --output");
        }

        String[] catalogArgs = line.getOptionValues("catalog");
        String keyArg = line.getOptionValue("key");
        String outputArg = line.getOptionValue("output");

        if (catalogArgs.length != 2) {
            throw new ParseException("Wrong arguments for 'catalog', expecting 2 arguments");
        }
        if (!catalogArgs[0].equals("e") && !catalogArgs[0].equals("d")) {
            throw new ParseException("Catalog mode should be either 'e' or 'd' but not " + catalogArgs[0]);
        }

        RSAKey rsaKey = RSAKey.read(Util.readFile(new File(keyArg)));

        System.out.println("Mode: " + (catalogArgs[0].equals("e") ? "encrypt" : "decrypt"));
        System.out.println("Catalog file: " + catalogArgs[1]);
        System.out.println("Key file: " + keyArg);
        System.out.println("Output file: " + outputArg);
        System.out.println();

        File in = new File(catalogArgs[1]);
        File out = new File(outputArg);
        BigInteger mod = new BigInteger(rsaKey.getModulus());

        if (catalogArgs[0].equals("e")) {
            BigInteger privateExp = new BigInteger(rsaKey.getPrivateExponent());

            RSAPrivateKey privateKey = CommonUtil.getPrivateKey(mod, privateExp);

            // compress
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            GZIPOutputStream gout = new GZIPOutputStream(bout);
            gout.write(Util.readFile(in));
            gout.finish();
            byte[] compressedData = bout.toByteArray();

            // encrypt
            int blockSize = mod.bitLength() / 8;
            byte[] encrypted = Util.rsaEncrypt(privateKey, blockSize, blockSize - 11, compressedData);

            // write to file
            Util.writeFile(out, encrypted);
        } else {
            BigInteger publicExp = new BigInteger(rsaKey.getPublicExponent());
            RSAPublicKey publicKey = CommonUtil.getPublicKey(mod, publicExp);

            // decrypt
            int blockSize = mod.bitLength() / 8;
            byte[] decrypted = Util.rsaDecrypt(publicKey, blockSize, Util.readFile(in));

            // decompress
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ByteArrayInputStream bin = new ByteArrayInputStream(decrypted);
            GZIPInputStream gin = new GZIPInputStream(bin);

            int byteRead;
            byte[] b = new byte[1024];
            while ((byteRead = gin.read(b)) != -1) {
                bout.write(b, 0, byteRead);
            }
            byte[] decompressedData = bout.toByteArray();

            // write to file
            Util.writeFile(out, decompressedData);
        }

        System.out.println("Manipulation succeed.");
    }

    public static void validate(CommandLine line, Options options) throws ParseException, Exception {
        String validateArg = line.getOptionValue("validate");
        String outputArg = line.getOptionValue("output");

        System.out.println("Script file: " + validateArg);
        if (outputArg != null) {
            System.out.println("Output file: " + outputArg);
        }
        System.out.println();

        byte[] scriptContent = Util.readFile(new File(validateArg));
        Document doc = XMLUtil.readDocument(scriptContent);
        Element rootElement = doc.getDocumentElement();

        byte[] contentToOutput = null;
        String rootElementTag = rootElement.getTagName();
        if (rootElementTag.equals("patches")) {
            contentToOutput = updater.script.Catalog.read(scriptContent).output();
        } else if (rootElementTag.equals("patch")) {
            contentToOutput = Patch.read(scriptContent).output();
        } else if (rootElementTag.equals("root")) {
            contentToOutput = Client.read(scriptContent).output();
        } else {
            throw new Exception("Failed to recognize the script file.");
        }

        if (outputArg != null) {
            Util.writeFile(new File(outputArg), contentToOutput);
        }

        System.out.println("Validation finished.");
    }

    public static void version() {
        System.out.println("Software Updater - Patch Builder\r\nversion: 0.1.0 preview");
    }

    public static void showHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("builder", options);
    }
}
