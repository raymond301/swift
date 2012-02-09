package edu.mayo.mprc.database;

import java.io.File;

/**
 * A dummy file token translator that does no translation at all - it stores the full file path directly.
 *
 * @author Roman Zenka
 */
public class DummyFileTokenTranslator implements FileTokenToDatabaseTranslator {
    @Override
    public String fileToDatabaseToken(File file) {
        return file.getAbsolutePath();
    }

    @Override
    public File databaseTokenToFile(String tokenPath) {
        return new File(tokenPath);
    }
}