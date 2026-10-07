/**
 * ForgeFarms — automated player-owned farms for Paper 26.3.
 *
 * <p>Original implementation. Place a farm item, it grows and harvests crops,
 * trees, or any configured blocks inside its radius into internal storage.
 * Trust/roles, hopper output, and holograms are built in — no paid
 * dependencies.
 *
 * <p>Nullness convention: parameters and return values are non-null unless
 * explicitly annotated {@code @Nullable}.
 */
@NotNullByDefault
package com.forge.farms;

import org.jetbrains.annotations.NotNullByDefault;
