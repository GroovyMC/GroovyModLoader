/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package com.matyrobbrt.gml.bus.type

/**
 * Indicates a type of event bus.
 * @see ForgeBus
 * @see ModBus
 */
sealed interface BusType permits ForgeBus, ModBus {
}
