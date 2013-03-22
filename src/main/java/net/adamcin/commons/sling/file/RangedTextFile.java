package net.adamcin.commons.sling.file;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class RangedTextFile {
    private static final Logger LOGGER = LoggerFactory.getLogger(RangedTextFile.class);

    public static final String PN_DIGEST            = "digest";
    public static final String PN_BINARY            = "binary";
    public static final String PN_INDEX             = "index";
    public static final String PN_ENCODING          = "encoding";
    public static final String PN_RANGE_ID_WIDTH    = "rangeIdWidth";

    private static final int RANGES_MAP_INIT_SIZE = 200000;
    public static final Comparator<String> SORT_CMP = new Comparator<String>() {
        public int compare(String left, String right) {
            return left.compareTo(right);
        }
    };

    private final File tempFile;
    private final HashMap<String, TextRange> serializableRanges;
    private final Map<String, TextRange> ranges;
    private final String encoding;
    private final int rangeIdWidth;
    private final byte[] digest;
    private volatile boolean closed;

    /**
     *
     * @param tempFile
     * @param ranges
     * @param digest
     * @param encoding
     * @param rangeIdWidth
     */
    protected RangedTextFile(final File tempFile,
                             final HashMap<String, TextRange> ranges,
                             final byte[] digest,
                             final String encoding,
                             final int rangeIdWidth) throws IOException {

        this.tempFile = tempFile;
        this.serializableRanges = ranges;
        this.ranges = Collections.synchronizedMap(Collections.unmodifiableMap(ranges));
        this.digest = digest;
        this.encoding = encoding;
        this.rangeIdWidth = rangeIdWidth;
    }

    public boolean usesSameTempFile(RangedTextFile otherTextFile) {
        if (!this.isClosed() && otherTextFile != null && !otherTextFile.isClosed()) {
            return this.tempFile.getAbsolutePath().equals(otherTextFile.tempFile.getAbsolutePath());
        } else {
            return false;
        }
    }

    /**
     * Returns true if the index has the specified range ID key
     * @param rangeId
     * @return
     */
    public boolean hasRange(String rangeId) {
        checkClosed();
        return ranges.containsKey(rangeId);
    }

    /**
     *
     * @return
     */
    public boolean isClosed() {
        return closed || !this.tempFile.exists();
    }

    protected void checkClosed() {
        if (isClosed()) {
            throw new IllegalStateException("RangedTextFile already closed");
        }
    }

    public boolean isDigestEqual(RangedTextFile otherIndex) {
        checkClosed();
        return otherIndex != null && !otherIndex.isClosed()
                && MessageDigest.isEqual(this.digest, otherIndex.digest);
    }

    public int size() {
        checkClosed();
        return ranges.size();
    }

    public synchronized void close() {
        if (!this.closed) {
            this.closed = true;
            this.tempFile.delete();
        }
    }

    public String getRange(String rangeId) {
        checkClosed();

        try {
            byte[] bytes = getRangeBytes(rangeId);
            if (bytes != null) {
                return new String(bytes, this.encoding);
            }
        } catch (IOException e) {
            LOGGER.error("[getRange] Exception", e);
        }

        return null;
    }

    byte[] getRangeBytes(final String rangeId) throws IOException {
        checkClosed();

        InputStream is = null;

        if (hasRange(rangeId)) {
            TextRange textRange = ranges.get(rangeId);

            try {
                is = new FileInputStream(this.tempFile);
                is.skip(textRange.getOffset());

                byte[] buf = new byte[textRange.getLen()];
                is.read(buf, 0, textRange.getLen());
                return buf;
            } catch (IOException e) {
                LOGGER.error("[getLines] Exception", e);
                throw e;
            } finally {
                IOUtils.closeQuietly(is);
            }
        } else {
            return null;
        }
    }

    synchronized void updateRangeDigests(MessageDigest digester) throws IOException {
        checkClosed();
        for (TextRange textRange : ranges.values()) {
            digester.reset();
            byte[] bytes = getRangeBytes(textRange.getRangeId());
            byte[] digest = digester.digest(bytes);
            textRange.setRangeDigest(digest);
        }
    }

    /**
     * Persists the RangedTextFile to the specified JCR node
     * @param node
     * @throws javax.jcr.RepositoryException
     * @throws java.io.IOException
     */
    public synchronized void saveToNode(Node node) throws RepositoryException, IOException {
        checkClosed();
        File indexFile = null;

        ValueFactory vf = node.getSession().getValueFactory();

        long start = System.currentTimeMillis();
        try {

            LOGGER.info("[saveToNode] {}: start", node.getPath());

            indexFile = serializeObjectToTempFile(serializableRanges, this.tempFile.getParentFile());
            node.setProperty(PN_INDEX, vf.createValue(vf.createBinary(new FileInputStream(indexFile))));

            node.setProperty(PN_BINARY, vf.createValue(vf.createBinary(new FileInputStream(this.tempFile))));
            node.setProperty(PN_DIGEST, vf.createValue(vf.createBinary(new ByteArrayInputStream(digest))));

            node.setProperty(PN_ENCODING, vf.createValue(encoding));
            node.setProperty(PN_RANGE_ID_WIDTH, vf.createValue(rangeIdWidth));
            node.getSession().save();

            long end = System.currentTimeMillis();
            LOGGER.info("[saveToNode] {}: Saved {} ranges to node in {} seconds .",
                    new Object[]{ node.getPath(), ranges.size(),
                            String.format("%.2f", (end - start) / 1000.0D)});

        } finally {
            if (indexFile != null) {
                indexFile.delete();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static RangedTextFile loadFromNode(Node node, File tempDir) throws RepositoryException {
        if (node.hasProperty(PN_BINARY) && node.hasProperty(PN_DIGEST)
                && node.hasProperty(PN_INDEX) && node.hasProperty(PN_ENCODING)
                && node.hasProperty(PN_RANGE_ID_WIDTH)) {

            Binary binary = node.getProperty(PN_BINARY).getBinary();
            Binary index = node.getProperty(PN_INDEX).getBinary();
            Binary digest = node.getProperty(PN_DIGEST).getBinary();

            String encoding = node.getProperty(PN_ENCODING).getString();
            int rangeIdWidth = (int) node.getProperty(PN_RANGE_ID_WIDTH).getLong();

            File tempFile = null;

            InputStream binaryStream = null;
            InputStream indexStream = null;
            InputStream digestStream = null;

            long start = System.currentTimeMillis();

            try {
                LOGGER.info("[loadFromNode] {}: start", node.getPath());

                binaryStream = binary.getStream();
                tempFile = copyStreamToTempFile(binaryStream, tempDir);

                indexStream = index.getStream();

                HashMap<String, TextRange> tuples =
                        (HashMap<String, TextRange>) (new ObjectInputStream(indexStream)).readObject();

                digestStream = digest.getStream();
                byte[] _digest = new byte[(int) digest.getSize()];
                digestStream.read(_digest);

                long end = System.currentTimeMillis();

                LOGGER.info("[loadFromNode] {}: Loaded {} ranges from node in {} seconds.",
                        new Object[]{ node.getPath(), tuples.size(),
                                String.format("%.2f", (end - start) / 1000.0D)});

                return new RangedTextFile(tempFile, tuples, _digest, encoding, rangeIdWidth);
            } catch (Exception e) {
                LOGGER.error("[loadFromNode] Exception", e);
                if (tempFile != null && tempFile.exists()) {
                    tempFile.delete();
                }
            } finally {
                IOUtils.closeQuietly(binaryStream);
                IOUtils.closeQuietly(indexStream);
                IOUtils.closeQuietly(digestStream);
            }
        }
        return null;
    }

    /**
     *
     * @param stream
     * @param encoding
     * @param rangeIdWidth
     * @param sort
     * @return
     * @throws java.io.IOException
     */
    public static RangedTextFile createFromStream(final InputStream stream,
                                                     final String encoding,
                                                     final int rangeIdWidth,
                                                     final boolean sort,
                                                     final File tempDir) throws IOException {
        File temp = null;
        try {
            temp = copyStreamToTempFile(stream, tempDir);
            Charset charset = Charset.forName(encoding);

            if (sort) sort(temp, charset, tempDir);

            return createFromSortedFile(temp, charset, rangeIdWidth);
        } catch (IOException e) {
            if (temp != null) {
                temp.delete();
            }
            throw e;
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    public static RangedTextFile createFromSortedFile(final File sortedFile,
                                                         final Charset charset,
                                                         final int rangeIdWidth) throws IOException {

        CountingInputStream countingInputStream = null;
        HashMap<String, TextRange> tuples = new HashMap<String, TextRange>(RANGES_MAP_INIT_SIZE);

        try {
            MessageDigest fullStreamDigester = getDigester();

            countingInputStream = new CountingInputStream(
                    new DigestInputStream(
                            new FileInputStream(sortedFile), fullStreamDigester));

            CharsetDecoder decoder = charset.newDecoder();
            CharsetEncoder encoder = charset.newEncoder();
            int bytesToRead = Math.round(encoder.maxBytesPerChar() * rangeIdWidth);
            long start = System.currentTimeMillis();

            long pos = 0L;
            long rangeStart = pos;
            int nLines = 1;
            String lastRangeId = readWidth(countingInputStream, decoder, bytesToRead, rangeIdWidth);

            while (readUntilNewLine(countingInputStream)) {
                nLines++;
                pos = countingInputStream.getByteCount();

                String rangeId = readWidth(countingInputStream, decoder, bytesToRead, rangeIdWidth);

                if (!rangeId.equals(lastRangeId)) {

                    TextRange textRange = new TextRange(lastRangeId, rangeStart, (int)(pos - rangeStart));
                    tuples.put(textRange.getRangeId(), textRange);

                    LOGGER.debug("[createFromSortedFile] [{}/{}] found range {}", new Object[]{rangeStart, pos, textRange});

                    if (rangeId.indexOf('\n') >= 0) {
                        LOGGER.warn("[createFromSortedFile] Found newline within specified rangeIdWidth on line {}", nLines);
                    }

                    lastRangeId = rangeId;
                    rangeStart = pos;
                }
            }

            byte[] digest = fullStreamDigester.digest();

            RangedTextFile bstf = new RangedTextFile(sortedFile, tuples, digest, charset.name(), rangeIdWidth);
            LOGGER.info("[createFromSortedFile] updating range digests");
            bstf.updateRangeDigests(fullStreamDigester);

            long end = System.currentTimeMillis();
            LOGGER.info("[createFromSortedFile] Indexed {} ranges in {} seconds.", tuples.size(),
                    String.format("%.2f", (end - start) / 1000.0D));

            return bstf;
        } finally {
            IOUtils.closeQuietly(countingInputStream);
        }
    }

    /**
     * Diffs the current file against the base. DiffInfo.TYPE values refer to transitions from the base parameter,
     * as if the base RangedTextFile is the older version, and this is the newer version
     * There are three general cases for the return value when calling this method
     * 1. null indicates that an argument was null or invalid
     * 2. Empty map indicates that the files are identical
     * 3. Populated map contains differences between files keyed on rangeId
     * @param base
     * @return
     */
    public static Map<String, RangeDiffInfoType> diffRanges(final RangedTextFile base, final RangedTextFile current) {
        if (base == null || current == null) {
            return null;
        }

        Map<String, RangeDiffInfoType> diffInfos = new HashMap<String, RangeDiffInfoType>(RANGES_MAP_INIT_SIZE);
        if (!current.isDigestEqual(base)) {
            for (TextRange range : base.ranges.values()) {
                String rangeId = range.getRangeId();
                LOGGER.debug("[diffRanges] base rangeId: {}", rangeId);
                if (!diffInfos.containsKey(rangeId)) {
                    if (current.ranges.containsKey(rangeId)) {
                        if (!MessageDigest.isEqual(current.ranges.get(rangeId).getRangeDigest(), range.getRangeDigest())) {
                            diffInfos.put(rangeId, RangeDiffInfoType.CHANGED);
                        }
                    } else {
                        diffInfos.put(rangeId, RangeDiffInfoType.REMOVED);
                    }
                }
            }

            for (String key : current.ranges.keySet()) {
                if (!base.hasRange(key)) {
                    diffInfos.put(key, RangeDiffInfoType.ADDED);
                }
            }
        }
        return Collections.unmodifiableMap(new TreeMap<String, RangeDiffInfoType>(diffInfos));
    }

    public static enum RangeDiffInfoType {
        ADDED,
        REMOVED,
        CHANGED
    }

    public static class TextRange implements Serializable {

        private static final long serialVersionUID = 1497029928792560129L;

        String rangeId;
        long offset;
        int len;
        byte[] rangeDigest;

        TextRange(String rangeId, long offset, int len) {
            this.rangeId = rangeId;
            this.offset = offset;
            this.len = len;
        }

        public String getRangeId() {
            return rangeId;
        }

        public long getOffset() {
            return offset;
        }

        public int getLen() {
            return len;
        }

        void setRangeDigest(byte[] rangeDigest) {
            this.rangeDigest = rangeDigest;
        }

        public byte[] getRangeDigest() {
            return rangeDigest;
        }

        @Override
        public String toString() {
            return "TextRange{" +
                    "rangeId='" + rangeId + '\'' +
                    ", offset=" + offset +
                    ", len=" + len +
                    '}';
        }
    }

    private static String readWidth(final InputStream stream,
                                    final CharsetDecoder decoder,
                                    final int bytesToRead,
                                    final int width) throws IOException {
        CharBuffer buffer = CharBuffer.allocate(width);
        byte[] bytes = new byte[bytesToRead];
        int read = stream.read(bytes);
        decoder.decode(ByteBuffer.wrap(bytes), buffer, true);
        buffer.rewind();
        return buffer.toString();
    }

    private static boolean readUntilNewLine(InputStream stream) throws IOException {
        boolean foundNewLine = false;

        int read;
        while (!foundNewLine && (read = stream.read()) != -1) {
            if (read == '\n') {
                foundNewLine = true;
            }
        }

        return foundNewLine;
    }

    private static File copyStreamToTempFile(InputStream stream, File tempDir) throws IOException {
        OutputStream out = null;
        File temp = null;
        try {
            temp = File.createTempFile(RangedTextFile.class.getSimpleName(), ".dat", tempDir);
            out = new FileOutputStream(temp);
            IOUtils.copy(stream, out);

            return temp;
        } catch (IOException e) {
            if (temp != null) {
                temp.delete();
            }
            throw e;
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    private static File serializeObjectToTempFile(Object obj, File tempDir) throws IOException {
        File indexFile = null;
        ObjectOutputStream oos = null;
        try {
            indexFile = File.createTempFile(RangedTextFile.class.getSimpleName(), ".dat", tempDir);

            oos = new ObjectOutputStream(new FileOutputStream(indexFile));

            oos.writeObject(obj);

            return indexFile;
        } catch (IOException e) {
            if (indexFile != null) {
                indexFile.delete();
            }
            throw e;
        } finally {
            IOUtils.closeQuietly(oos);
        }
    }

    private static void sort(final File toSort,
                             final Charset charset,
                             final File tempDir) throws IOException {

        long startSort = System.currentTimeMillis();

        List<File> l = ExternalSort.sortInBatch(toSort, SORT_CMP, ExternalSort.DEFAULTMAXTEMPFILES, charset, tempDir);
        ExternalSort.mergeSortedFiles(l, toSort, SORT_CMP, charset);

        long endSort = System.currentTimeMillis();
        LOGGER.info("[sort] Sorted {} in {} seconds.", toSort.getAbsolutePath(),
                String.format("%.2f", (endSort - startSort) / 1000.0D));
    }

    private static MessageDigest getDigester() {
        try {
            return MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("[getDigester] This shouldn't have happened", e);
        }
        return null;
    }
}
