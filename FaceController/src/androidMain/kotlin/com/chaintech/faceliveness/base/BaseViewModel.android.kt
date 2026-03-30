@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.faceliveness.base
// In androidMain/kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

actual open class BaseViewModel actual constructor() : ViewModel() {

    actual val viewModelScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    actual override fun onCleared() {
        super.onCleared()
    }
}
