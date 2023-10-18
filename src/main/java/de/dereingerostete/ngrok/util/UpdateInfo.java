package de.dereingerostete.ngrok.util;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;

@Data
public class UpdateInfo {
    private final @NotNull Future<Process> completionFuture; // Completes if the update processes finishes
    private final @NotNull Future<Boolean> updatingFuture;   // Completes if the thread detected the state of the update

}
