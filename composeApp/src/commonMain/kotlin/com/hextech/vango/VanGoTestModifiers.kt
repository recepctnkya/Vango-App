package com.hextech.vango

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

fun Modifier.vangoTestTag(tag: String): Modifier =
    this
        .testTag(tag)
        .semantics { contentDescription = tag }
