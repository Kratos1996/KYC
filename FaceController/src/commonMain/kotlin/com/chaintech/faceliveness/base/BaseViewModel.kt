@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.faceliveness.base

// In commonMain/kotlin
import kotlinx.coroutines.CoroutineScope

expect open class BaseViewModel() {
    val viewModelScope: CoroutineScope
    protected open fun onCleared()
}
