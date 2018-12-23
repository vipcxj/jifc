package me.cxj.ifc.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.cxj.ifc.utils.GeomUtils;
import me.cxj.ifc.utils.IOUtils;
import org.bimserver.emf.IdEObject;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.models.geometry.Bounds;
import org.bimserver.models.geometry.Buffer;
import org.bimserver.models.geometry.GeometryData;
import org.bimserver.models.geometry.GeometryFactory;
import org.bimserver.models.geometry.GeometryInfo;
import org.bimserver.models.ifc4.IfcProduct;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by vipcxj on 2018/12/14.
 */
public class GeomModel {

    private List<GeomIndexer> indexers;
    private ByteBuffer buffer;

    public List<GeomIndexer> getIndexers() {
        return indexers;
    }

    public void setIndexers(List<GeomIndexer> indexers) {
        this.indexers = indexers;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public void save(OutputStream os) throws IOException {
        Gson gson = new Gson();
        String jsonPart = gson.toJson(indexers);
        byte[] bJsonPart = jsonPart.getBytes(StandardCharsets.UTF_8);
        int padding = bJsonPart.length % 4;
        if (padding != 0) {
            padding = 4 - padding;
        }
        Header header = new Header();
        header.setIndexersLength(bJsonPart.length + padding);
        header.setBinaryLength(buffer.capacity());
        header.save(os);
        os.write(bJsonPart);
        for (int i = 0; i < padding; ++i) {
            os.write(' ');
        }
        buffer.position(0);
        buffer.limit(buffer.capacity());
        for (int i = 0; i < buffer.capacity(); ++i) {
            os.write(buffer.get());
        }
    }

    public GeomModel load(InputStream is) throws IOException {
        Header header = new Header().load(is);
        byte[] bJsonPart = new byte[header.getIndexersLength()];
        IOUtils.readFully(is, bJsonPart);
        String jsonPart = new String(bJsonPart, StandardCharsets.UTF_8);
        Gson gson = new Gson();
        indexers = gson.fromJson(jsonPart, new TypeToken<List<GeomIndexer>>() {}.getType());
        buffer = ByteBuffer.allocate(header.getBinaryLength());
        for (int i = 0; i < buffer.capacity(); ++i) {
            buffer.put((byte) is.read());
        }
        buffer.rewind();
        return this;
    }

    private static GeomIndexer readyIndexer(String guid, GeometryInfo info, GeomModel geomModel) {
        GeometryData data = info.getData();
        GeomIndexer indexer = new GeomIndexer();
        indexer.guid = guid;
        indexer.nmInd = data.getIndices() != null ? data.getIndices().getData().length / 4 : 0;
        indexer.nmVer = data.getVertices() != null ? data.getVertices().getData().length / 12 : 0;
        indexer.nmNor = data.getNormals() != null ? data.getNormals().getData().length / 12 : 0;
        indexer.nmCol = data.getColorsQuantized() != null ? data.getColorsQuantized().getData().length / 16 : 0;
        geomModel.indexers.add(indexer);
        return indexer;
    }

    private static void fromGeomInfo(GeometryInfo info, GeomIndexer indexer,
                                     ByteBuffer transBuffer, DoubleBuffer doubleTransBuffer,
                                     ByteBuffer indBuffer,
                                     ByteBuffer verBuffer,
                                     ByteBuffer norBuffer,
                                     ByteBuffer colBuffer) {
        if (info.getBounds() != null) {
            indexer.setBounds(new double[] {
                    info.getBounds().getMin().getX(),
                    info.getBounds().getMin().getY(),
                    info.getBounds().getMin().getZ(),
                    info.getBounds().getMax().getX(),
                    info.getBounds().getMax().getY(),
                    info.getBounds().getMax().getZ(),
            });
        }
        if (info.getTransformation() != null) {
            double[] trans = new double[16];
            transBuffer.rewind();
            doubleTransBuffer.rewind();
            transBuffer.put(info.getTransformation());
            doubleTransBuffer.get(trans);
            indexer.setTrans(trans);
        }
        indexer.setArea(info.getArea());
        indexer.setVolume(info.getVolume());
        indexer.setNmTri(info.getPrimitiveCount() != null ? info.getPrimitiveCount() : indexer.nmInd / 3);
        GeometryData data = info.getData();
        indexer.otInd = indBuffer.position();
        if (data.getIndices() != null) {
            indBuffer.put(data.getIndices().getData());
        }
        indexer.otVer = verBuffer.position();
        if (data.getVertices() != null) {
            verBuffer.put(data.getVertices().getData());
        }
        indexer.otNor = norBuffer.position();
        if (data.getNormals() != null) {
            norBuffer.put(data.getNormals().getData());
        }
        indexer.otCol = colBuffer.position();
        if (data.getColorsQuantized() != null) {
            colBuffer.put(data.getColorsQuantized().getData());
        }
    }

    public static GeomModel fromModel(IfcModel model) {
        GeomModel geomModel = new GeomModel();
        geomModel.indexers = new ArrayList<>();
        ByteBuffer transBuffer = ByteBuffer.allocate(16 * 8).order(ByteOrder.LITTLE_ENDIAN);
        DoubleBuffer doubleTransBuffer = transBuffer.asDoubleBuffer();
        if (model.isIfc4()) {
            for (IfcProduct product : model.getAllWithSubTypes(IfcProduct.class)) {
                GeometryInfo info = product.getGeometry();
                if (info != null) {
                    GeomIndexer indexer = readyIndexer(product.getGlobalId(), info, geomModel);
                }
            }
        } else {
            for (org.bimserver.models.ifc2x3tc1.IfcProduct product : model.getAllWithSubTypes(org.bimserver.models.ifc2x3tc1.IfcProduct.class)) {
                GeometryInfo info = product.getGeometry();
                if (info != null) {
                    GeomIndexer indexer = readyIndexer(product.getGlobalId(), info, geomModel);
                }
            }
        }
        int i, nIndAll = 0, nVerAll = 0, nNorAll = 0, nColAll = 0;
        for (i = 0; i < geomModel.indexers.size(); ++i) {
            GeomIndexer indexer = geomModel.indexers.get(i);
            nIndAll += indexer.nmInd;
            nVerAll += indexer.nmVer;
            nNorAll += indexer.nmNor;
            nColAll += indexer.nmCol;
        }
        int oInd = 0;
        int oVer = nIndAll * 4;
        int oNor = oVer + nVerAll * 4 * 3;
        int oCol = oNor + nNorAll * 4 * 3;
        int size = oCol + nColAll * 4 * 4;
        geomModel.buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer indBuffer = geomModel.buffer.slice();
        indBuffer.position(oInd);
        ByteBuffer verBuffer = geomModel.buffer.slice();
        verBuffer.position(oVer);
        ByteBuffer norBuffer = geomModel.buffer.slice();
        norBuffer.position(oNor);
        ByteBuffer colBuffer = geomModel.buffer.slice();
        colBuffer.position(oCol);
        i = 0;
        if (model.isIfc4()) {
            for (IfcProduct product : model.getAllWithSubTypes(IfcProduct.class)) {
                GeometryInfo info = product.getGeometry();
                if (info != null) {
                    GeomIndexer indexer = geomModel.indexers.get(i);
                    fromGeomInfo(info, indexer, transBuffer, doubleTransBuffer, indBuffer, verBuffer, norBuffer, colBuffer);
                    ++i;
                }
            }
        } else {
            for (org.bimserver.models.ifc2x3tc1.IfcProduct product : model.getAllWithSubTypes(org.bimserver.models.ifc2x3tc1.IfcProduct.class)) {
                GeometryInfo info = product.getGeometry();
                if (info != null) {
                    GeomIndexer indexer = geomModel.indexers.get(i);
                    fromGeomInfo(info, indexer, transBuffer, doubleTransBuffer, indBuffer, verBuffer, norBuffer, colBuffer);
                    ++i;
                }
            }
        }
        return geomModel;
    }

    private static Buffer createBuffer(ByteBuffer byteBuffer, int offset, int num, int size) {
        Buffer buffer = GeometryFactory.eINSTANCE.createBuffer();
        byte[] data = new byte[num * size];
        byteBuffer.mark();
        byteBuffer.position(offset);
        byteBuffer.get(data);
        byteBuffer.reset();
        buffer.setData(data);
        return buffer;
    }

    private static GeometryInfo toGeometryInfo(GeomIndexer indexer, ByteBuffer buffer) {
        GeometryInfo geometryInfo = GeometryFactory.eINSTANCE.createGeometryInfo();
        double[] arBounds = indexer.getBounds();
        if (arBounds != null) {
            Bounds bounds = GeometryFactory.eINSTANCE.createBounds();
            bounds.setMin(GeomUtils.createVector3f(arBounds[0], arBounds[1], arBounds[2]));
            bounds.setMax(GeomUtils.createVector3f(arBounds[3], arBounds[4], arBounds[5]));
            geometryInfo.setBounds(bounds);
        }
        geometryInfo.setArea(indexer.area);
        geometryInfo.setVolume(indexer.volume);
        geometryInfo.setPrimitiveCount(indexer.nmTri);
        if (indexer.trans != null) {
            ByteBuffer transBuffer = ByteBuffer.allocate(8 * 16).order(ByteOrder.LITTLE_ENDIAN);
            transBuffer.asDoubleBuffer().put(indexer.trans);
            geometryInfo.setTransformation(transBuffer.array());
        }
        GeometryData geometryData = GeometryFactory.eINSTANCE.createGeometryData();
        if (indexer.nmInd > 0) {
            geometryData.setIndices(createBuffer(buffer, indexer.otInd, indexer.nmInd, 4));
        }
        if (indexer.nmVer > 0) {
            geometryData.setVertices(createBuffer(buffer, indexer.otVer, indexer.nmVer, 12));
        }
        if (indexer.nmNor > 0) {
            geometryData.setNormals(createBuffer(buffer, indexer.otNor, indexer.nmNor, 12));
        }
        if (indexer.nmCol > 0) {
            geometryData.setColorsQuantized(createBuffer(buffer, indexer.otCol, indexer.nmCol, 16));
        }
        geometryInfo.setData(geometryData);
        return geometryInfo;
    }

    public void fillModel(IfcModelInterface model) {
        for (GeomIndexer indexer : indexers) {
            IdEObject idEObject = model.getByGuid(indexer.guid);
            GeometryInfo info = toGeometryInfo(indexer, buffer);
            if (idEObject instanceof IfcProduct) {
                ((IfcProduct) idEObject).setGeometry(info);
            } else if (idEObject instanceof org.bimserver.models.ifc2x3tc1.IfcProduct) {
                ((org.bimserver.models.ifc2x3tc1.IfcProduct) idEObject).setGeometry(info);
            }
        }
    }

    public static class GeomIndexer {
        private String guid;
        private double[] bounds;
        private double area;
        private double volume;
        private int nmTri;
        private double[] trans;
        private int otInd;
        private int nmInd;
        private int otVer;
        private int nmVer;
        private int otNor;
        private int nmNor;
        private int otCol;
        private int nmCol;

        public String getId() {
            return guid;
        }

        public void setId(String guid) {
            this.guid = guid;
        }

        public double[] getBounds() {
            return bounds;
        }

        public void setBounds(double[] bounds) {
            this.bounds = bounds;
        }

        public double getArea() {
            return area;
        }

        public void setArea(double area) {
            this.area = area;
        }

        public double getVolume() {
            return volume;
        }

        public void setVolume(double volume) {
            this.volume = volume;
        }

        public int getNmTri() {
            return nmTri;
        }

        public void setNmTri(int nmTri) {
            this.nmTri = nmTri;
        }

        public double[] getTrans() {
            return trans;
        }

        public void setTrans(double[] trans) {
            this.trans = trans;
        }

        public int getOtInd() {
            return otInd;
        }

        public void setOtInd(int otInd) {
            this.otInd = otInd;
        }

        public int getNmInd() {
            return nmInd;
        }

        public void setNmInd(int nmInd) {
            this.nmInd = nmInd;
        }

        public int getOtVer() {
            return otVer;
        }

        public void setOtVer(int otVer) {
            this.otVer = otVer;
        }

        public int getNmVer() {
            return nmVer;
        }

        public void setNmVer(int nmVer) {
            this.nmVer = nmVer;
        }

        public int getOtNor() {
            return otNor;
        }

        public void setOtNor(int otNor) {
            this.otNor = otNor;
        }

        public int getNmNor() {
            return nmNor;
        }

        public void setNmNor(int nmNor) {
            this.nmNor = nmNor;
        }

        public int getOtCol() {
            return otCol;
        }

        public void setOtCol(int otCol) {
            this.otCol = otCol;
        }

        public int getNmCol() {
            return nmCol;
        }

        public void setNmCol(int nmCol) {
            this.nmCol = nmCol;
        }
    }

    public static class Header {
        public static final byte[] MAGIC = new byte[] {0x67, 0x65, 0x6f, 0x6d}; // geom
        private int version = 1;
        private int headerLength = 20;
        private int indexersLength;
        private int binaryLength;

        public byte[] getMagic() {
            return MAGIC;
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public int getHeaderLength() {
            return headerLength;
        }

        public void setHeaderLength(int headerLength) {
            this.headerLength = headerLength;
        }

        public int getIndexersLength() {
            return indexersLength;
        }

        public void setIndexersLength(int indexersLength) {
            this.indexersLength = indexersLength;
        }

        public int getBinaryLength() {
            return binaryLength;
        }

        public void setBinaryLength(int binaryLength) {
            this.binaryLength = binaryLength;
        }

        private static void putInt(OutputStream os, int v) throws IOException {
            os.write(0xFF & v);
            os.write(0xFF & (v >> 8));
            os.write(0xFF & (v >> 16));
            os.write(0xFF & (v >> 24));
        }

        public final int getInt(InputStream is) throws IOException {
            int byte1 = is.read();
            int byte2 = is.read();
            int byte3 = is.read();
            int byte4 = is.read();
            if (byte4 == -1) {
                throw new EOFException();
            }
            return (byte4 << 24)
                    + ((byte3 << 24) >>> 8)
                    + ((byte2 << 24) >>> 16)
                    + ((byte1 << 24) >>> 24);
        }

        public static byte[] getMagic(InputStream is) throws IOException {
            int byte1 = is.read();
            int byte2 = is.read();
            int byte3 = is.read();
            int byte4 = is.read();
            if (byte4 == -1) {
                throw new EOFException();
            }
            return new byte[] {(byte) byte1, (byte) byte2, (byte) byte3, (byte) byte4};
        }

        public void save(OutputStream os) throws IOException {
            os.write(MAGIC);
            putInt(os, version);
            putInt(os, headerLength);
            putInt(os, indexersLength);
            putInt(os, binaryLength);
        }

        public Header load(InputStream is) throws IOException {
            byte[] magic = getMagic(is);
            if (!Arrays.equals(magic, MAGIC)) {
                throw new IllegalArgumentException("Invalid magic: " + new String(magic, StandardCharsets.UTF_8) + ", expect for " + new String(MAGIC, StandardCharsets.UTF_8) + ".");
            }
            this.version = getInt(is);
            this.headerLength = getInt(is);
            this.indexersLength = getInt(is);
            this.binaryLength = getInt(is);
            return this;
        }
    }
}
