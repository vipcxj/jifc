package me.cxj.ifc.ifc4.presets;

import org.bytedeco.javacpp.annotation.Platform;
import org.bytedeco.javacpp.annotation.Properties;
import org.bytedeco.javacpp.tools.Info;
import org.bytedeco.javacpp.tools.InfoMap;
import org.bytedeco.javacpp.tools.InfoMapper;

/**
 * Created by vipcxj on 2018/12/3.
 */
@Properties(value = {
        @Platform(include = {"IfcGeomDataIterator.hpp"}, link = {"IfcGeom_ifc4"}, preload = {"IfcGeom_ifc4"})
}, target = "me.cxj.ifc.ifc4.IfcGeomIterator")
public class IfcGeomIterator implements InfoMapper {

    public void map(InfoMap infoMap) {
        infoMap.put(new Info("DllExport").skip());
    }
}
