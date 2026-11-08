package com.hextech.vango.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Shared profile repository + VM scope for profile and login flows inside Settings. */
val profileRepositorySingleton = DefaultProfileRepository()

@Composable
fun rememberProfileViewModel(): ProfileViewModel = remember {
    ProfileViewModel(profileRepositorySingleton)
}
