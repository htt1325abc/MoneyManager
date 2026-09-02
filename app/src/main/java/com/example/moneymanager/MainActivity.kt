package com.example.moneymanager

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.moneymanager.databinding.ActivityMainBinding
import com.example.moneymanager.presentation.home.HomeFragment
import com.example.moneymanager.presentation.main.MoneyManagerViewModel
import com.example.moneymanager.presentation.main.MoneyManagerViewModelFactory
import com.example.moneymanager.presentation.main.UiEvent
import com.example.moneymanager.presentation.statistics.StatisticsFragment
import com.example.moneymanager.presentation.transaction.TransactionFormDialogFragment
import com.example.moneymanager.presentation.transaction.TransactionsFragment
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val viewModel: MoneyManagerViewModel by viewModels {
        val app = application as MoneyManagerApplication
        MoneyManagerViewModelFactory(app.transactionRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemInsets()
        configureNavigation()
        collectEvents()

        if (savedInstanceState == null) {
            showScreen(HomeFragment(), HOME_TAG)
        }
    }

    fun showHome() = selectScreen(R.id.homeButton, HOME_TAG) { HomeFragment() }

    fun showTransactions() = selectScreen(
        R.id.transactionsButton,
        TRANSACTIONS_TAG,
    ) { TransactionsFragment() }

    fun showStatistics() = selectScreen(
        R.id.statisticsButton,
        STATISTICS_TAG,
    ) { StatisticsFragment() }

    fun showTransactionForm(transactionId: Long? = null) {
        TransactionFormDialogFragment.newInstance(transactionId)
            .show(supportFragmentManager, TransactionFormDialogFragment.TAG)
    }

    private fun configureNavigation() {
        binding.navigationGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.homeButton -> showScreen(HomeFragment(), HOME_TAG)
                R.id.transactionsButton -> showScreen(TransactionsFragment(), TRANSACTIONS_TAG)
                R.id.statisticsButton -> showScreen(StatisticsFragment(), STATISTICS_TAG)
            }
        }
    }

    private fun selectScreen(buttonId: Int, tag: String, createFragment: () -> Fragment) {
        if (binding.navigationGroup.checkedButtonId == buttonId) {
            showScreen(createFragment(), tag)
        } else {
            binding.navigationGroup.check(buttonId)
        }
    }

    private fun showScreen(fragment: Fragment, tag: String) {
        if (supportFragmentManager.findFragmentByTag(tag)?.isVisible == true) return
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.screenContainer, fragment, tag)
            .commit()
    }

    private fun collectEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is UiEvent.ShowMessage -> Snackbar
                            .make(binding.root, event.message, Snackbar.LENGTH_SHORT)
                            .setAnchorView(binding.navigationCard)
                            .show()
                    }
                }
            }
        }
    }

    private fun applySystemInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private companion object {
        const val HOME_TAG = "home"
        const val TRANSACTIONS_TAG = "transactions"
        const val STATISTICS_TAG = "statistics"
    }
}
