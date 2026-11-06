package ru.inversion.fx.help.store;

import java.io.File;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author perov
 */
public interface IStoreHelp {

    public void saveHelpToFile( File file, List<AdapterHelp> controllers );

    public List<AdapterHelp> loadHelpFromFile(File file, StringBuilder errorProtocol);

    //public void save(Path fileTo, Format format, Iterator<> );
}
