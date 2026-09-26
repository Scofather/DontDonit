package com.prismgrade

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.prismgrade.ui.navigation.PrismGradeNavHost
import com.prismgrade.ui.theme.PrismGradeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val services = (application as PrismGradeApp).services

        setContent {
            PrismGradeTheme {
                PrismGradeNavHost(services = services)
            }
        }
    }
}
