package com.chaintech.kyc.core.navigation.kycNavigation

import androidx.navigation.NavHostController
import com.chaintech.kyc.ui.HomeViewModel
import kotlinx.coroutines.flow.collectLatest

sealed class Navigator {
    data class NavigateRoute(
        val route: Any // Any serializable route object
    ) : Navigator()

    data class NavigatePopBackStack(
        val route: Any,
        val inclusive: Boolean,
        val saveState: Boolean = false
    ) : Navigator()

    data class NavigateWithPopUp(
        val route: Any,
        val popUpToRoute: Any,
        val inclusive: Boolean = false,
    ): Navigator()


    data object NavigateBack: Navigator()

}
suspend fun KycNavigation(
    navController: NavHostController,
    viewModel: HomeViewModel,
    finishActivity:()-> Unit
) {
    viewModel.navListener.collectLatest { navigationData ->
        navigationCases(
            navigationData = navigationData,
            navController = navController,
            finishActivity = finishActivity
        )
    }
}

private fun navigationCases(
    navigationData: Navigator,
    navController: NavHostController,
    finishActivity:()-> Unit
) {
    when (navigationData) {
        is Navigator.NavigateRoute -> {
            navController.navigate(navigationData.route)
        }

        is Navigator.NavigatePopBackStack -> {
            navController.popBackStack(
                route = navigationData.route,
                inclusive = navigationData.inclusive,
                saveState = navigationData.saveState
            )
        }
        //This will navigate to navigationData.route and remove navigationData.popUpToRoute from the back stack. 🚀
        is Navigator.NavigateWithPopUp -> {
            navController.navigate(navigationData.route) {
                popUpTo(navigationData.popUpToRoute) {
                    inclusive = navigationData.inclusive // remove this from backstack
                }
            }
        }

        is Navigator.NavigateBack -> {
            if (!navController.popBackStack()) {
               finishActivity()
            }
        }
    }
}
