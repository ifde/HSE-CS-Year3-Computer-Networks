package mydrive.protocol;

import java.util.ArrayList;
import java.util.List;

public class FileResponseMessage implements Message {
    private List<String> missingFiles = new ArrayList<>();

    public void addMissingFile(String fileName) {
        missingFiles.add(fileName);
    }

    public List<String> getMissingFiles() {
        return missingFiles;
    }
}
