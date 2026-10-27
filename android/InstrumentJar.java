import java.io.*;
import java.nio.file.*;
import java.util.zip.*;
import org.microemu.android.asm.AndroidProducer;

/** Applies the same Java ME compatibility transformations as J2ME Loader's importer. */
public final class InstrumentJar {
    public static void main(String[] args) throws Exception {
        if (args.length != 2 || args[0].equals(args[1])) throw new IllegalArgumentException("input output required");
        int classes = 0;
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(Path.of(args[0])));
             ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(Path.of(args[1])))) {
            for (ZipEntry entry; (entry = in.getNextEntry()) != null;) {
                if (entry.isDirectory()) continue;
                byte[] data = in.readAllBytes();
                if (entry.getName().endsWith(".class")) {
                    data = AndroidProducer.instrument(data, entry.getName());
                    classes++;
                }
                out.putNextEntry(new ZipEntry(entry.getName()));
                out.write(data); out.closeEntry();
            }
        }
        if (classes == 0) throw new IllegalStateException("No game classes found");
        System.out.println("Instrumented Java ME classes: " + classes);
    }
}
