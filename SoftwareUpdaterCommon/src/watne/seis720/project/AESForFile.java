/**
 * The Software and related technology may not be downloaded or otherwise 
 * exported or reexported (i) into (or to a national or resident of) Cuba, Iraq, Libya, 
 * North Korea, Iran, or any other country to which the U.S. has embargoed goods; 
 * or (ii) to anyone on the U.S. Treasury Department's list of Specially Designated 
 * Nationals or the U.S. Commerce Department's Table of Denial Orders. By installing 
 * or using the Software, Licensee is agreeing to the foregoing and representing 
 * and warranting that they are not located in, under the control of, or a national or 
 * resident of any such country or on any such list. In addition, The Licensee agrees 
 * to comply with any other applicable U.S. export control laws and any local laws
 * in their jurisdiction that may impact the right to import, export, or use the Software. 
 * By installing or using the Software, Licensee is also representing and warranting
 * that they will not use, or permit or authorize others to use, the Software in connection 
 * with the design, development, production, stockpiling or use of any chemical or 
 * biological weapons.
 */
package watne.seis720.project;

import java.awt.HeadlessException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author John Watne
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class AESForFile extends WatneAES_Implementer {

    protected AESForFileListener listener;
    protected final List<Runnable> interruptedTasks;
    protected boolean pause;

    public AESForFile() {
        super();
        interruptedTasks = Collections.synchronizedList(new ArrayList<Runnable>());
        pause = false;
    }

    public void setListener(AESForFileListener listener) {
        this.listener = listener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void encryptFile(File inputFile, File outputFile) throws Exception {
        byte[] originalIV = null; // Used for CBC mode.

        if (this.getMode() == Mode.CBC) {
            if (this.getInitializationVector() == null) {
                throw new InvalidAlgorithmParameterException("Uninitialized initialization vector.");
            }

            originalIV = new byte[AES_Common.BLOCKSIZE];
            System.arraycopy(this.getInitializationVector(), 0, originalIV, 0, AES_Common.BLOCKSIZE);
        }

        BufferedInputStream reader = null;
        BufferedOutputStream writer = null;

        try {
            reader = new BufferedInputStream(new FileInputStream(inputFile), 32768);
            writer = new BufferedOutputStream(new FileOutputStream(outputFile), 32768);

            // Create array for storing current block read from input.
            byte[] currBlock = new byte[BLOCKSIZE];
            // operation.
            Mode mode = this.getMode(); // Save the requested mode.
            // Temporarily set mode to ECB, and handle any additional XOR logic for other modes in this method.
            this.setMode(Mode.ECB);
            Padding padding = this.getPadding(); // Save requested padding.
            // Temporarily set padding to no padding, for all but the last block.
            this.setPadding(Padding.NOPADDING);
            int bytesRead = reader.read(currBlock, 0, BLOCKSIZE);
            // Bytes read from file.

            int delayRound = 32768 / BLOCKSIZE;
            long inputFileLength = inputFile.length(), cumulateRead = 0, roundCount = 0;
            while (bytesRead >= 0) {
                cumulateRead += bytesRead;

                if (roundCount >= delayRound) {
                    check();
                    if (listener != null) {
                        listener.cryptProgress((int) ((double) (cumulateRead * 100) / (double) inputFileLength));
                    }
                    roundCount = 0;
                }
                roundCount++;

                // Save reference to current currBlock array.
                byte[] save = currBlock;

                // Keep reading the file until get to the end.
                if (reader.available() <= 0) {
                    // If there are no bytes left to read from the input file, then this is the last block, so pad it.
                    this.setPadding(padding); // Restore original padding.

                    // Only read bytesRead bytes from file -- resize currBlock accordingly.
                    byte[] tmp = new byte[bytesRead];
                    // Copy the bytes read to the temporary array.
                    System.arraycopy(currBlock, 0, tmp, 0, bytesRead);
                    currBlock = tmp; // Point currBlock to the resized array.

                    // currBlock = AES_Utilities.getPadded(currBlock, this.getPadding(), this.getKeySize());
                }

                // Encrypt the block.
                this.setPlainText(currBlock);
                this.encrypt();

                // Update encrypted[], based on Mode.
                switch (mode) {
                    case ECB:
                        // Just write the most recently encrypted block to output.
                        writer.write(this.getCipherBytes(), 0, this.getCipherBytes().length);
                        break;
                    case CBC:
                        // Write the block.
                        writer.write(this.getCipherBytes(), 0, this.getCipherBytes().length);

                        // Reset IV to cipher block if more bytes left to read.
                        if (reader.available() > 0) {
                            this.setInitializationVector(this.getCipherBytes());
                        }

                        break;
                    default:
                        throw new InvalidAlgorithmParameterException(mode + " mode not supported at this time.");
                }

                // Restore original currBlock, to ensure array of size BLOCKSIZE bits.
                currBlock = save;
                // Read next block.
                bytesRead = reader.read(currBlock, 0, BLOCKSIZE);
            }
        } finally {
            if (mode == Mode.CBC) {
                // Restore original initialization vector.
                this.setInitializationVector(originalIV);
            }

            this.setMode(mode); // Restore original mode.

            if (writer != null) {
                writer.close(); // Close output stream.
            }
            if (reader != null) {
                reader.close();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void decryptFile(File inputFile, File outputFile) throws HeadlessException, IOException, Exception {
        byte[] originalIV = null; // Used for CBC mode.
        byte[] saveBlock = new byte[AES_Common.BLOCKSIZE]; // Store most
        // recently used encrypted block.

        if (this.getMode() == Mode.CBC) {
            if (this.getInitializationVector() == null) {
                throw new InvalidAlgorithmParameterException("Uninitialized initialization vector.");
            }

            originalIV = new byte[AES_Common.BLOCKSIZE];
            System.arraycopy(this.getInitializationVector(), 0, originalIV, 0, AES_Common.BLOCKSIZE);
        }

        // Create array for storing current block read from input.
        byte[] currBlock = new byte[BLOCKSIZE];
        // operation.
        Mode mode = this.getMode(); // Save the requested mode.
        // Temporarily set mode to ECB, and handle any additional XOR logic for other modes in this method.
        this.setMode(Mode.ECB);
        Padding padding = this.getPadding(); // Save requested padding.
        // Temporarily set padding to no padding, for all but the last block.
        this.setPadding(Padding.NOPADDING);

        BufferedInputStream reader = null;
        BufferedOutputStream writer = null;

        try {
            reader = new BufferedInputStream(new FileInputStream(inputFile), 32768);
            writer = new BufferedOutputStream(new FileOutputStream(outputFile), 32768);

            int delayRound = 32768 / BLOCKSIZE;
            long inputFileLength = inputFile.length(), cumulateRead = 0, roundCount = 0;
            while (reader.read(currBlock, 0, BLOCKSIZE) >= 0) {
                if (roundCount >= delayRound) {
                    check();
                    if (listener != null) {
                        listener.cryptProgress((int) ((double) (cumulateRead * 100) / (double) inputFileLength));
                    }
                    roundCount = 0;
                }
                roundCount++;

                cumulateRead += BLOCKSIZE;

                // Keep reading the file until get to the end.
                if (reader.available() <= 0) {
                    // If there are no bytes left to read from the input file, then this is the last block, so unpad it.
                    this.setPadding(padding); // Restore original padding.
                }

                // Encrypt the block.
                this.setCipherBytes(currBlock);

                if (this.getMode() == Mode.CBC) {
                    // Save a copy of the current encrypted block.
                    System.arraycopy(currBlock, 0, saveBlock, 0, AES_Common.BLOCKSIZE);
                }

                this.decrypt();

                // Update encrypted[], based on Mode.
                switch (mode) {
                    case ECB:
                        // Just write the most recently decrypted block to output.
                        writer.write(this.getPlainTextBytes(), 0, this.getPlainTextBytes().length);
                        break;
                    case CBC:
                        // Write the most recently decrypted block.
                        writer.write(this.getPlainTextBytes(), 0, this.getPlainTextBytes().length);
                        // Reset initialization vector to last encrypted block.
                        this.setInitializationVector(saveBlock);
                        break;
                    default:
                        throw new InvalidAlgorithmParameterException(mode + " mode not supported at this time.");
                }
            }
        } finally {
            this.setMode(mode); // Restore original mode.

            if (mode == Mode.CBC) {
                // Restore original initialization vector.
                this.setInitializationVector(originalIV);
            }

            if (writer != null) {
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
        }
    }

    public void addInterruptedTask(Runnable task) {
        interruptedTasks.add(task);
    }

    public void removeInterruptedTask(Runnable task) {
        interruptedTasks.remove(task);
    }

    public void pause(boolean pause) {
        synchronized (this) {
            this.pause = pause;
            if (!pause) {
                notifyAll();
            }
        }
    }

    protected void check() {
        synchronized (this) {
            if (pause) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                }
            }
        }
        if (Thread.interrupted()) {
            synchronized (interruptedTasks) {
                for (Runnable task : interruptedTasks) {
                    task.run();
                }
            }
            throw new RuntimeException(new InterruptedException());
        }
    }
}
