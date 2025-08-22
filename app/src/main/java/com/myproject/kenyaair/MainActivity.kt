package com.myproject.kenyaair

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myproject.kenyaair.data.AirRepository
import com.myproject.kenyaair.data.net.OpenAQApi
import com.myproject.kenyaair.data.net.OpenAQClient
import com.myproject.kenyaair.ui.AirVMFactory
import com.myproject.kenyaair.ui.AirViewModel
import com.myproject.kenyaair.ui.AppScreen
import retrofit2.create

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val api = OpenAQClient.retrofit.create<OpenAQApi>()
        val repo = AirRepository(api)

        setContent {
            val vm: AirViewModel = viewModel(factory = AirVMFactory(repo))
            AppScreen(vm)
        }
    }
}
