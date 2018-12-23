package me.cxj.ifc.serializer;

import me.cxj.ifc.model.IfcModel;
import org.bimserver.plugins.serializers.ProgressReporter;
import org.bimserver.plugins.serializers.SerializerException;

import java.io.OutputStream;

/**
 * Created by vipcxj on 2018/12/14.
 */
public interface IfcSerializer {
    void write(IfcModel model, OutputStream outputStream, ProgressReporter reporter) throws SerializerException;
}
