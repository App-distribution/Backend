package com.appdist.app.navigation

import androidx.lifecycle.ViewModel
import com.appdist.core.datastore.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppNavViewModel @Inject constructor(
    val tokenManager: TokenManager
) : ViewModel()
