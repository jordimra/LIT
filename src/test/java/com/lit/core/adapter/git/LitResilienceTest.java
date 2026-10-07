package com.lit.core.adapter.git;

import com.lit.api.LitService;
import com.lit.api.WorkspaceStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

public class LitResilienceTest {

    private Path tempDir;
    private LitService litService;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("lit_resilience_test");
        litService = new GitAdapterLitService();
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(java.io.File::delete);
    }

    @Test
    void testResilienceToExternalGitCheckout() throws Exception {
        litService.initialize(tempDir);
        
        // LIT creates a task called 'main' by default
        assertEquals("main", litService.getActiveTask());
        
        // 1. User executes an "illegal" native git command
        executeNativeGit("checkout", "-b", "secret-branch");
        
        // 2. LIT should elegantly reflect the new state without crashing
        assertEquals("secret-branch", litService.getActiveTask());
        
        // 3. User can still save through LIT
        Files.writeString(tempDir.resolve("hello.txt"), "world");
        litService.save("LIT save on secret branch");
        
        WorkspaceStatus status = litService.getStatus();
        assertFalse(status.hasChanges());
    }
    
    @Test
    void testResilienceToExternalGitReset() throws Exception {
        litService.initialize(tempDir);
        
        Files.writeString(tempDir.resolve("a.txt"), "A");
        litService.save("First save");
        
        Files.writeString(tempDir.resolve("b.txt"), "B");
        litService.save("Second save");
        
        // User illegally resets using Git
        executeNativeGit("reset", "--hard", "HEAD~1");
        
        // LIT status should survive and show clean workspace
        WorkspaceStatus status = litService.getStatus();
        assertFalse(status.hasChanges());
        
        // The file B should be gone
        assertFalse(Files.exists(tempDir.resolve("b.txt")));
    }

    private void executeNativeGit(String... args) throws Exception {
        java.util.List<String> command = new java.util.ArrayList<>();
        command.add("git");
        command.addAll(java.util.Arrays.asList(args));
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(tempDir.toFile());
        Process process = pb.start();
        int exitCode = process.waitFor();
        assertEquals(0, exitCode, "Native git command failed");
    }
}
