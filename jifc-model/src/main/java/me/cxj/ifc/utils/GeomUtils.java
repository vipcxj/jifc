package me.cxj.ifc.utils;

import me.cxj.ifc.ifc4.IfcGeomIterator;
import me.cxj.ifc.model.PackageMetaDataSet;
import org.bimserver.emf.IdEObject;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.emf.PackageMetaData;
import org.bimserver.geometry.Matrix;
import org.bimserver.models.geometry.*;
import org.bimserver.models.ifc4.IfcProduct;
import org.bytedeco.javacpp.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Created by vipcxj on 2018/12/10.
 */
public class GeomUtils {

    public static void generateGeomData(IfcModelInterface model, byte[] data) {
        GeometryFactory factory = GeometryFactory.eINSTANCE;
        try (PointerScope scope = new PointerScope()){
            int setting = IfcGeomIterator.DataIterator.EXCLUDE_SOLIDS_AND_SURFACES | IfcGeomIterator.DataIterator.WELD_VERTICES;
            double dt = 1e-4;
            boolean ifc4 = model.getPackageMetaData() == PackageMetaDataSet.IFC4.getMetaData();
            if (ifc4) {
                BytePointer pointer = IfcGeomIterator.allocateByteArray(data.length);
                pointer.put(data);
                IfcGeomIterator.DataIterator dataIterator = new IfcGeomIterator.DataIterator(setting, dt, pointer, data.length);
                while (dataIterator.hasNext()) {
                    IfcGeomIterator.GeomData geomData = dataIterator.next();
                    IdEObject idEObject = model.getByGuid(geomData.guid().getString("UTF-8"));
                    assert idEObject != null;
                    if (idEObject instanceof IfcProduct) {
                        IfcProduct product = (IfcProduct) idEObject;
                        GeometryInfo geometryInfo = createGeometryInfo(model, geomData, factory);
                        product.setGeometry(geometryInfo);
                    }
                }
            } else {
                BytePointer pointer = me.cxj.ifc.ifc2x3.IfcGeomIterator.allocateByteArray(data.length);
                pointer.put(data);
                me.cxj.ifc.ifc2x3.IfcGeomIterator.DataIterator dataIterator = new me.cxj.ifc.ifc2x3.IfcGeomIterator.DataIterator(setting, dt, pointer, data.length);
                while (dataIterator.hasNext()) {
                    me.cxj.ifc.ifc2x3.IfcGeomIterator.GeomData geomData = dataIterator.next();
                    IdEObject idEObject = model.getByGuid(geomData.guid().getString("UTF-8"));
                    assert idEObject != null;
                    if (idEObject instanceof org.bimserver.models.ifc2x3tc1.IfcProduct) {
                        org.bimserver.models.ifc2x3tc1.IfcProduct product = (org.bimserver.models.ifc2x3tc1.IfcProduct) idEObject;
                        GeometryInfo geometryInfo = createGeometryInfo(model, geomData, factory);
                        product.setGeometry(geometryInfo);
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("This is impossible!", e);
        }
    }

    public static Vector3f createVector3f(double x, double y, double z) {
        Vector3f vector3f = GeometryFactory.eINSTANCE.createVector3f();
        vector3f.setX(x);
        vector3f.setY(y);
        vector3f.setZ(z);
        return vector3f;
    }

    public static Vector3f createVector3f(double defaultValue) {
        return createVector3f(defaultValue, defaultValue, defaultValue);
    }

    private static Buffer createBuffer(IntPointer pointer, long size) {
        if (pointer == null || pointer.isNull()) {
            return null;
        }
        Buffer buffer = GeometryFactory.eINSTANCE.createBuffer();
        int[] backArray = new int[(int) size];
        pointer.get(backArray);
        ByteBuffer bb = ByteBuffer.allocate((int) (size * 4));
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.asIntBuffer().put(backArray);
        buffer.setData(bb.array());
        return buffer;
    }

    private static Buffer createBuffer(FloatPointer pointer, long size) {
        if (pointer == null || pointer.isNull()) {
            return null;
        }
        Buffer buffer = GeometryFactory.eINSTANCE.createBuffer();
        float[] backArray = new float[(int) size];
        pointer.get(backArray);
        ByteBuffer bb = ByteBuffer.allocate((int) (size * 4));
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.asFloatBuffer().put(backArray);
        buffer.setData(bb.array());
        return buffer;
    }

    private static Vector3f[] initExtendsBound() {
        Vector3f[] bounds = new Vector3f[2];
        bounds[0] = GeometryFactory.eINSTANCE.createVector3f();
        bounds[0].setX(Float.POSITIVE_INFINITY);
        bounds[0].setY(Float.POSITIVE_INFINITY);
        bounds[0].setZ(Float.POSITIVE_INFINITY);

        bounds[1] = GeometryFactory.eINSTANCE.createVector3f();
        bounds[1].setX(Float.NEGATIVE_INFINITY);
        bounds[1].setY(Float.NEGATIVE_INFINITY);
        bounds[1].setZ(Float.NEGATIVE_INFINITY);
        return bounds;
    }

    private static void processExtends(GeometryInfo geometryInfo, double[] transformationMatrix, FloatPointer vertices, int index, Vector3f[] boundOut) {
        double x = vertices.get(index);
        double y = vertices.get(index + 1);
        double z = vertices.get(index + 2);
        double[] result = new double[4];
        Matrix.multiplyMV(result, 0, transformationMatrix, 0, new double[] { x, y, z, 1 }, 0);
        x = result[0];
        y = result[1];
        z = result[2];
        Bounds bounds = geometryInfo.getBounds();
        bounds.getMin().setX(Math.min(x, bounds.getMin().getX()));
        bounds.getMin().setY(Math.min(y, bounds.getMin().getY()));
        bounds.getMin().setZ(Math.min(z, bounds.getMin().getZ()));
        bounds.getMax().setX(Math.max(x, bounds.getMax().getX()));
        bounds.getMax().setY(Math.max(y, bounds.getMax().getY()));
        bounds.getMax().setZ(Math.max(z, bounds.getMax().getZ()));
        boundOut[0].setX(Math.min(x, boundOut[0].getX()));
        boundOut[0].setY(Math.min(y, boundOut[0].getY()));
        boundOut[0].setZ(Math.min(z, boundOut[0].getZ()));
        boundOut[1].setX(Math.max(x, boundOut[1].getX()));
        boundOut[1].setY(Math.max(y, boundOut[1].getY()));
        boundOut[1].setZ(Math.max(z, boundOut[1].getZ()));
    }

    private static String toString(BytePointer pointer) {
        try {
            return pointer.getString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("This is impossible!", e);
        }
    }

    public static GeometryInfo createGeometryInfo(IfcModelInterface model, IfcGeomIterator.GeomData data, GeometryFactory factory) {
        return _createGeometryInfo(model, factory, data.id(), toString(data.guid()), toString(data.name()), toString(data.type()),  data.area(), data.volume(), data.indices(), data.szIndices(), data.positions(), data.szPositions(), data.normals(), data.szNormals(), data.materialIndices(), data.szMaterialIndices(), data.colors(), data.szColors(), data.matrix());
    }

    public static GeometryInfo createGeometryInfo(IfcModelInterface model, me.cxj.ifc.ifc2x3.IfcGeomIterator.GeomData data, GeometryFactory factory) {
        return _createGeometryInfo(model, factory, data.id(), toString(data.guid()), toString(data.name()), toString(data.type()), data.area(), data.volume(), data.indices(), data.szIndices(), data.positions(), data.szPositions(), data.normals(), data.szNormals(), data.materialIndices(), data.szMaterialIndices(), data.colors(), data.szColors(), data.matrix());
    }

    private static GeometryInfo _createGeometryInfo(
            IfcModelInterface model,
            GeometryFactory factory,
            int oid, String guid, String name, String type,
            double area, double volume,
            IntPointer indices, long szIndices,
            FloatPointer positions, long szPositions,
            FloatPointer normals, long szNormals,
            IntPointer materialIndices, long szMaterialIndices,
            FloatPointer colors, long szColors,
            DoublePointer matrix
    ) {
        try {
            GeometryInfo geometryInfo = factory.createGeometryInfo();
            geometryInfo.setArea(area);
            if (volume < 0d) {
                volume = -volume;
            }
            geometryInfo.setVolume(volume);

            GeometryData geometryData = factory.createGeometryData();

            geometryData.setIndices(createBuffer(indices, szIndices));
            geometryData.setVertices(createBuffer(positions, szPositions));
            geometryData.setNormals(createBuffer(normals, szNormals));
            int nVerts = geometryData.getVertices().getData().length / 12;
            assert nVerts == szPositions / 3;
            int nInd = geometryData.getIndices().getData().length / 4;
            assert nInd == szIndices;
            assert nInd >= nVerts;
            geometryInfo.setPrimitiveCount(nInd / 3);

            if (colors != null && !colors.isNull()) {
                boolean hasMaterial = false;

                float[] vertex_colors = new float[nVerts * 4];
                materialIndices.position(0);
                for (int i = 0; i < szMaterialIndices; ++i) {
                    int c = materialIndices.get(i);
                    for (int j = 0; j < 3; ++j) {
                        int k = indices.get(i * 3 + j);
                        if (c > -1) {
                            hasMaterial = true;
                            for (int l = 0; l < 4; ++l) {
                                vertex_colors[4 * k + l] = colors.get(4 * c + l);
                            }
                        }
                    }
                }
                if (hasMaterial) {
                    ByteBuffer colorBuffer = ByteBuffer.wrap(new byte[vertex_colors.length * 4]);
                    colorBuffer.order(ByteOrder.LITTLE_ENDIAN);
                    colorBuffer.asFloatBuffer().put(vertex_colors);
                    Buffer colorsQuantizedBuffer = factory.createBuffer();
                    colorsQuantizedBuffer.setData(colorBuffer.array());
                    geometryData.setColorsQuantized(colorsQuantizedBuffer);
                }
            }

            double[] transformationMatrix = new double[16];
            Matrix.setIdentityM(transformationMatrix, 0);
            if (matrix != null && !matrix.isNull()) {
                matrix.get(transformationMatrix);
            }
            transformationMatrix = Matrix.changeOrientation(transformationMatrix);
            if (szIndices > 0) {
                Bounds bounds = factory.createBounds();
                bounds.setMin(createVector3f(Double.POSITIVE_INFINITY));
                bounds.setMax(createVector3f(-Double.POSITIVE_INFINITY));
                geometryInfo.setBounds(bounds);
                indices.position(0);
                Vector3f[] extendsBound = initExtendsBound();
                for (int i = 0; i < szIndices; i++) {
                    processExtends(geometryInfo, transformationMatrix, positions, indices.get(i) * 3, extendsBound);
                }
            }
            geometryInfo.setData(geometryData);
            ByteBuffer bfTransformationMatrix = ByteBuffer.allocate(16 * 8);
            bfTransformationMatrix.order(ByteOrder.LITTLE_ENDIAN);
            bfTransformationMatrix.asDoubleBuffer().put(transformationMatrix);
            geometryInfo.setTransformation(bfTransformationMatrix.array());
            return geometryInfo;
        } catch (Throwable e) {
            String msg = "[" + oid + "]" + type + "/" + name + "(" + guid + "): Unable to create geom data.";
            throw new RuntimeException(msg, e);
        }
    }
}
