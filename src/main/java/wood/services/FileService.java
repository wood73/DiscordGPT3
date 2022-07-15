package wood.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

public class FileService {

    public static final String KEY_FILE = "key.txt";

    /**
     * Loads the key from FileService.KEY_FILE
     * @return Optional.empty() if the file doesn't exist (or is empty),
     *      otherwise it'll return FileService.KEY_FILE's contents
     */
    public static Optional<String> loadKey() {
        File keyFile = new File(KEY_FILE);

        if(keyFile.exists()) {
            try {
                List<String> lines = Files.readAllLines(keyFile.toPath());

                if (lines.size() == 0) //will be 0 if the file is empty
                    return Optional.empty();
                else
                    return Optional.of(lines.get(0).trim());

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return Optional.empty(); //failed to read key
    }



}
