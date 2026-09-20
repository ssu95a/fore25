/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.help.store;

import ru.inversion.fx.app.es.JInvErrorService;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author perov
 */
public class StoreHelpImpl implements IStoreHelp {

    @Override
    public void saveHelpToFile(File file, List<AdapterHelp> controllers) {
        try {
            JAXBContext context = JAXBContext.newInstance(PHelpWrapper.class);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            // Wrapping our person data.
            PHelpWrapper wrapper = new PHelpWrapper();
            wrapper.setControllers(controllers);

            // Marshalling and saving XML to the file.
            m.marshal(wrapper, file);

        } catch (JAXBException ex) {
            JInvErrorService.handleException(null, ex);
        }
    }

    @Override
    public List<AdapterHelp> loadHelpFromFile(File file, StringBuilder errorProtocol) {
        List<AdapterHelp> list = new LinkedList<>();
        try {
            JAXBContext context = JAXBContext.newInstance(PHelpWrapper.class);
            Unmarshaller um = context.createUnmarshaller();

            // Reading XML from the file and unmarshalling.
            PHelpWrapper wrapper = (PHelpWrapper) um.unmarshal(file);

            list.addAll(wrapper.getControllers());

            // Save the file path to the registry.
        } catch (Exception e) { 
            try {
                errorProtocol.append("Неверный формат файла помощи \n");
                errorProtocol.append(Files.readAllLines(file.toPath()));
            } catch (IOException ex) {
                JInvErrorService.handleException(null, ex);
            }
        }
        return list;
    }

}
