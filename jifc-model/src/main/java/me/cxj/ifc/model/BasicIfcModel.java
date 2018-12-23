package me.cxj.ifc.model;

import com.google.gson.Gson;
import me.cxj.ifc.utils.GeomUtils;
import me.cxj.ifc.utils.ModelUtils;
import org.bimserver.emf.IdEObject;
import org.bimserver.emf.PackageMetaData;
import org.bimserver.models.ifc2x3tc1.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by vipcxj on 2018/11/16.
 */
public class BasicIfcModel extends AbstractIfcModel {

    private boolean hasGeom = false;

    public BasicIfcModel(PackageMetaData packageMetaData, Map<Integer, Long> pidRoidMap) {
        super(packageMetaData, pidRoidMap);
    }

    public BasicIfcModel(PackageMetaData packageMetaData, Map<Integer, Long> pidRoidMap, int size) {
        super(packageMetaData, pidRoidMap, size);
    }

    @Override
    public void load(IdEObject idEObject) {
    }

    @Override
    public void generateGeomData(byte[] data) {
        GeomUtils.generateGeomData(this, data);
        hasGeom = true;
    }

    @Override
    public void importGeomData(GeomModel geomModel) {
        geomModel.fillModel(this);
        hasGeom = true;
    }

    @Override
    public GeomModel exportGeomData() {
        return GeomModel.fromModel(this);
    }

    @Override
    public boolean hasGeomData() {
        return hasGeom;
    }

    private static void setProperty(Map<String, Object> obj, String name, Object value) {
        if (value != null) {
            obj.put(name, value);
        }
    }

    private Map<String, Object> getAllProperties(IfcObject object) {
        Map<String, Object> properties = new HashMap<>();
        IfcTypeObject type = ModelUtils.getTypeObject(object);
        if (type != null) {
            for (IfcPropertySetDefinition propertySetDefinition : type.getHasPropertySets()) {
                if (propertySetDefinition instanceof IfcPropertySet) {
                    for (IfcProperty property : ((IfcPropertySet) propertySetDefinition).getHasProperties()) {
                        properties.put(propertySetDefinition.getName() + "." + property.getName(), ModelUtils.propertyToJavaObject(property));
                    }
                }
            }
        }
        for (IfcRelDefines relDefines : object.getIsDefinedBy()) {
            if (relDefines instanceof IfcRelDefinesByProperties) {
                IfcPropertySetDefinition propertySetDefinition = ((IfcRelDefinesByProperties) relDefines).getRelatingPropertyDefinition();
                if (propertySetDefinition instanceof IfcPropertySet) {
                    for (IfcProperty property : ((IfcPropertySet) propertySetDefinition).getHasProperties()) {
                        properties.put(propertySetDefinition.getName() + "." + property.getName(), ModelUtils.propertyToJavaObject(property));
                    }
                }
            }
        }
        return properties;
    }



    private Object createEntity(IfcObject object, Map<String, Object> cache) {
        if (cache.containsKey(object.getGlobalId())) {
            Map<String, String> ref = new HashMap<>();
            ref.put("ref", object.getGlobalId());
            return ref;
        }
        Map<String, Object> obj = new HashMap<>();
        obj.put("guid", object.getGlobalId());
        obj.put("name", object.getName());
        obj.put("description", object.getDescription());
        obj.put("type", object.getObjectType() != null ? object.getObjectType() : object.eClass().getName());
        obj.put("properties", getAllProperties(object));
        List<Object> aggregates = new ArrayList<>();
        List<Object> nests = new ArrayList<>();
        for (IfcRelDecomposes relDecomposes : object.getIsDecomposedBy()) {
            if (relDecomposes instanceof IfcRelAggregates) {
                aggregates.addAll(relDecomposes.getRelatedObjects().stream().filter(o -> o instanceof IfcObject).map(o -> createEntity((IfcObject) o, cache)).collect(Collectors.toList()));
            }
            if (relDecomposes instanceof IfcRelNests) {
                nests.addAll(relDecomposes.getRelatedObjects().stream().filter(o -> o instanceof IfcObject).map(o -> createEntity((IfcObject) o, cache)).collect(Collectors.toList()));
            }
        }
        obj.put("aggregates", aggregates);
        obj.put("nests", nests);
        if (object instanceof IfcSpatialStructureElement) {
            List<Object> contains = new ArrayList<>();
            for (IfcRelContainedInSpatialStructure relContainedInSpatialStructure : ((IfcSpatialStructureElement) object).getContainsElements()) {
                contains.addAll(relContainedInSpatialStructure.getRelatedElements().stream().map(o -> createEntity(o, cache)).collect(Collectors.toList()));
            }
            obj.put("contains", contains);
            List<Object> references = new ArrayList<>();
            for (IfcRelReferencedInSpatialStructure relReferencedInSpatialStructure : ((IfcSpatialStructureElement) object).getReferencesElements()) {
                references.addAll(relReferencedInSpatialStructure.getRelatedElements().stream().map(o -> createEntity(o, cache)).collect(Collectors.toList()));
            }
            obj.put("references", references);
        }
        if (object instanceof IfcProject) {
            IfcProject project = (IfcProject) object;
            obj.put("trueNorth", ModelUtils.getTrueNorth(project));
        }
        cache.put(object.getGlobalId(), obj);
        return obj;
    }



    @Override
    public void inspect(OutputStream os) throws IOException {
        if (!isIfc4()) {
            List<IfcProject> projects = getAllWithSubTypes(IfcProject.class);
            if (!projects.isEmpty()) {
                IfcProject project = projects.get(0);
                Map<String, Object> cache = new HashMap<>();
                Object root = createEntity(project, cache);
                Gson gson = new Gson();
                OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8);
                gson.toJson(root, writer);
                writer.flush();
                // gson.toJson(root, new OutputStreamWriter(os, StandardCharsets.UTF_8));
            }
        }
    }
}
