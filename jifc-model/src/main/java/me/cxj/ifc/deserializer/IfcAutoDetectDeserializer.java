package me.cxj.ifc.deserializer;

import me.cxj.ifc.model.IfcModel;
import me.cxj.ifc.model.PackageMetaDataSet;
import me.cxj.ifc.utils.IOUtils;
import org.bimserver.emf.PackageMetaData;
import org.bimserver.emf.Schema;
import org.bimserver.plugins.deserializers.ByteProgressReporter;
import org.bimserver.plugins.deserializers.DeserializeException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Created by vipcxj on 2018/11/20.
 */
public class IfcAutoDetectDeserializer implements IfcDeserializer {

    @Override
    public IfcModel read(InputStream inputStream, ByteProgressReporter reporter) throws DeserializeException {
        BufferedInputStream bis = new BufferedInputStream(inputStream);
        bis.mark(4096);
        byte[] buffer = new byte[4096];
        try {
            int read = IOUtils.tryReadFully(bis, buffer);
            bis.reset();
            String str = new String(buffer, 0, read, StandardCharsets.UTF_8).trim();
            Schema schema;
            boolean xml;
            IfcDeserializer deserializer;
            if (str.startsWith("<")) {
                schema = checkXml(str);
                xml = true;
            } else {
                schema = checkStep(str);
                xml = false;
            }
            if (schema == null) {
                throw new DeserializeException("Unable to auto detect the schema version of the ifc file.");
            }
            deserializer = xml ? getXmlDeserializer(schema) : getStepDeserializer(schema);
            if (deserializer == null) {
                throw new DeserializeException("Unsupported ifc schema version: " + schema.getHeaderName() + ".");
            }
            return deserializer.read(bis, reporter);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static IfcDeserializer getXmlDeserializer(Schema schema) {
        PackageMetaData metaData;
        switch (schema) {
            case IFC2X3TC1:
                metaData = PackageMetaDataSet.IFC2x3TC1.getMetaData();
                return new IfcXmlDeserializer(metaData);
            case IFC4:
                metaData = PackageMetaDataSet.IFC4.getMetaData();
                return new IfcXmlDeserializer(metaData);
        }
        return null;
    }

    private static IfcDeserializer getStepDeserializer(Schema schema) {
        PackageMetaData metaData;
        switch (schema) {
            case IFC2X3TC1:
                metaData = PackageMetaDataSet.IFC2x3TC1.getMetaData();
                return new IfcStepDeserializer(metaData);
            case IFC4:
                metaData = PackageMetaDataSet.IFC4.getMetaData();
                return new IfcStepDeserializer(metaData);
        }
        return null;
    }

    private static Schema checkXml(String content) {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        try {
            Schema schema = checkISO_10303_28(inputFactory, content);
            if (schema != null) {
                return schema;
            }
            schema = checkIFC4(inputFactory, content);
            if (schema != null) {
                return schema;
            }
            return null;
        } catch (XMLStreamException e) {
            return null;
        }
    }

    private static Schema checkISO_10303_28(XMLInputFactory inputFactory, String content) throws XMLStreamException {
        XMLEventReader reader = inputFactory.createXMLEventReader(new StringReader(content));
        XMLEvent event = XmlUtils.stepUntil(reader, "iso_10303_28", null);
        if (event != null) {
            return Schema.IFC2X3TC1;
        } else {
            return null;
        }
    }

    private static Schema checkIFC4(XMLInputFactory inputFactory, String content) throws XMLStreamException {
        XMLEventReader reader = inputFactory.createXMLEventReader(new StringReader(content));
        XMLEvent event = XmlUtils.stepUntil(reader, "ifcXML", null);
        if (event != null) {
            return Schema.IFC4;
        } else {
            return null;
        }
    }

    private static Schema checkStep(String content) throws DeserializeException, IOException {
        BufferedReader reader = new BufferedReader(new StringReader(content));
        String line = getFullLine(reader);
        while (line != null) {
            if (line.startsWith("FILE_SCHEMA")) {
                String fileSchema = line.substring("FILE_SCHEMA".length()).trim();
                line = fileSchema.substring(1, fileSchema.length() - 2).replace("\r\n", "");
                StepParser stepParser = new StepParser(line);
                String ifcSchemaVersion = stepParser.readNextString();
                if (Schema.IFC2X3TC1.getHeaderName().equalsIgnoreCase(ifcSchemaVersion)) {
                    return Schema.IFC2X3TC1;
                } else if (Schema.IFC4.getHeaderName().equalsIgnoreCase(ifcSchemaVersion)) {
                    return Schema.IFC4;
                } else {
                    throw new DeserializeException("Unsupported ifc schema version: " + ifcSchemaVersion + ".");
                }
            }
            line = getFullLine(reader);
        }
        return null;
    }

    private static String getFullLine(BufferedReader reader) throws IOException {
        CodePointStream cps = new CodePointStream(reader);
        StringBuilder sb = new StringBuilder();
        int cp = cps.read();
        if (!Character.isWhitespace(cp)) {
            sb.appendCodePoint(cp);
        }
        // 0: normal
        // 1: in string
        // 2: in binary
        int mode = 0;
        while (cp != -1) {
            switch (mode) {
                case 0: {
                    if (cp == '\'') {
                        mode = 1;
                    } else if (cp == '"') {
                        mode = 2;
                    } else if (cp == ';') {
                        return sb.toString();
                    }
                    break;
                }
                case 1: {
                    if (cp == '\\') {
                        cp = cps.read();
                        sb.appendCodePoint(cp);
                    } else if (cp == '\'') {
                        mode = 0;
                    }
                    break;
                }
                case 2: {
                    if (cp == '"') {
                        mode = 0;
                    }
                }
            }
            cp = cps.read();
            sb.appendCodePoint(cp);
        }
        return null;
    }
}
