package psdutil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ApplicationCore {

    public static void main(String[] args) throws IOException {
        if(args.length == 0)
            convertPath(Path.of(Settings.INPUT_FOLDER));
        else for (int i = 0; i < args.length; i++)
            convertPath(Path.of(args[i]));
    }

    public static void convertPath(final Path p) throws IOException {
        Files.find(p, Settings.MAX_FOLDER_SEARCH, (path, atts)-> path.toString().endsWith(".psd")).forEach((path -> {
            try {
                System.out.println("Converting file: " + path.toString());
                final String[] inputSplit = path.toString().replace(Settings.INPUT_FOLDER, "").split("\\\\");
                final String[] copy = new String[inputSplit.length-1];
                System.arraycopy(inputSplit,0,copy, 0,inputSplit.length-1);
                String output = String.join("\\", copy);
                ConverterCore.main(new String[]{path.toString(), Settings.OUTPUT_FOLDER+output});
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException();
            }
        }));
    }
}
