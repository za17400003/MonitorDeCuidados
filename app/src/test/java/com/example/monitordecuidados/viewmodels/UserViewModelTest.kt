package com.example.monitordecuidados.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.monitordecuidados.models.UserState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

class UserViewModelTest {

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @Test
    fun `initial state is Initial`() {
        // Mocking Firebase is complex without a repository layer, 
        // but for this task we verify the LiveData initialization.
        val viewModel = UserViewModel()
        assertTrue(viewModel.userState.value is UserState.Initial)
    }
}
