package updater.downloader.download;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import javax.crypto.Cipher;
import updater.script.Catalog;
import updater.downloader.util.Util;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class RemoteContent {

    protected RemoteContent() {
    }

    public static GetCatalogResult getCatalog(String url, long lastUpdateDate, RSAPublicKey key) {
        GetCatalogResult returnResult = new GetCatalogResult(null, false);

        InputStream in = null;
        HttpURLConnection httpConn = null;
        try {
            URL urlObj = new URL(url);

            // open connection
            URLConnection conn = urlObj.openConnection();
            if (!(conn instanceof HttpURLConnection)) {
                throw new MalformedURLException("It is not a valid http URL: " + urlObj.toString());
            }
            httpConn = (HttpURLConnection) conn;

            // connection setting
            httpConn.setDoInput(true);
            httpConn.setDoOutput(true);

            // set request header
            httpConn.setRequestProperty("Connection", "close");
//            httpConn.setRequestProperty("Accept-Encoding", "gzip");
            httpConn.setRequestProperty("User-Agent", "Software Updater");
            httpConn.setUseCaches(false);
            if (lastUpdateDate != -1) {
                httpConn.setIfModifiedSince(lastUpdateDate);
            }

            // connect
            httpConn.connect();

            // get header
            int httpStatusCode = httpConn.getResponseCode();
            String contentEncoding = httpConn.getHeaderField("Content-Encoding");
            int contentLength = -1;
            //<editor-fold defaultstate="collapsed" desc="content length">
            String contentLengthString = httpConn.getHeaderField("Content-Length");
            if (contentLengthString != null) {
                try {
                    contentLength = Integer.parseInt(contentLengthString.trim());
                } catch (Exception ex) {
                    // ignore
                }
            }
            //</editor-fold>

            // check according to header information
            if (httpStatusCode == 304 && lastUpdateDate != -1) {
                return new GetCatalogResult(null, true);
            } else if (httpStatusCode != 200) {
                returnResult = new GetCatalogResult(null, false);
                throw new Exception("HTTP status not 200, status: " + httpStatusCode);
            }


            // download
            in = httpConn.getInputStream();
            in = (contentEncoding != null && contentEncoding.equals("gzip")) ? new GZIPInputStream(in, 8192) : new BufferedInputStream(in);
            ByteArrayOutputStream buffer = contentLength == -1 ? new ByteArrayOutputStream() : new ByteArrayOutputStream(contentLength);
            int byteRead;
            byte[] b = new byte[32];
            while ((byteRead = in.read(b)) != -1) {
                if (Thread.interrupted()) {
                    throw new InterruptedException("Download cancelled by user.");
                }
                buffer.write(b, 0, byteRead);
            }

            // downloaded
            byte[] content = buffer.toByteArray();

            // decrypt
            if (key != null) {
                ByteArrayOutputStream rsaBuffer = new ByteArrayOutputStream(contentLength);

                Cipher decryptCipher = Cipher.getInstance("RSA");
                decryptCipher.init(Cipher.DECRYPT_MODE, key.getKey());

                int maxContentLength = key.getBlockSize();
                if (content.length % maxContentLength != 0) {
                    throw new Exception("RSA block size not match, content length: " + content.length + ", RSA block size: " + maxContentLength);
                }

                for (int i = 0, iEnd = content.length; i < iEnd; i += maxContentLength) {
                    rsaBuffer.write(decryptCipher.doFinal(content, i, maxContentLength));
                }

                content = rsaBuffer.toByteArray();
            }

            // decompress
            ByteArrayOutputStream decompressedOut = new ByteArrayOutputStream();
            ByteArrayInputStream compressedIn = new ByteArrayInputStream(content);
            GZIPInputStream decompressedGIn = new GZIPInputStream(compressedIn);
            while ((byteRead = decompressedGIn.read(b)) != -1) {
                decompressedOut.write(b, 0, byteRead);
            }
            content = decompressedOut.toByteArray();

            returnResult = new GetCatalogResult(Catalog.read(content), false);
        } catch (Exception ex) {
            Logger.getLogger(RemoteContent.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (httpConn != null) {
                    httpConn.disconnect();
                }
            } catch (IOException ex) {
                Logger.getLogger(RemoteContent.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return returnResult;
    }

    protected static void digest(MessageDigest digest, File file) throws Exception {
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);
            long fileLength = file.length();

            int byteRead, cumulateByteRead = 0;
            byte[] b = new byte[1024];
            while ((byteRead = fin.read(b)) != -1) {
                digest.update(b, 0, byteRead);
                cumulateByteRead += byteRead;

                if (cumulateByteRead >= fileLength) {
                    break;
                }
            }

            if (cumulateByteRead != fileLength) {
                throw new Exception("The total number of bytes read does not match the file size. Actual file size: " + fileLength + ", bytes read: " + cumulateByteRead + ", path: " + file.getAbsolutePath());
            }
        } finally {
            if (fin != null) {
                fin.close();
            }
        }
    }

    public static GetPatchResult getPatch(GetPatchListener listener, String url, File saveToFile, String fileSHA256, int expectedLength) {
        GetPatchResult returnResult = new GetPatchResult(false, false);

        InputStream in = null;
        HttpURLConnection httpConn = null;
        OutputStream fout = null;
        try {
            if (!fileSHA256.matches("^[0-9a-f]{64}$")) {
                throw new Exception("SHA format invalid, expected: ^[0-9a-f]{64}$, checksum: " + fileSHA256);
            }

            URL urlObj = new URL(url);
            long fileLength = saveToFile.length();

            // check saveToFile with fileLength and expectedLength
            if (fileLength != 0) {
                if ((fileLength == expectedLength && !Util.getSHA256String(saveToFile).equals(fileSHA256))
                        || fileLength > expectedLength) {
                    // truncate/delete the file
                    try {
                        new FileOutputStream(saveToFile).close();
                    } catch (Exception ex2) {
                        saveToFile.delete();
                    }
                    fileLength = 0;
                } else if (fileLength == expectedLength) {
                    return new GetPatchResult(true, false);
                }
            }


            // open connection
            URLConnection conn = urlObj.openConnection();
            if (!(conn instanceof HttpURLConnection)) {
                throw new MalformedURLException("It is not a valid http URL: " + conn.toString());
            }
            httpConn = (HttpURLConnection) conn;

            // connection setting
            httpConn.setDoInput(true);
            httpConn.setDoOutput(true);

            // set request header
            if (fileLength != 0) {
                httpConn.setRequestProperty("Range", "bytes=" + fileLength + "-");
            }
            httpConn.setRequestProperty("Connection", "close");
            httpConn.setRequestProperty("User-Agent", "Software Updater");
            httpConn.setUseCaches(false);

            // connect
            httpConn.connect();

            // get header
            int httpStatusCode = httpConn.getResponseCode();
            String contentEncoding = httpConn.getHeaderField("Content-Encoding");
            int contentLength = -1;
            //<editor-fold defaultstate="collapsed" desc="content length">
            String contentLengthString = httpConn.getHeaderField("Content-Length");
            if (contentLengthString != null) {
                if (fileLength != 0) {
                    Pattern pattern = Pattern.compile("^([0-9]+)-([0-9]+)/([0-9]+)$");
                    String contentRangeString = httpConn.getHeaderField("Content-Range");
                    if (contentRangeString != null) {
                        Matcher matcher = pattern.matcher(contentRangeString.trim());
                        if (matcher.matches()) {
                            int rangeStart = Integer.parseInt(matcher.group(1));
                            int rangeEnd = Integer.parseInt(matcher.group(2));
                            contentLength = Integer.parseInt(matcher.group(3));
                            if (rangeStart != fileLength) {
                                throw new Exception("Request byte range from " + rangeStart + " but respond byte range: " + contentRangeString);
                            }
                            if (contentLength - 1 != rangeEnd) {
                                throw new Exception("Respond byte range end do not match content length, http respond range end: " + rangeEnd + ", content length: " + contentLength);
                            }
                        }
                    }
                } else {
                    try {
                        contentLength = Integer.parseInt(contentLengthString.trim());
                    } catch (Exception ex) {
                    }
                }
            }
            //</editor-fold>

            // check according to header information
            if (httpStatusCode != 200 && httpStatusCode != 206) {
                throw new Exception("HTTP status is not 200 or 206, status: " + httpStatusCode);
            }
            if (contentLength != - 1 && contentLength != expectedLength) {
                throw new Exception("Expected length and respond content length not match, expected: " + expectedLength + ", actual: " + contentLength);
            }

            // notify listener the starting byte position
            if (httpStatusCode == 200) {
                listener.byteStart(0);
            } else if (httpStatusCode == 206) {
                listener.byteStart(fileLength);
            }

            // download
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            if (fileLength != 0) {
                digest(digest, saveToFile);
            }
            in = httpConn.getInputStream();
            in = (contentEncoding != null && contentEncoding.equals("gzip")) ? new GZIPInputStream(in, 32768) : new BufferedInputStream(in, 32768);
            fout = new BufferedOutputStream(new FileOutputStream(saveToFile, httpStatusCode == 206), 32768);

            int byteRead, cumulateByteRead = 0;
            byte[] b = new byte[32];
            while ((byteRead = in.read(b)) != -1) {
                if (Thread.interrupted()) {
                    boolean result = listener.downloadInterrupted();
                    if (result) {
                        throw new InterruptedException();
                    }
                }

                digest.update(b, 0, byteRead);
                fout.write(b, 0, byteRead);
                cumulateByteRead += byteRead;

                if (listener != null) {
                    listener.byteDownloaded(byteRead);
                }
            }

            // check the downloaded file
            if (cumulateByteRead + fileLength != expectedLength) {
                throw new Exception("Error occurred when reading (cumulated bytes read != expected length), cumulated bytes read: " + cumulateByteRead + " + " + fileLength + ", expected: " + expectedLength);
            }
            if (!Util.byteArrayToHexString(digest.digest()).equals(fileSHA256)) {
                throw new Exception("Checksum not matched. got: " + Util.byteArrayToHexString(digest.digest()) + ", expected: " + fileSHA256);
            }

            returnResult = new GetPatchResult(true, false);
        } catch (InterruptedException ex) {
            return new GetPatchResult(false, true);
        } catch (Exception ex) {
            returnResult = new GetPatchResult(false, false);
            Logger.getLogger(RemoteContent.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (httpConn != null) {
                    httpConn.disconnect();
                }
                if (fout != null) {
                    fout.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(RemoteContent.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return returnResult;
    }

    public static interface GetPatchListener {

        boolean downloadInterrupted();

        void byteStart(long pos);

        void byteDownloaded(int numberOfBytes);
    }

    public static class GetPatchResult {

        protected boolean result;
        protected boolean interrupted;

        public GetPatchResult(boolean result, boolean interrupted) {
            this.result = result;
            this.interrupted = interrupted;
        }

        public boolean getResult() {
            return result;
        }

        public boolean isInterrupted() {
            return interrupted;
        }
    }

    public static class GetCatalogResult {

        protected Catalog catalog;
        protected boolean notModified;

        public GetCatalogResult(Catalog catalog, boolean notModified) {
            this.catalog = catalog;
            this.notModified = notModified;
        }

        public Catalog getCatalog() {
            return catalog;
        }

        public boolean isNotModified() {
            return notModified;
        }
    }

    public static class RSAPublicKey {

        protected BigInteger modulus;
        protected BigInteger exponent;

        public RSAPublicKey(BigInteger mod, BigInteger exp) {
            this.modulus = mod;
            this.exponent = exp;
        }

        public PublicKey getKey() {
            try {
                RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, exponent);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                return keyFactory.generatePublic(keySpec);
            } catch (Exception ex) {
                Logger.getLogger(RemoteContent.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        }

        public int getBlockSize() {
            return (modulus.bitLength() / 8);
        }
    }
}
