package org.dcm4chee.arc.conf.json;

import org.dcm4che3.conf.json.JsonReader;
import org.dcm4che3.conf.json.JsonWriter;
import org.dcm4che3.conf.json.hl7.JsonHL7ConfigurationExtension;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.hl7.HL7Application;
import org.dcm4chee.arc.conf.ArchiveHL7ApplicationExtension;

import javax.json.stream.JsonParser;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @since Jan 2016
 */
public class JsonArchivHL7Configuration implements JsonHL7ConfigurationExtension {
    @Override
    public void storeTo(HL7Application hl7App, Device device, JsonWriter writer) {
        ArchiveHL7ApplicationExtension ext = hl7App.getHL7ApplicationExtension(ArchiveHL7ApplicationExtension.class);
        if (ext == null)
            return;

        writer.writeStartObject("dcmArchiveHL7Application");
        writer.writeNotNull("hl7PatientUpdateTemplateURI", ext.getPatientUpdateTemplateURI());
        writer.writeEnd();
    }

    @Override
    public boolean loadHL7ApplicationExtension(Device device, HL7Application hl7App, JsonReader reader) {
        if (!reader.getString().equals("dcmArchiveHL7Application"))
            return false;

        reader.next();
        reader.expect(JsonParser.Event.START_OBJECT);
        ArchiveHL7ApplicationExtension ext = new ArchiveHL7ApplicationExtension();
        loadFrom(ext, reader);
        hl7App.addHL7ApplicationExtension(ext);
        reader.expect(JsonParser.Event.END_OBJECT);
        return true;
    }

    private void loadFrom(ArchiveHL7ApplicationExtension ext, JsonReader reader) {
        while (reader.next() == JsonParser.Event.KEY_NAME) {
            switch (reader.getString()) {
                case "hl7PatientUpdateTemplateURI":
                    ext.setPatientUpdateTemplateURI(reader.stringValue());
                default:
                    reader.skipUnknownProperty();
            }
        }
    }
}
