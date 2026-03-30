@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.faceliveness.base

import kotlinx.coroutines.CoroutineScope

// In iosMain/kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

actual open class BaseViewModel actual constructor() {
    actual val viewModelScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    protected actual open fun onCleared() {
        viewModelScope.cancel()
    }
}
