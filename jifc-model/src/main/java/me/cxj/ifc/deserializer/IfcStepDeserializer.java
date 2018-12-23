package me.cxj.ifc.deserializer;

import me.cxj.ifc.model.BasicIfcModel;
import me.cxj.ifc.model.IfcModel;
import nl.tue.buildingsmart.schema.Attribute;
import nl.tue.buildingsmart.schema.EntityDefinition;
import nl.tue.buildingsmart.schema.ExplicitAttribute;
import org.bimserver.emf.*;
import org.bimserver.models.ifc4.Ifc4Package;
import org.bimserver.models.store.IfcHeader;
import org.bimserver.models.store.StoreFactory;
import org.bimserver.plugins.deserializers.ByteProgressReporter;
import org.bimserver.plugins.deserializers.DeserializeException;
import org.bimserver.shared.ListWaitingObject;
import org.bimserver.shared.SingleWaitingObject;
import org.bimserver.shared.WaitingList;
import org.bimserver.utils.StringUtils;
import org.eclipse.emf.common.util.AbstractEList;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.impl.EClassImpl;
import org.eclipse.emf.ecore.impl.EEnumImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Optional;

/**
 * Created by vipcxj on 2018/11/19.
 */
@SuppressWarnings("StatementWithEmptyBody")
public class IfcStepDeserializer implements IfcDeserializer {
    /*
     * The following hacks are present
     *
     * - For every feature of type double there is an extra feature (name
     * appended with "AsString") of type String to keep the original String
     * version this is also done for aggregate features - WrappedValues for all
     * for derived primitive types and enums that are used in a "select"
     */

    private static final int AVERAGE_LINE_LENGTH = 58;
    private static final String WRAPPED_VALUE = "wrappedValue";
    private final WaitingList<Integer> waitingList = new WaitingList<>();
    private Mode mode = Mode.HEADER;
    private IfcModel model;
    private int lineNumber;
    private PackageMetaData metaData;

    public enum Mode {
        HEADER, DATA, FOOTER, DONE
    }

    public IfcStepDeserializer(PackageMetaData metaData) {
        this.metaData = metaData;
    }

    public PackageMetaData getPackageMetaData() {
        return metaData;
    }

    public Schema getSchema() {
        return metaData.getSchema();
    }

    @Override
    public IfcModel read(InputStream inputStream, ByteProgressReporter reporter) throws DeserializeException {
        model = new BasicIfcModel(metaData, null);
        long bytesRead = 0;
        lineNumber = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))){
            StringBuilder line = Optional.ofNullable(reader.readLine()).map(StringBuilder::new).orElse(null);
            if (line == null) {
                throw new DeserializeException("Unexpected end of stream reading first line " + model);
            }
            MessageDigest md = MessageDigest.getInstance("MD5");
            while (line != null) {
                byte[] bytes = line.toString().getBytes(StandardCharsets.UTF_8);
                md.update(bytes, 0, bytes.length);
                try {
                    while (!processLine(line.toString().trim())) {
                        String readLine = reader.readLine();
                        if (readLine == null) {
                            break;
                        }
                        line.append(readLine);
                        lineNumber++;
                    }
                } catch (Exception e) {
                    if (e instanceof DeserializeException) {
                        throw (DeserializeException)e;
                    } else {
                        throw new DeserializeException(lineNumber, " (" + e.getMessage() + ") " + line, e);
                    }
                }
                bytesRead += bytes.length;
                if (reporter != null) {
                    reporter.progress(bytesRead);
                }

                line = Optional.ofNullable(reader.readLine()).map(StringBuilder::new).orElse(null);
                lineNumber++;
            }
            model.getModelMetaData().setChecksum(md.digest());
            if (mode == Mode.HEADER) {
                throw new DeserializeException(lineNumber, "No valid IFC header found");
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new DeserializeException(lineNumber, e);
        }
        return model;
    }

    public IfcModelInterface getModel() {
        return model;
    }

    private boolean processLine(String line) throws DeserializeException, MetaDataException {
        switch (mode) {
            case HEADER:
                if (line.length() > 0) {
                    if (line.endsWith(";")) {
                        processHeader(line);
                    } else {
                        return false;
                    }
                }
                if (line.equals("DATA;")) {
                    mode = Mode.DATA;
                }
                break;
            case DATA:
                if (line.equals("ENDSEC;")) {
                    mode = Mode.FOOTER;
                } else {
                    if (line.length() > 0 && line.charAt(0) == '#') {
                        while (line.endsWith("*/")) {
                            line = line.substring(0, line.lastIndexOf("/*")).trim();
                        }
                        if (line.endsWith(";")) {
                            processRecord(line);
                        } else {
                            return false;
                        }
                    }
                }
                break;
            case FOOTER:
                if (line.equals("ENDSEC;")) {
                    mode = Mode.DONE;
                }
                break;
            case DONE:
        }
        return true;
    }

    private void processHeader(String line) throws DeserializeException {
        try {
            IfcHeader ifcHeader = model.getModelMetaData().getIfcHeader();
            if (ifcHeader == null) {
                ifcHeader = StoreFactory.eINSTANCE.createIfcHeader();
                model.getModelMetaData().setIfcHeader(ifcHeader);
            }

            if (line.startsWith("FILE_DESCRIPTION")) {
                String filedescription = line.substring("FILE_DESCRIPTION".length()).trim();
                new IfcHeaderParser().parseDescription(filedescription.substring(1, filedescription.length() - 2), ifcHeader);
            } else if (line.startsWith("FILE_NAME")) {
                String filename = line.substring("FILE_NAME".length()).trim();
                new IfcHeaderParser().parseFileName(filename.substring(1, filename.length() - 2), ifcHeader);
            } else if (line.startsWith("FILE_SCHEMA")) {
                String fileschema = line.substring("FILE_SCHEMA".length()).trim();
                new IfcHeaderParser().parseFileSchema(fileschema.substring(1, fileschema.length() - 2), ifcHeader);

                String ifcSchemaVersion = ifcHeader.getIfcSchemaVersion();
                if (!ifcSchemaVersion.toLowerCase().equalsIgnoreCase(getSchema().getHeaderName().toLowerCase())) {
                    throw new DeserializeException(lineNumber, ifcSchemaVersion + " is not supported by this deserializer (" + getSchema().getHeaderName() + " is)");
                }
                ifcHeader.setIfcSchemaVersion(ifcSchemaVersion);
            } else if (line.startsWith("ENDSEC;")) {
                // Do nothing
            }
        } catch (ParseException e) {
            throw new DeserializeException(lineNumber, e);
        }
    }

    public void processRecord(String line) throws DeserializeException, MetaDataException {
        int equalSignLocation = line.indexOf("=");
        int lastIndexOfSemiColon = line.lastIndexOf(";");
        if (lastIndexOfSemiColon == -1) {
            throw new DeserializeException(lineNumber, "No semicolon found in line");
        }
        int indexOfFirstParen = line.indexOf("(", equalSignLocation);
        if (indexOfFirstParen == -1) {
            throw new DeserializeException(lineNumber, "No left parenthesis found in line");
        }
        int indexOfLastParen = line.lastIndexOf(")", lastIndexOfSemiColon);
        if (indexOfLastParen == -1) {
            throw new DeserializeException(lineNumber, "No right parenthesis found in line");
        }
        int recordNumber = Integer.parseInt(line.substring(1, equalSignLocation).trim());
        String name = line.substring(equalSignLocation + 1, indexOfFirstParen).trim();
        EClass eClass = (EClass) getPackageMetaData().getEClassifierCaseInsensitive(name);
        if (eClass != null) {
            IdEObject object = getPackageMetaData().create(eClass);
            try {
                model.add(recordNumber, object);
            } catch (IfcModelInterfaceException e) {
                throw new DeserializeException(lineNumber, e);
            }
            ((IdEObjectImpl) object).setExpressId(recordNumber);
            String realData = line.substring(indexOfFirstParen + 1, indexOfLastParen);
            int lastIndex = 0;
            EntityDefinition entityBN = getPackageMetaData().getSchemaDefinition().getEntityBN(name);
            if (entityBN == null) {
                throw new DeserializeException(lineNumber, "Unknown entity " + name);
            }
            for (EStructuralFeature eStructuralFeature : eClass.getEAllStructuralFeatures()) {
                if (getPackageMetaData().useForSerialization(eClass, eStructuralFeature)) {
                    if (getPackageMetaData().useForDatabaseStorage(eClass, eStructuralFeature)) {
                        int nextIndex = StringUtils.nextString(realData, lastIndex);
                        if (nextIndex <= lastIndex && eStructuralFeature == Ifc4Package.eINSTANCE.getIfcSurfaceStyleShading_Transparency()) {
                            // IFC4 add1/add2 hack
                            object.eSet(eStructuralFeature, 0D);
                            object.eSet(eStructuralFeature.getEContainingClass().getEStructuralFeature(eStructuralFeature.getName() + "AsString"), "0");
                            continue;
                        }
                        String val;
                        try {
                            val = realData.substring(lastIndex, nextIndex - 1).trim();
                        } catch (Exception e) {
                            int expected = 0;
                            for (Attribute attribute2 : entityBN.getAttributesCached(true)) {
                                if (attribute2 instanceof ExplicitAttribute) {
                                    expected++;
                                }
                            }
                            throw new DeserializeException(lineNumber, eClass.getName() + " expects " + expected + " fields, but less found");
                        }
                        lastIndex = nextIndex;
                        char firstChar = val.charAt(0);
                        if (firstChar == '$') {
                            object.eUnset(eStructuralFeature);
                            if (eStructuralFeature.getEType() == EcorePackage.eINSTANCE.getEDouble()) {
                                EStructuralFeature doubleStringFeature = eClass.getEStructuralFeature(eStructuralFeature.getName() + "AsString");
                                object.eUnset(doubleStringFeature);
                            }
                        } else if (firstChar == '#') {
                            readReference(val, object, eStructuralFeature);
                        } else if (firstChar == '.') {
                            readEnum(val, object, eStructuralFeature);
                        } else if (firstChar == '(') {
                            readList(val, object, eStructuralFeature);
                        } else if (firstChar == '*') {
                        } else {
                            if (!eStructuralFeature.isMany()) {
                                object.eSet(eStructuralFeature, convert(eStructuralFeature, eStructuralFeature.getEType(), val));
                                if (eStructuralFeature.getEType() == EcorePackage.eINSTANCE.getEDouble()) {
                                    EStructuralFeature doubleStringFeature = eClass.getEStructuralFeature(eStructuralFeature.getName() + "AsString");
                                    object.eSet(doubleStringFeature, val);
                                }
                            } else {
                                // It's not a list in the file, but it is in the
                                // schema??
                            }
                        }
                    } else {
                        lastIndex = StringUtils.nextString(realData, lastIndex);
                    }
                } else {
                    if (getPackageMetaData().useForDatabaseStorage(eClass, eStructuralFeature)) {
                        if (eStructuralFeature instanceof EReference && getPackageMetaData().isInverse((EReference) eStructuralFeature)) {
                            object.eUnset(eStructuralFeature);
                        } else {
                            if (eStructuralFeature.getEAnnotation("asstring") == null) {
                                object.eUnset(eStructuralFeature);
                            }
                        }
                    }
                }
            }
            if (waitingList.containsKey(recordNumber)) {
                waitingList.updateNode(recordNumber, eClass, object);
            }
        } else {
            throw new DeserializeException(lineNumber, name + " is not a known entity");
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void readList(String val, EObject object, EStructuralFeature structuralFeature) throws DeserializeException, MetaDataException {
        int index = 0;
        if (!structuralFeature.isMany()) {
            throw new DeserializeException(lineNumber, "Field " + structuralFeature.getName() + " of " + structuralFeature.getEContainingClass().getName() + " is no aggregation");
        }
        AbstractEList list = (AbstractEList) object.eGet(structuralFeature);
        AbstractEList doubleStringList = null;
        if (structuralFeature.getEType() == EcorePackage.eINSTANCE.getEDouble()) {
            EStructuralFeature doubleStringFeature = structuralFeature.getEContainingClass().getEStructuralFeature(structuralFeature.getName() + "AsString");
            if (doubleStringFeature == null) {
                throw new DeserializeException(lineNumber, "Field not found: " + structuralFeature.getName() + "AsString");
            }
            doubleStringList = (AbstractEList) object.eGet(doubleStringFeature);
        }
        String realData = val.substring(1, val.length() - 1);
        int lastIndex = 0;
        while (lastIndex != realData.length() + 1) {
            int nextIndex = StringUtils.nextString(realData, lastIndex);
            String stringValue = realData.substring(lastIndex, nextIndex - 1).trim();
            lastIndex = nextIndex;
            if (stringValue.length() > 0) {
                if (stringValue.charAt(0) == '#') {
                    Integer referenceId = Integer.parseInt(stringValue.substring(1));
                    if (model.contains(referenceId)) {
                        EObject referencedObject = model.get(referenceId);
                        if (referencedObject != null) {
                            EClass referenceEClass = referencedObject.eClass();
                            if (((EClass) structuralFeature.getEType()).isSuperTypeOf(referenceEClass)) {
                                while (list.size() <= index) {
                                    list.addUnique(referencedObject);
                                }
                                list.setUnique(index, referencedObject);
                            } else {
                                throw new DeserializeException(lineNumber, referenceEClass.getName() + " cannot be stored in " + structuralFeature.getName());
                            }
                        }
                    } else {
                        waitingList.add(referenceId, new ListWaitingObject(lineNumber, object, (EReference) structuralFeature, index));
                    }
                } else if (stringValue.charAt(0) == '(') {
                    // Two dimensional list
                    IdEObject newObject = getPackageMetaData().create((EClass) structuralFeature.getEType());
                    readList(stringValue, newObject, newObject.eClass().getEStructuralFeature("List"));
                    list.addUnique(newObject);
                } else {
                    Object convert = convert(structuralFeature, structuralFeature.getEType(), stringValue);
                    if (convert != null) {
                        while (list.size() <= index) {
                            if (doubleStringList != null) {
                                doubleStringList.addUnique(stringValue);
                            }
                            list.addUnique(convert);
                        }
                        if (doubleStringList != null) {
                            doubleStringList.setUnique(index, stringValue);
                        }
                        list.setUnique(index, convert);
                    }
                }
            }
            index++;
        }
    }

    private Object convert(EStructuralFeature eStructuralFeature, EClassifier classifier, String value) throws DeserializeException, MetaDataException {
        if (classifier != null) {
            if (classifier instanceof EClassImpl) {
                if (null != ((EClassImpl) classifier).getEStructuralFeature(WRAPPED_VALUE)) {
                    IdEObject newObject = getPackageMetaData().create((EClass) classifier);
                    Class<?> instanceClass = newObject.eClass().getEStructuralFeature(WRAPPED_VALUE).getEType().getInstanceClass();
                    if (value.equals("")) {

                    } else {
                        if (instanceClass == Integer.class || instanceClass == int.class) {
                            try {
                                newObject.eSet(newObject.eClass().getEStructuralFeature(WRAPPED_VALUE), Integer.parseInt(value));
                            } catch (NumberFormatException e) {
                                throw new DeserializeException(lineNumber, value + " is not a valid integer(32) value");
                            }
                        } else if (instanceClass == Long.class || instanceClass == long.class) {
                            try {
                                newObject.eSet(newObject.eClass().getEStructuralFeature(WRAPPED_VALUE), Long.parseLong(value));
                            } catch (NumberFormatException e) {
                                throw new DeserializeException(lineNumber, value + " is not a valid integer(64) value");
                            }
                        } else if (instanceClass == Boolean.class || instanceClass == boolean.class) {
                            newObject.eSet(newObject.eClass().getEStructuralFeature(WRAPPED_VALUE), value.equals(".T."));
                        } else if (instanceClass == Double.class || instanceClass == double.class) {
                            try {
                                newObject.eSet(newObject.eClass().getEStructuralFeature(WRAPPED_VALUE), Double.parseDouble(value));
                            } catch (NumberFormatException e) {
                                throw new DeserializeException(lineNumber, value + " is not a valid floating point(32) number");
                            }
                            newObject.eSet(newObject.eClass().getEStructuralFeature(WRAPPED_VALUE + "AsString"), value);
                        } else if (instanceClass == String.class) {
                            newObject.eSet(newObject.eClass().getEStructuralFeature(WRAPPED_VALUE), IfcParserWriterUtils.readString(value, lineNumber));
                        } else if (instanceClass.getSimpleName().equals("Tristate")) {
                            Object tristate = null;
                            switch (value) {
                                case ".T.":
                                    tristate = getPackageMetaData().getEEnumLiteral("Tristate", "TRUE").getInstance();
                                    break;
                                case ".F.":
                                    tristate = getPackageMetaData().getEEnumLiteral("Tristate", "FALSE").getInstance();
                                    break;
                                case ".U.":
                                    tristate = getPackageMetaData().getEEnumLiteral("Tristate", "UNDEFINED").getInstance();
                                    break;
                            }
                            newObject.eSet(newObject.eClass().getEStructuralFeature(WRAPPED_VALUE), tristate);
                        } else {
                            throw new DeserializeException(lineNumber, instanceClass.getSimpleName() + " not implemented");
                        }
                    }
                    return newObject;
                } else {
                    return processInline(eStructuralFeature, classifier, value);
                }
            } else if (classifier instanceof EDataType) {
                return IfcParserWriterUtils.convertSimpleValue(getPackageMetaData(), eStructuralFeature, classifier.getInstanceClass(), value, lineNumber);
            }
        }
        return null;
    }

    private Object processInline(EStructuralFeature eStructuralFeature, EClassifier classifier, String value) throws DeserializeException, MetaDataException {
        if (value.contains("(")) {
            String typeName = value.substring(0, value.indexOf("(")).trim();
            String v = value.substring(value.indexOf("(") + 1, value.length() - 1);
            EClassifier eClassifier = getPackageMetaData().getEClassifierCaseInsensitive(typeName);
            if (eClassifier instanceof EClass) {
                Object convert = convert(eStructuralFeature, eClassifier, v);
                try {
                    model.add(-1, (IdEObject) convert);
                } catch (IfcModelInterfaceException e) {
                    throw new DeserializeException(lineNumber, e);
                }
                return convert;
            } else {
                throw new DeserializeException(lineNumber, typeName + " is not an existing IFC entity");
            }
        } else {
            return IfcParserWriterUtils.convertSimpleValue(getPackageMetaData(), eStructuralFeature, classifier.getInstanceClass(), value, lineNumber);
        }
    }

    private void readEnum(String val, EObject object, EStructuralFeature structuralFeature) throws DeserializeException {
        switch (val) {
            case ".T.":
                if (structuralFeature.getEType().getName().equals("Tristate")) {
                    object.eSet(structuralFeature, getPackageMetaData().getEEnumLiteral("Tristate", "TRUE").getInstance());
                } else if (structuralFeature.getEType().getName().equals("IfcBoolean")) {
                    EClass eClass = getPackageMetaData().getEClass("IfcBoolean");
                    EObject createIfcBoolean = getPackageMetaData().create(eClass);
                    createIfcBoolean.eSet(eClass.getEStructuralFeature("WrappedValue"), getPackageMetaData().getEEnumLiteral("Tristate", "TRUE").getInstance());
                    object.eSet(structuralFeature, createIfcBoolean);
                } else if (structuralFeature.getEType() == EcorePackage.eINSTANCE.getEBoolean()) {
                    object.eSet(structuralFeature, true);
                } else {
                    EClass eClass = getPackageMetaData().getEClass("IfcLogical");
                    EObject createIfcBoolean = getPackageMetaData().create(eClass);
                    createIfcBoolean.eSet(eClass.getEStructuralFeature("WrappedValue"), getPackageMetaData().getEEnumLiteral("Tristate", "TRUE").getInstance());
                    object.eSet(structuralFeature, createIfcBoolean);
                }
                break;
            case ".F.":
                if (structuralFeature.getEType().getName().equals("Tristate")) {
                    object.eSet(structuralFeature, getPackageMetaData().getEEnumLiteral("Tristate", "FALSE").getInstance());
                } else if (structuralFeature.getEType().getName().equals("IfcBoolean")) {
                    EClass eClass = getPackageMetaData().getEClass("IfcBoolean");
                    EObject createIfcBoolean = getPackageMetaData().create(eClass);
                    createIfcBoolean.eSet(eClass.getEStructuralFeature("WrappedValue"), getPackageMetaData().getEEnumLiteral("Tristate", "FALSE").getInstance());
                    object.eSet(structuralFeature, createIfcBoolean);
                } else if (structuralFeature.getEType() == EcorePackage.eINSTANCE.getEBoolean()) {
                    object.eSet(structuralFeature, false);
                } else {
                    EClass eClass = getPackageMetaData().getEClass("IfcLogical");
                    EObject createIfcBoolean = getPackageMetaData().create(eClass);
                    createIfcBoolean.eSet(eClass.getEStructuralFeature("WrappedValue"), getPackageMetaData().getEEnumLiteral("Tristate", "FALSE").getInstance());
                    object.eSet(structuralFeature, createIfcBoolean);
                }
                break;
            case ".U.":
                if (structuralFeature.getEType().getName().equals("Tristate")) {
                    object.eSet(structuralFeature, getPackageMetaData().getEEnumLiteral("Tristate", "UNDEFINED").getInstance());
                } else if (structuralFeature.getEType() == EcorePackage.eINSTANCE.getEBoolean()) {
                    object.eUnset(structuralFeature);
                } else {
                    EClass eClass = getPackageMetaData().getEClass("IfcLogical");
                    EObject createIfcBoolean = getPackageMetaData().create(eClass);
                    createIfcBoolean.eSet(eClass.getEStructuralFeature("WrappedValue"), getPackageMetaData().getEEnumLiteral("Tristate", "UNDEFINED").getInstance());
                    object.eSet(structuralFeature, createIfcBoolean);
                }
                break;
            default:
                if (structuralFeature.getEType() instanceof EEnumImpl) {
                    String realEnumValue = val.substring(1, val.length() - 1);
                    EEnumLiteral enumValue = (((EEnumImpl) structuralFeature.getEType()).getEEnumLiteral(realEnumValue));
                    if (enumValue == null) {
                        // Another IFC4/add1/add2 hack
                        if (structuralFeature.getEType() == Ifc4Package.eINSTANCE.getIfcExternalSpatialElementTypeEnum() && realEnumValue.equals("NOTDEFIEND")) {
                            realEnumValue = "NOTDEFINED";
                            enumValue = (((EEnumImpl) structuralFeature.getEType()).getEEnumLiteral(realEnumValue));
                        }
                    }
                    if (enumValue == null) {
                        throw new DeserializeException(lineNumber, "Enum type " + structuralFeature.getEType().getName() + " has no literal value '" + realEnumValue + "'");
                    }
                    object.eSet(structuralFeature, enumValue.getInstance());
                } else {
                    throw new DeserializeException(lineNumber, "Value " + val + " indicates enum type but " + structuralFeature.getEType().getName() + " expected");
                }
                break;
        }
    }

    private void readReference(String val, EObject object, EStructuralFeature structuralFeature) throws DeserializeException {
        if (structuralFeature == Ifc4Package.eINSTANCE.getIfcIndexedColourMap_Opacity()) {
            // HACK for IFC4/add1/add2
            object.eSet(structuralFeature, 0D);
            object.eSet(structuralFeature.getEContainingClass().getEStructuralFeature(structuralFeature.getName() + "AsString"), "0");
            return;
        }

        int referenceId;
        try {
            referenceId = Integer.parseInt(val.substring(1));
        } catch (NumberFormatException e) {
            throw new DeserializeException(lineNumber, "'" + val + "' is not a valid reference");
        }
        if (model.contains(referenceId)) {
            object.eSet(structuralFeature, model.get(referenceId));
        } else {
            waitingList.add(referenceId, new SingleWaitingObject(lineNumber, object, (EReference) structuralFeature));
        }
    }
}
