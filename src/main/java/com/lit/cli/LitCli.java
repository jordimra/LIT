package com.lit.cli;

import com.lit.api.*;
import com.lit.api.exceptions.*;
import com.lit.core.adapter.git.GitAdapterLitService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class LitCli {
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";
    private static final String BLUE = "\u001B[34m";
    
    private static void printSuccess(String message) {
        System.out.println(GREEN + "✔ " + message + RESET);
    }
    
    private static void printInfo(String message) {
        System.out.println(BLUE + "✦ " + message + RESET);
    }
    
    private static void printWarning(String message) {
        System.out.println(YELLOW + "⚠ " + message + RESET);
    }
    
    private static void printError(String message) {
        System.out.println(RED + "✖ " + message + RESET);
    }

    public static void main(String[] args) {
        if (args.length == 0 || args[0].equals("-h") || args[0].equals("--help") || args[0].equals("help")) {
            printHelp();
            return;
        }
        
        String command = args[0].toLowerCase();
        LitService service = new GitAdapterLitService();
        
        try {
            switch (command) {
                case "init":
                    service.initialize(Path.of("."));
                    printSuccess("¡Repositorio LIT creado con éxito en este directorio!");
                    printInfo("Tarea por defecto establecida en: 'main'");
                    printInfo("Se ha guardado el estado inicial (Snapshot inicial) de tus archivos.");
                    break;
                    
                case "status":
                    executeStatus(service);
                    break;
                    
                case "save":
                    if (args.length < 2 || args[1].trim().isEmpty()) {
                        printError("Error: Debes proporcionar un mensaje para guardar tu progreso.");
                        System.out.println("Sintaxis: lit save \"[mensaje]\"");
                        System.exit(1);
                    }
                    executeSave(service, args[1]);
                    break;
                    
                case "task":
                    if (args.length < 2) {
                        executeListTasks(service);
                    } else {
                        executeSwitchTask(service, args[1]);
                    }
                    break;
                    
                case "sync":
                    executeSync(service);
                    break;
                    
                case "undo":
                    executeUndo(service);
                    break;
                    
                default:
                    printError("Comando no reconocido: '" + args[0] + "'");
                    printHelp();
                    System.exit(1);
            }
        } catch (RepositoryNotFoundException e) {
            printError("No se encontró ningún repositorio LIT en esta ruta.");
            System.exit(1);
        } catch (ActiveConflictException e) {
            printError(e.getMessage());
            System.exit(1);
        } catch (NothingToSaveException e) {
            printError(e.getMessage());
            System.exit(1);
        } catch (LitException e) {
            printError("Error de LIT: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            printError("Error inesperado: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void executeStatus(LitService service) {
        WorkspaceStatus status = service.getStatus();
        if (!status.activeConflicts().isEmpty()) {
            printError("¡Conflicto de sincronización detectado!");
            printWarning("Se han encontrado colisiones de cambios en " + status.activeConflicts().size() + " archivo(s).");
            System.out.println();
            System.out.println("  Archivo(s) en conflicto:");
            for (ConflictRecord conflict : status.activeConflicts()) {
                System.out.println("    - " + conflict.relativePath());
            }
            System.out.println();
            printInfo("LIT ha pausado la sincronización para evitar sobrescribir código.");
            System.out.println("Por favor, abre los archivos en tu editor de código. Encontrarás colisiones como esta:");
            ConflictRecord first = status.activeConflicts().get(0);
            System.out.println("<<<<<<< Tu versión local");
            first.localLines().stream().limit(3).forEach(System.out::println);
            if (first.localLines().size() > 3) System.out.println("...");
            System.out.println("=======");
            first.incomingLines().stream().limit(3).forEach(System.out::println);
            if (first.incomingLines().size() > 3) System.out.println("...");
            System.out.println(">>>>>>> Cambios entrantes del servidor");
            System.out.println();
            System.out.println("1. Edita el archivo y decide qué versión conservar.");
            System.out.println("2. Una vez guardado el archivo en tu editor, ejecuta:");
            System.out.println("   lit save \"Resuelto conflicto\"");
            return;
        }
        
        if (!status.hasChanges()) {
            printSuccess("Todo al día. No hay cambios pendientes de guardar en la tarea '" + status.activeTaskName() + "'.");
            return;
        }
        
        printInfo("Estado actual en la tarea '" + status.activeTaskName() + "':");
        System.out.println();
        
        List<ChangedFile> modifiedOrDeleted = status.modifiedFiles().stream()
            .filter(f -> f.type() == ChangeType.MODIFIED || f.type() == ChangeType.DELETED)
            .collect(Collectors.toList());
            
        List<ChangedFile> added = status.modifiedFiles().stream()
            .filter(f -> f.type() == ChangeType.ADDED)
            .collect(Collectors.toList());
            
        if (!modifiedOrDeleted.isEmpty()) {
            System.out.println("Archivos modificados (esperando ser guardados):");
            for (ChangedFile f : modifiedOrDeleted) {
                String icon = f.type() == ChangeType.DELETED ? "✖" : "⚠";
                String color = f.type() == ChangeType.DELETED ? RED : YELLOW;
                System.out.println("    " + color + icon + "  " + f.relativePath() + RESET);
            }
            System.out.println();
        }
        
        if (!added.isEmpty()) {
            System.out.println("Archivos nuevos (se incluirán en el próximo guardado):");
            for (ChangedFile f : added) {
                System.out.println("    " + BLUE + "✦  " + f.relativePath() + RESET);
            }
            System.out.println();
        }
        
        System.out.println("(Usa 'lit save \"mensaje\"' para guardar estos cambios en tu historial)");
    }

    private static void executeSave(LitService service, String message) {
        printInfo("Guardando progreso...");
        SnapshotRecord record = service.save(message);
        printSuccess("¡Progreso guardado correctamente!");
        String shortId = record.id().substring(0, Math.min(7, record.id().length()));
        printInfo("Snapshot creado: #" + shortId + " (" + record.message() + ")");
    }

    private static void executeListTasks(LitService service) {
        List<String> tasks = service.listTasks();
        String active = service.getActiveTask();
        for (String task : tasks) {
            if (task.equals(active)) {
                System.out.println("  * " + GREEN + task + RESET);
            } else {
                System.out.println("    " + task);
            }
        }
    }

    private static void executeSwitchTask(LitService service, String taskName) {
        WorkspaceStatus status = service.getStatus();
        String currentTask = status.activeTaskName();
        if (currentTask.equals(taskName)) {
            printSuccess("Ya estás en la tarea '" + taskName + "'.");
            return;
        }
        
        if (status.hasChanges()) {
            printInfo("Guardando temporalmente tus cambios locales de la tarea '" + currentTask + "'...");
        }
        
        service.switchTask(taskName);
        printSuccess("Cambiado a la tarea '" + taskName + "'.");
    }

    private static void executeSync(LitService service) {
        printInfo("Conectando con el servidor remoto...");
        printInfo("Descargando capturas de estado del servidor...");
        printInfo("Enviando tus capturas de estado locales...");
        
        try {
            service.synchronize();
            printSuccess("Sincronización finalizada correctamente. Tu proyecto está al día.");
        } catch (ActiveConflictException e) {
            executeStatus(service);
            System.exit(1);
        }
    }

    private static void executeUndo(LitService service) {
        String lastSaveId = null;
        String lastSaveMsg = null;
        
        Path logPath = Path.of(".git").resolve("lit").resolve("audit_log.json");
        boolean isLastSave = false;
        if (Files.exists(logPath)) {
            try {
                String content = Files.readString(logPath).trim();
                List<String> entries;
                if (content.startsWith("[")) {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    entries = mapper.readValue(content, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
                } else {
                    entries = new java.util.ArrayList<>();
                    for (String line : Files.readAllLines(logPath)) {
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty()) {
                            entries.add(trimmed);
                        }
                    }
                }
                
                if (!entries.isEmpty()) {
                    String lastAction = entries.get(entries.size() - 1);
                    if (lastAction.startsWith("save:")) {
                        isLastSave = true;
                        lastSaveId = lastAction.substring(5);
                    }
                }
            } catch (Exception ignored) {}
        }
        
        if (isLastSave && lastSaveId != null) {
            try {
                Process p = new ProcessBuilder("git", "log", "-1", lastSaveId, "--format=%s").start();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    lastSaveMsg = r.readLine();
                }
                p.waitFor();
            } catch (Exception ignored) {}
            
            String displayMsg = lastSaveMsg != null ? lastSaveMsg : "último guardado";
            String displayId = lastSaveId.substring(0, Math.min(7, lastSaveId.length()));
            
            printWarning("¡Atención! Estás a punto de deshacer el último guardado (#" + displayId + ": \"" + displayMsg + "\").");
            printInfo("Los archivos modificados volverán a tu espacio de trabajo como cambios pendientes.");
            
            service.undo();
            printSuccess("Último guardado deshecho correctamente.");
        } else {
            service.undo();
            printSuccess("Se han restaurado los archivos al estado del último Snapshot.");
        }
    }

    private static void printHelp() {
        System.out.println("LIT - Light Intelligent Tracking (Gestión de Versiones Simplificada)");
        System.out.println();
        System.out.println("Uso: lit <comando> [argumentos]");
        System.out.println();
        System.out.println("Comandos disponibles:");
        System.out.println("  init               Inicializa un repositorio LIT en el directorio actual");
        System.out.println("  status             Muestra el estado del área de trabajo y conflictos");
        System.out.println("  save \"<mensaje>\"   Guarda el progreso de tus archivos en un nuevo Snapshot");
        System.out.println("  task               Lista todas las tareas locales");
        System.out.println("  task <nombre>      Cambia de tarea (creándola si no existe) con stashing automático");
        System.out.println("  sync               Sincroniza tus cambios con el servidor remoto");
        System.out.println("  undo               Deshace la última acción (save, switch, sync)");
        System.out.println();
        System.out.println("Para más información, visita docs/README.md");
    }
}
