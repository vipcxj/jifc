package me.cxj.ifc.deserializer;

import me.cxj.ifc.model.IfcModel;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.plugins.deserializers.ByteProgressReporter;
import org.bimserver.plugins.deserializers.DeserializeException;

import java.io.InputStream;

/**
 * Created by vipcxj on 2018/11/19.
 */
public interface IfcDeserializer {

    IfcModel read(InputStream inputStream, ByteProgressReporter reporter) throws DeserializeException;
}
