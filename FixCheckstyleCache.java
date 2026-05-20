import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class FixCheckstyle {
    public static void main(String[] args) throws IOException {
        Path startPath = Paths.get("d:/Dev/project/xyz/zerx/zerx-spring/zerx-spring-cache/zerx-spring-cache-impl/src/main/java");
        try (Stream<Path> stream = Files.walk(startPath)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> p.toString().endsWith(".java"))
                  .forEach(FixCheckstyle::fixFile);
        }
    }

    private static void fixFile(Path path) {
        try {
            List<String> lines = Files.readAllLines(path);
            List<String> newLines = new ArrayList<>();
            List<String> imports = new ArrayList<>();
            boolean inImports = false;
            
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                
                // Fix log constant
                if (line.contains("private static final Logger log =")) {
                    line = line.replace(" Logger log ", " Logger LOG ");
                }
                if (line.contains("log.info") || line.contains("log.debug") || line.contains("log.warn") || line.contains("log.error")) {
                    line = line.replace("log.", "LOG.");
                }

                // Fix one-line getters/setters (LeftCurly, RightCurlyAlone)
                if (line.matches("\\s*public\\s+\\w+(<.*>)?\\s+get\\w+\\(\\)\\s*\\{\\s*return\\s+.*;\\s*\\}")) {
                    int firstBrace = line.indexOf('{');
                    int lastBrace = line.lastIndexOf('}');
                    newLines.add(line.substring(0, firstBrace).trim() + " {");
                    newLines.add("        " + line.substring(firstBrace + 1, lastBrace).trim());
                    newLines.add("    }");
                    continue;
                }
                if (line.matches("\\s*public\\s+void\\s+set\\w+\\(.*\\)\\s*\\{\\s*this\\..*\\s*\\}")) {
                    int firstBrace = line.indexOf('{');
                    int lastBrace = line.lastIndexOf('}');
                    newLines.add(line.substring(0, firstBrace).trim() + " {");
                    newLines.add("        " + line.substring(firstBrace + 1, lastBrace).trim());
                    newLines.add("    }");
                    continue;
                }
                
                // Collect imports
                if (line.startsWith("import ")) {
                    if (!inImports) {
                        inImports = true;
                    }
                    if (line.contains("*")) {
                        // We shouldn't use star imports, but let's just leave them if they exist and only sort
                    }
                    imports.add(line);
                    continue;
                } else if (inImports && line.trim().isEmpty()) {
                    continue; // skip empty lines in import section
                } else if (inImports && !line.startsWith("import ")) {
                    // end of imports, sort and output
                    inImports = false;
                    sortAndOutputImports(imports, newLines);
                    newLines.add(""); // blank line after imports
                    if (!line.trim().isEmpty()) {
                        newLines.add(line);
                    }
                    continue;
                }
                
                newLines.add(line);
            }
            
            // Fix EOF newline
            if (!newLines.isEmpty() && newLines.get(newLines.size() - 1).trim().isEmpty()) {
                newLines.remove(newLines.size() - 1);
            }
            
            Files.write(path, newLines);
        } catch (Exception e) {
            System.err.println("Error processing " + path + ": " + e.getMessage());
        }
    }

    private static void sortAndOutputImports(List<String> imports, List<String> newLines) {
        List<String> javaImports = new ArrayList<>();
        List<String> javaxImports = new ArrayList<>();
        List<String> orgImports = new ArrayList<>();
        List<String> comZerxImports = new ArrayList<>();

        for (String imp : imports) {
            if (imp.startsWith("import java.")) {
                javaImports.add(imp);
            } else if (imp.startsWith("import javax.") || imp.startsWith("import jakarta.")) {
                javaxImports.add(imp);
            } else if (imp.startsWith("import com.zerx.")) {
                comZerxImports.add(imp);
            } else {
                orgImports.add(imp);
            }
        }

        Collections.sort(javaImports);
        Collections.sort(javaxImports);
        Collections.sort(orgImports);
        Collections.sort(comZerxImports);

        if (!javaImports.isEmpty()) {
            newLines.addAll(javaImports);
            newLines.add("");
        }
        if (!javaxImports.isEmpty()) {
            newLines.addAll(javaxImports);
            newLines.add("");
        }
        if (!orgImports.isEmpty()) {
            newLines.addAll(orgImports);
            newLines.add("");
        }
        if (!comZerxImports.isEmpty()) {
            newLines.addAll(comZerxImports);
        } else if (!newLines.isEmpty() && newLines.get(newLines.size() - 1).isEmpty()) {
            newLines.remove(newLines.size() - 1);
        }
    }
}
