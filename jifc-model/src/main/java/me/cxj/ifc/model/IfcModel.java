package me.cxj.ifc.model;

import me.cxj.ifc.serializer.IfcStepSerializer;
import me.cxj.ifc.utils.ModelUtils;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.models.ifc4.IfcProject;
import org.bimserver.plugins.serializers.SerializerException;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Created by vipcxj on 2018/12/14.
 */
public interface IfcModel extends IfcModelInterface {

    void generateGeomData(byte[] data);
    default void generateGeomData() {
        int capacity = getObjects().size() * 32;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(capacity)){
            try (OutputStream os = new BufferedOutputStream(baos)){
                new IfcStepSerializer(getPackageMetaData()).write(this, os, null);
                generateGeomData(baos.toByteArray());
            }
        } catch (IOException | SerializerException e) {
            throw new RuntimeException(e);
        }
    }
    void importGeomData(GeomModel geomModel);
    GeomModel exportGeomData();
    boolean hasGeomData();
    default boolean isIfc4() {
        return "IFC4".equalsIgnoreCase(getPackageMetaData().getSchema().getEPackageName());
    }
    void inspect(OutputStream os) throws IOException;

    default double[] getTrueNorth() {
        if (isIfc4()) {
            List<IfcProject> projects = getAllWithSubTypes(IfcProject.class);
            if (projects == null || projects.isEmpty()) {
                throw new IllegalArgumentException("No project in this ifc model.");
            }
            return ModelUtils.getTrueNorth(projects.get(0));
        } else {
            List<org.bimserver.models.ifc2x3tc1.IfcProject> projects = getAllWithSubTypes(org.bimserver.models.ifc2x3tc1.IfcProject.class);
            if (projects == null || projects.isEmpty()) {
                throw new IllegalArgumentException("No project in this ifc model.");
            }
            return ModelUtils.getTrueNorth(projects.get(0));
        }
    }
}
