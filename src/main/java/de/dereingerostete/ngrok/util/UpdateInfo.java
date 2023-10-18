/*
 * Copyright (c) 2023 - DerEingerostete
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package de.dereingerostete.ngrok.util;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;

@Data
public class UpdateInfo {
    private final @NotNull Future<Process> completionFuture; // Completes if the update processes finishes
    private final @NotNull Future<Boolean> updatingFuture;   // Completes if the thread detected the state of the update

}
