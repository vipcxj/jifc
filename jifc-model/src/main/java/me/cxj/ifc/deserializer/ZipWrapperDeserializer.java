package me.cxj.ifc.deserializer;

import me.cxj.ifc.utils.IOUtils;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.plugins.deserializers.ByteProgressReporter;
import org.bimserver.plugins.deserializers.DeserializeException;
import org.bimserver.utils.FakeClosingInputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by vipcxj on 2018/11/19.
 */
public class ZipWrapperDeserializer<D extends IfcDeserializer> implements IfcDeserializer {

    private static final byte[] ZIP_MAGIC = new byte[] {0x50, 0x4b, 0x03, 0x04};
    private final D deserializer;

    public ZipWrapperDeserializer(D deserializer) {
        this.deserializer = deserializer;
    }

    @Override
    public IfcModelInterface read(InputStream inputStream, ByteProgressReporter reporter) throws DeserializeException {
        BufferedInputStream bis = new BufferedInputStream(inputStream);
        bis.mark(4);
        byte[] magic = new byte[4];
        try {
            IOUtils.readFully(bis, magic);
            bis.reset();
            if (Arrays.equals(ZIP_MAGIC, magic)) {
                ZipInputStream zipInputStream = new ZipInputStream(bis);
                ZipEntry nextEntry = zipInputStream.getNextEntry();
                if (nextEntry == null) {
                    throw new DeserializeException("Zip files must contain exactly one IFC-file, this zip-file looks empty");
                }
                IfcModelInterface model;
                FakeClosingInputStream fakeClosingInputStream = new FakeClosingInputStream(zipInputStream);
                model = deserializer.read(fakeClosingInputStream, reporter);
                if (model.size() == 0) {
                    throw new DeserializeException("Input file does not seem to be a correct IFC file");
                }
                if (zipInputStream.getNextEntry() != null) {
                    zipInputStream.close();
                    throw new DeserializeException("Zip files may only contain one IFC-file, this zip-file contains more files");
                } else {
                    zipInputStream.close();
                    return model;
                }
            } else {
                return deserializer.read(inputStream, reporter);
            }
        } catch (IOException e) {
            throw new DeserializeException(e);
        }
    }
}
