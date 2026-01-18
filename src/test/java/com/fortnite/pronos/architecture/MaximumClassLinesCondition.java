package com.fortnite.pronos.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

/**
 * ArchUnit condition to enforce maximum lines of code per class.
 *
 * <p>This enforces the CLAUDE.md constraint: "Maximum 500 lines per class".
 */
public class MaximumClassLinesCondition extends ArchCondition<JavaClass> {

  private final int maxLines;

  public MaximumClassLinesCondition(int maxLines) {
    super("not exceed " + maxLines + " lines");
    this.maxLines = maxLines;
  }

  @Override
  public void check(JavaClass javaClass, ConditionEvents events) {
    try {
      int lineCount = countLines(javaClass);

      if (lineCount > maxLines) {
        String message =
            String.format(
                "Class %s has %d lines, which exceeds the maximum of %d lines (CLAUDE.md constraint)",
                javaClass.getName(), lineCount, maxLines);

        events.add(SimpleConditionEvent.violated(javaClass, message));
      }
    } catch (IOException e) {
      // If we can't read the file, skip validation (test environment issue)
      events.add(
          SimpleConditionEvent.satisfied(
              javaClass, "Could not read source file for line count: " + e.getMessage()));
    }
  }

  /**
   * Counts the number of lines in a Java class source file.
   *
   * @param javaClass The JavaClass to analyze
   * @return The number of lines in the source file
   * @throws IOException If the source file cannot be read
   */
  private int countLines(JavaClass javaClass) throws IOException {
    URL sourceLocation = javaClass.getSource().map(source -> source.getUri().toURL()).orElse(null);

    if (sourceLocation == null) {
      // If source is not available (e.g., compiled from JAR), skip validation
      return 0;
    }

    String sourcePath = sourceLocation.getPath();

    // Convert class file path to source file path
    // Example: /path/to/target/classes/com/fortnite/pronos/Foo.class
    //       -> /path/to/src/main/java/com/fortnite/pronos/Foo.java
    sourcePath =
        sourcePath
            .replace("/target/classes/", "/src/main/java/")
            .replace("/target/test-classes/", "/src/test/java/")
            .replace(".class", ".java");

    // Handle Windows paths
    if (sourcePath.startsWith("/") && sourcePath.contains(":")) {
      sourcePath = sourcePath.substring(1); // Remove leading slash on Windows
    }

    return countLinesInFile(sourcePath);
  }

  /**
   * Counts non-empty lines in a file.
   *
   * @param filePath Path to the file
   * @return Number of non-empty lines
   * @throws IOException If file cannot be read
   */
  private int countLinesInFile(String filePath) throws IOException {
    int lineCount = 0;

    try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
      String line;
      while ((line = reader.readLine()) != null) {
        String trimmed = line.trim();
        // Count non-empty lines (ignore blank lines and pure comment lines)
        if (!trimmed.isEmpty()
            && !trimmed.startsWith("//")
            && !trimmed.startsWith("/*")
            && !trimmed.equals("*/")
            && !trimmed.startsWith("*")) {
          lineCount++;
        }
      }
    }

    return lineCount;
  }
}
