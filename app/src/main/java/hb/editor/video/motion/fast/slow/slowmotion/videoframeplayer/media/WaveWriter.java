package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.media;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

public class WaveWriter {
    private static final int OUTPUT_STREAM_BUFFER = 16384;
    private int mBytesWritten = 0;
    private int mChannels;
    private File mOutFile;
    private BufferedOutputStream mOutStream;
    private int mSampleBits;
    private int mSampleRate;

    public WaveWriter(String path, String name, int sampleRate, int channels, int sampleBits) {
        this.mOutFile = new File(path + File.separator + name);
        this.mSampleRate = sampleRate;
        this.mChannels = channels;
        this.mSampleBits = sampleBits;
    }

    public WaveWriter(File file, int sampleRate, int channels, int sampleBits) {
        this.mOutFile = file;
        this.mSampleRate = sampleRate;
        this.mChannels = channels;
        this.mSampleBits = sampleBits;
    }

    public boolean createWaveFile() throws IOException {
        if (this.mOutFile.exists()) {
            this.mOutFile.delete();
        }
        if (!this.mOutFile.createNewFile()) {
            return false;
        }
        this.mOutStream = new BufferedOutputStream(new FileOutputStream(this.mOutFile), 16384);
        this.mOutStream.write(new byte[44]);
        return true;
    }

    public void write(short[] src, int offset, int length) throws IOException {
        if (this.mChannels == 1) {
            if (offset > length) {
                throw new IndexOutOfBoundsException(String.format("offset %d is greater than length %d", new Object[]{Integer.valueOf(offset), Integer.valueOf(length)}));
            }
            for (int i = offset; i < length; i++) {
                writeUnsignedShortLE(this.mOutStream, src[i]);
                this.mBytesWritten += 2;
            }
        }
    }

    public void write(short[] left, short[] right, int offset, int length) throws IOException {
        if (this.mChannels == 2) {
            if (offset > length) {
                throw new IndexOutOfBoundsException(String.format("offset %d is greater than length %d", new Object[]{Integer.valueOf(offset), Integer.valueOf(length)}));
            }
            for (int i = offset; i < length; i++) {
                writeUnsignedShortLE(this.mOutStream, left[i]);
                writeUnsignedShortLE(this.mOutStream, right[i]);
                this.mBytesWritten += 4;
            }
        }
    }

    public void write(byte[] data, int offset, int length) throws IOException {
        if (this.mChannels == 2) {
            if (offset > length) {
                throw new IndexOutOfBoundsException(String.format("offset %d is greater than length %d", new Object[]{Integer.valueOf(offset), Integer.valueOf(length)}));
            }
            this.mOutStream.write(data, offset, length);
            this.mBytesWritten += data.length;
        }
    }

    public void closeWaveFile() throws IOException {
        if (this.mOutStream != null) {
            this.mOutStream.flush();
            this.mOutStream.close();
        }
        writeWaveHeader();
    }

    private void writeWaveHeader() throws IOException {
        RandomAccessFile file = new RandomAccessFile(this.mOutFile, "rw");
        file.seek(0);
        int bytesPerSec = (this.mSampleBits + 7) / 8;
        file.writeBytes("RIFF");
        file.writeInt(Integer.reverseBytes(this.mBytesWritten + 36));
        file.writeBytes("WAVE");
        file.writeBytes("fmt ");
        file.writeInt(Integer.reverseBytes(16));
        file.writeShort(Short.reverseBytes((short) 1));
        file.writeShort(Short.reverseBytes((short) this.mChannels));
        file.writeInt(Integer.reverseBytes(this.mSampleRate));
        file.writeInt(Integer.reverseBytes((this.mSampleRate * this.mChannels) * bytesPerSec));
        file.writeShort(Short.reverseBytes((short) (this.mChannels * bytesPerSec)));
        file.writeShort(Short.reverseBytes((short) this.mSampleBits));
        file.writeBytes("data");
        file.writeInt(Integer.reverseBytes(this.mBytesWritten));
        file.close();
    }

    private static void writeUnsignedShortLE(BufferedOutputStream stream, short sample) throws IOException {
        stream.write(sample);
        stream.write(sample >> 8);
    }
}
