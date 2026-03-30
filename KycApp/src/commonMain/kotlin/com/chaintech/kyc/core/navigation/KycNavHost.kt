package com.chaintech.kyc.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.chaintech.kyc.ui.HomeViewModel
import kotlinx.coroutines.launch
import com.chaintech.kyc.core.navigation.kycNavigation.KycNavigation
import com.chaintech.kyc.theme.KycAppColor

@Composable
fun KycNavHost(
    viewModel: HomeViewModel,
    finishActivity:()-> Unit
) {

    val navController = rememberNavController()
    LaunchedEffect(Unit, block = {
        launch {
            KycNavigation(
                navController = navController,
                viewModel = viewModel,
                finishActivity = finishActivity
            )
        }
    })

    Scaffold(
        containerColor = KycAppColor.colors.white,
        modifier = Modifier
            .fillMaxSize()
            .background(KycAppColor.colors.white)
            .navigationBarsPadding()
    ) { padding ->
        KycNavigationGraph(
            modifier = Modifier.padding(padding),
            navController = navController,
            homeViewModel = viewModel
        )
    }
}

