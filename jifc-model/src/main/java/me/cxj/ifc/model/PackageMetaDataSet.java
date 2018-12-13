package me.cxj.ifc.model;

import org.bimserver.emf.PackageMetaData;
import org.bimserver.emf.Schema;
import org.bimserver.models.ifc2x3tc1.Ifc2x3tc1Package;
import org.bimserver.models.ifc4.Ifc4Package;
import org.eclipse.emf.ecore.EPackage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by vipcxj on 2018/11/19.
 */
public enum PackageMetaDataSet {

    IFC2x3TC1(Ifc2x3tc1Package.eINSTANCE, Schema.IFC2X3TC1), IFC4(Ifc4Package.eINSTANCE, Schema.IFC4);

    private PackageMetaData metaData;

    PackageMetaDataSet(EPackage ePackage, Schema schema) {
        try {
            Path tmpDir = Files.createTempDirectory("ifc");
            this.metaData = new PackageMetaData(ePackage, schema, tmpDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public PackageMetaData getMetaData() {
        return metaData;
    }
}
