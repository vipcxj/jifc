package me.cxj.ifc.serializer;

import me.cxj.ifc.deserializer.IfcParserWriterUtils;
import me.cxj.ifc.model.IfcModel;
import nl.tue.buildingsmart.schema.EntityDefinition;
import org.bimserver.emf.IdEObject;
import org.bimserver.emf.IdEObjectImpl;
import org.bimserver.emf.PackageMetaData;
import org.bimserver.models.store.IfcHeader;
import org.bimserver.plugins.serializers.ProgressReporter;
import org.bimserver.plugins.serializers.SerializerException;
import org.bimserver.utils.StringUtils;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by vipcxj on 2018/12/14.
 */
public class IfcStepSerializer implements IfcSerializer {
    private static final Logger LOGGER = Logger.getLogger(IfcStepSerializer.class.getName());
    private static final byte[] NEW_LINE = "\n".getBytes(StandardCharsets.UTF_8);
    private static final EcorePackage ECORE_PACKAGE_INSTANCE = EcorePackage.eINSTANCE;
    private static final String NULL = "NULL";
    private static final String OPEN_CLOSE_PAREN = "()";
    private static final String ASTERISK = "*";
    private static final String PAREN_CLOSE_SEMICOLON = ");";
    private static final String DASH = "#";
    private static final String IFC_LOGICAL = "IfcLogical";
    private static final String IFC_BOOLEAN = "IfcBoolean";
    private static final String DOT = ".";
    private static final String COMMA = ",";
    private static final String OPEN_PAREN = "(";
    private static final String CLOSE_PAREN = ")";
    private static final String BOOLEAN_UNDEFINED = ".U.";
    private static final String DOLLAR = "$";
    private static final String WRAPPED_VALUE = "wrappedValue";
    private PackageMetaData metaData;
    private String headerSchema;
    private int expressIdCounter;

    public enum Mode {
        HEADER, DATA, FOOTER, DONE
    }

    public IfcStepSerializer(PackageMetaData metaData) {
        this.metaData = metaData;
        this.headerSchema = metaData.getSchema().getHeaderName();
    }

    @Override
    public void write(IfcModel model, OutputStream os, ProgressReporter reporter) throws SerializerException {
        try {
            long wc = 0;
            writeHeader(model, os);
            expressIdCounter = 1;
            Iterator<IdEObject> iterator = model.getValues().iterator();
            while (iterator.hasNext()) {
                IdEObject next = iterator.next();
                while (next.eClass().getEPackage() != metaData.getEPackage() && iterator.hasNext()) {
                    next = iterator.next();
                }
                write(model, next, os);
                if (reporter != null) {
                    reporter.update(++wc, model.size());
                }
            }
            writeFooter(os);
            if (reporter != null) {
                reporter.update(model.size(), model.size());
            }
        } catch (IOException e) {
            throw new SerializerException(e);
        }
    }

    protected int getExpressId(IdEObject object) {
        if (object.getExpressId() == -1) {
            ((IdEObjectImpl)object).setExpressId(expressIdCounter ++);
        }
        return object.getExpressId();
    }

    private void println(OutputStream os, String line) throws IOException {
        byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
        os.write(bytes, 0, bytes.length);
        os.write(NEW_LINE, 0, NEW_LINE.length);
    }

    private void print(OutputStream os, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        os.write(bytes, 0, bytes.length);
    }

    private void writeHeader(IfcModel model, OutputStream os) throws IOException {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        println(os, "ISO-10303-21;");
        println(os, "HEADER;");
        IfcHeader ifcHeader = model.getModelMetaData().getIfcHeader();
        if (ifcHeader == null) {
            Date date = new Date();
            println(os, "FILE_DESCRIPTION ((''), '2;1');");
            println(os, "FILE_NAME ('', '" + dateFormatter.format(date) + "', (''), (''), '', 'BIMserver', '');");
            println(os, "FILE_SCHEMA (('" + headerSchema + "'));");
        } else {
            print(os, "FILE_DESCRIPTION ((");
            print(os, StringUtils.concat(ifcHeader.getDescription(), "'", ", "));
            println(os, "), '" + ifcHeader.getImplementationLevel() + "');");
            println(os, "FILE_NAME ('" + ifcHeader.getFilename().replace("\\", "\\\\") + "', '" + dateFormatter.format(ifcHeader.getTimeStamp()) + "', (" + StringUtils.concat(ifcHeader.getAuthor(), "'", ", ") + "), (" + StringUtils.concat(ifcHeader.getOrganization(), "'", ", ") + "), '" + ifcHeader.getPreProcessorVersion() + "', '" + ifcHeader.getOriginatingSystem() + "', '"	+ ifcHeader.getAuthorization() + "');");

            //	println("FILE_SCHEMA (('" + ifcHeader.getIfcSchemaVersion() + "'));");
            println(os, "FILE_SCHEMA (('" + headerSchema + "'));");
        }
        println(os, "ENDSEC;");
        println(os, "DATA;");
        // println("//This program comes with ABSOLUTELY NO WARRANTY.");
        // println("//This is free software, and you are welcome to redistribute it under certain conditions. See www.bimserver.org <http://www.bimserver.org>");
    }

    private void writeFooter(OutputStream os) throws IOException {
        println(os, "ENDSEC;");
        println(os, "END-ISO-10303-21;");
    }

    private void writeEnum(EObject object, EStructuralFeature feature, OutputStream os) throws SerializerException, IOException {
        Object val = object.eGet(feature);
        if (feature.getEType().getName().equals("Tristate")) {
            IfcParserWriterUtils.writePrimitive(val, os);
        } else {
            if (val == null) {
                print(os, DOLLAR);
            } else {
                if (val.toString().equals(NULL)) {
                    print(os, DOLLAR);
                } else {
                    print(os, DOT);
                    print(os, val.toString());
                    print(os, DOT);
                }
            }
        }
    }

    private void writeEmbedded(IfcModel model, EObject eObject, OutputStream os) throws SerializerException, IOException {
        EClass class1 = eObject.eClass();
        print(os, metaData.getUpperCase(class1));
        print(os, OPEN_PAREN);
        EStructuralFeature structuralFeature = class1.getEStructuralFeature(WRAPPED_VALUE);
        if (structuralFeature != null) {
            Object realVal = eObject.eGet(structuralFeature);
            if (structuralFeature.getEType() == ECORE_PACKAGE_INSTANCE.getEDouble()) {
                writeDoubleValue(model, (Double)realVal, eObject, structuralFeature, os);
            } else {
                IfcParserWriterUtils.writePrimitive(realVal, os);
            }
        }
        print(os, CLOSE_PAREN);
    }

    @SuppressWarnings("ConstantConditions")
    private void writeList(IfcModel model, IdEObject object, EStructuralFeature feature, OutputStream os) throws SerializerException, IOException {
        List<?> list = (List<?>) object.eGet(feature);
        List<?> doubleStingList = null;
        if (feature.getEType() == EcorePackage.eINSTANCE.getEDouble() && model.isUseDoubleStrings()) {
            EStructuralFeature doubleStringFeature = feature.getEContainingClass().getEStructuralFeature(feature.getName() + "AsString");
            if (doubleStringFeature == null) {
                throw new SerializerException("Field " + feature.getName() + "AsString" + " not found");
            }
            doubleStingList = (List<?>) object.eGet(doubleStringFeature);
        }
        if (list.isEmpty()) {
            if (!feature.isUnsettable()) {
                print(os, OPEN_CLOSE_PAREN);
            } else {
                print(os, "$");
            }
        } else {
            print(os, OPEN_PAREN);
            boolean first = true;
            int index = 0;
            for (Object listObject : list) {
                if (!first) {
                    print(os, COMMA);
                }
                if ((listObject instanceof IdEObject) && model.contains((IdEObject)listObject)) {
                    IdEObject eObject = (IdEObject) listObject;
                    print(os, DASH);
                    print(os, String.valueOf(getExpressId(eObject)));
                } else {
                    if (listObject == null) {
                        print(os, DOLLAR);
                    } else {
                        if (listObject instanceof IdEObject && feature.getEType().getEAnnotation("wrapped") != null) {
                            IdEObject eObject = (IdEObject) listObject;
                            Object realVal = eObject.eGet(eObject.eClass().getEStructuralFeature("wrappedValue"));
                            if (realVal instanceof Double) {
                                if (model.isUseDoubleStrings()) {
                                    Object stringVal = eObject.eGet(eObject.eClass().getEStructuralFeature("wrappedValueAsString"));
                                    if (stringVal != null) {
                                        print(os, (String) stringVal);
                                    } else {
                                        IfcParserWriterUtils.writePrimitive(realVal, os);
                                    }
                                } else {
                                    IfcParserWriterUtils.writePrimitive(realVal, os);
                                }
                            } else {
                                IfcParserWriterUtils.writePrimitive(realVal, os);
                            }
                        } else if (listObject instanceof EObject) {
                            IdEObject eObject = (IdEObject) listObject;
                            EClass class1 = eObject.eClass();
                            EStructuralFeature structuralFeature = class1.getEStructuralFeature(WRAPPED_VALUE);
                            if (structuralFeature != null) {
                                Object realVal = eObject.eGet(structuralFeature);
                                print(os, metaData.getUpperCase(class1));
                                print(os, OPEN_PAREN);
                                if (realVal instanceof Double) {
                                    writeDoubleValue(model, (Double)realVal, eObject, structuralFeature, os);
                                } else {
                                    IfcParserWriterUtils.writePrimitive(realVal, os);
                                }
                                print(os, CLOSE_PAREN);
                            } else {
                                if (feature.getEAnnotation("twodimensionalarray") != null) {
                                    writeList(model, eObject, eObject.eClass().getEStructuralFeature("List"), os);
                                } else {
                                    LOGGER.info("Unfollowable reference found from " + object + "(" + object.getOid() + ")." + feature.getName() + " to " + eObject + "(" + eObject.getOid() + ")");
                                }
                            }
                        } else {
                            if (doubleStingList != null) {
                                if (index < doubleStingList.size()) {
                                    String val = (String)doubleStingList.get(index);
                                    if (val == null) {
                                        IfcParserWriterUtils.writePrimitive(listObject, os);
                                    } else {
                                        print(os, val);
                                    }
                                } else {
                                    IfcParserWriterUtils.writePrimitive(listObject, os);
                                }
                            } else {
                                IfcParserWriterUtils.writePrimitive(listObject, os);
                            }
                        }
                    }
                }
                first = false;
                index++;
            }
            print(os, CLOSE_PAREN);
        }
    }

    private void writeWrappedValue(IfcModel model, EObject object, EStructuralFeature feature, EClass ec, OutputStream os) throws SerializerException, IOException {
        Object get = object.eGet(feature);
        boolean isWrapped = ec.getEAnnotation("wrapped") != null;
        EStructuralFeature structuralFeature = ec.getEStructuralFeature(WRAPPED_VALUE);
        if (get instanceof EObject) {
            boolean isDefinedWrapped = feature.getEType().getEAnnotation("wrapped") != null;
            EObject betweenObject = (EObject) get;
            if (isWrapped && isDefinedWrapped) {
                Object val = betweenObject.eGet(structuralFeature);
                String name = structuralFeature.getEType().getName();
                if ((name.equals(IFC_BOOLEAN) || name.equals(IFC_LOGICAL)) && val == null) {
                    print(os, BOOLEAN_UNDEFINED);
                } else if (structuralFeature.getEType() == ECORE_PACKAGE_INSTANCE.getEDouble()) {
                    writeDoubleValue(model, (Double)val, betweenObject, feature, os);
                } else {
                    IfcParserWriterUtils.writePrimitive(val, os);
                }
            } else {
                writeEmbedded(model, betweenObject, os);
            }
        } else if (get instanceof EList<?>) {
            EList<?> list = (EList<?>) get;
            if (list.isEmpty()) {
                if (!feature.isUnsettable()) {
                    print(os, OPEN_CLOSE_PAREN);
                } else {
                    print(os, "$");
                }
            } else {
                print(os, OPEN_PAREN);
                boolean first = true;
                for (Object o : list) {
                    if (!first) {
                        print(os, COMMA);
                    }
                    EObject object2 = (EObject) o;
                    Object val = object2.eGet(structuralFeature);
                    if (structuralFeature.getEType() == ECORE_PACKAGE_INSTANCE.getEDouble()) {
                        writeDoubleValue(model, (Double)val, object2, structuralFeature, os);
                    } else {
                        IfcParserWriterUtils.writePrimitive(val, os);
                    }
                    first = false;
                }
                print(os, CLOSE_PAREN);
            }
        } else {
            if (get == null) {
                EClassifier type = structuralFeature.getEType();
                if (type.getName().equals("IfcBoolean") || type.getName().equals("IfcLogical") || type == ECORE_PACKAGE_INSTANCE.getEBoolean()) {
                    print(os, BOOLEAN_UNDEFINED);
                } else {
                    EntityDefinition entityBN = metaData.getSchemaDefinition().getEntityBN(object.eClass().getName());
                    if (entityBN != null && entityBN.isDerived(feature.getName())) {
                        print(os, ASTERISK);
                    } else {
                        print(os, DOLLAR);
                    }
                }
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void writeEClass(IfcModel model, IdEObject object, EStructuralFeature feature, OutputStream os) throws SerializerException, IOException {
        Object referencedObject = object.eGet(feature);
        if (referencedObject instanceof IdEObject && ((IdEObject)referencedObject).eClass().getEAnnotation("wrapped") != null) {
            writeWrappedValue(model, object, feature, ((EObject)referencedObject).eClass(), os);
        } else {
            if (referencedObject instanceof EObject && model.contains((IdEObject) referencedObject)) {
                print(os, DASH);
                print(os, String.valueOf(getExpressId((IdEObject) referencedObject)));
            } else {
                EntityDefinition entityBN = metaData.getSchemaDefinition().getEntityBN(object.eClass().getName());
                writeEDataType(model, object, entityBN, feature, os);
            }
        }
    }

    private void writeObject(IfcModel model, IdEObject object, EStructuralFeature feature, OutputStream os) throws SerializerException, IOException {
        Object ref = object.eGet(feature);
        if (ref == null || (feature.isUnsettable() && !object.eIsSet(feature))) {
            EClassifier type = feature.getEType();
            if (type instanceof EClass) {
                EStructuralFeature structuralFeature = ((EClass) type).getEStructuralFeature(WRAPPED_VALUE);
                if (structuralFeature != null) {
                    String name = structuralFeature.getEType().getName();
                    if (name.equals(IFC_BOOLEAN) || name.equals(IFC_LOGICAL) || structuralFeature.getEType() == EcorePackage.eINSTANCE.getEBoolean()) {
                        print(os, BOOLEAN_UNDEFINED);
                    } else {
                        print(os, DOLLAR);
                    }
                } else {
                    print(os, DOLLAR);
                }
            } else {
                if (type == EcorePackage.eINSTANCE.getEBoolean()) {
                    print(os, BOOLEAN_UNDEFINED);
                } else if (feature.isMany()) {
                    print(os, "()");
                } else {
                    print(os, DOLLAR);
                }
            }
        } else {
            if (ref instanceof EObject) {
                writeEmbedded(model, (EObject) ref, os);
            } else if (feature.getEType() == ECORE_PACKAGE_INSTANCE.getEDouble()) {
                writeDoubleValue(model, (Double) ref, object, feature, os);
            } else {
                IfcParserWriterUtils.writePrimitive(ref, os);
            }
        }
    }

    private void writeDoubleValue(IfcModel model, double value, EObject object, EStructuralFeature feature, OutputStream os) throws SerializerException, IOException {
        if (model.isUseDoubleStrings()) {
            Object stringValue = object.eGet(object.eClass().getEStructuralFeature(feature.getName() + "AsString"));
            if (stringValue != null) {
                print(os, (String) stringValue);
                return;
            }
        }
        IfcParserWriterUtils.writePrimitive(value, os);
    }

    private void writeEDataType(IfcModel model, IdEObject object, EntityDefinition entityBN, EStructuralFeature feature, OutputStream os) throws SerializerException, IOException {
        if (entityBN != null && entityBN.isDerived(feature.getName())) {
            print(os, ASTERISK);
        } else if (feature.isMany()) {
            writeList(model, object, feature, os);
        } else {
            writeObject(model, object, feature, os);
        }
    }

    private void write(IfcModel model, IdEObject object, OutputStream os) throws SerializerException, IOException {
        EClass eClass = object.eClass();
        if (eClass.getEAnnotation("hidden") != null) {
            return;
        }
        print(os, DASH);
        int convertedKey = getExpressId(object);
        print(os, String.valueOf(convertedKey));
        print(os, "= ");
        String upperCase = metaData.getUpperCase(eClass);
        if (upperCase == null) {
            throw new SerializerException("Type not found: " + eClass.getName());
        }
        print(os, upperCase);
        print(os, OPEN_PAREN);
        boolean isFirst = true;
        EntityDefinition entityBN = metaData.getSchemaDefinition().getEntityBN(object.eClass().getName());
        for (EStructuralFeature feature : eClass.getEAllStructuralFeatures()) {
            if (feature.getEAnnotation("hidden") == null && (entityBN != null && (!entityBN.isDerived(feature.getName()) || entityBN.isDerivedOverride(feature.getName())))) {
                EClassifier type = feature.getEType();
                if (type instanceof EEnum) {
                    if (!isFirst) {
                        print(os, COMMA);
                    }
                    writeEnum(object, feature, os);
                    isFirst = false;
                } else if (type instanceof EClass) {
                    EReference eReference = (EReference)feature;
                    if (!metaData.isInverse(eReference)) {
                        if (!isFirst) {
                            print(os, COMMA);
                        }
                        writeEClass(model, object, feature, os);
                        isFirst = false;
                    }
                } else if (type instanceof EDataType) {
                    if (!isFirst) {
                        print(os, COMMA);
                    }
                    writeEDataType(model, object, entityBN, feature, os);
                    isFirst = false;
                }
            }
        }
        println(os, PAREN_CLOSE_SEMICOLON);
    }
}
