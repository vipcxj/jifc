package me.cxj.ifc.deserializer;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.Map;

/**
 * Created by vipcxj on 2018/11/20.
 */
public class XmlUtils {

    public static XMLEvent stepUntil(XMLEventReader reader, String nodeName, Map<String, String> attributes) throws XMLStreamException {
        boolean waitEnd = false;
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (waitEnd) {
                if (event.isEndElement()) {
                    waitEnd = false;
                }
            } else {
                if (event.isStartElement()) {
                    StartElement startElement = event.asStartElement();
                    String name = startElement.getName().getLocalPart();
                    if (nodeName == null || nodeName.equalsIgnoreCase(name)) {
                        if (attributes == null) {
                            return event;
                        }
                        boolean found = true;
                        for (Map.Entry<String, String> entry : attributes.entrySet()) {
                            Attribute attribute = startElement.getAttributeByName(QName.valueOf(entry.getKey()));
                            if (attribute == null || !attribute.getValue().equalsIgnoreCase(entry.getValue())) {
                                found = false;
                                break;
                            }
                        }
                        if (found) {
                            return event;
                        }
                    }
                    waitEnd = true;
                }
            }
        }
        return null;
    }
}
