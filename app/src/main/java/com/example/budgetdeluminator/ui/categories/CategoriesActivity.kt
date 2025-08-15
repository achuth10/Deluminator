package com.example.budgetdeluminator.ui.categories

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgetdeluminator.R
import com.example.budgetdeluminator.data.entity.BudgetCategory
import com.example.budgetdeluminator.databinding.ActivityCategoriesBinding
import com.example.budgetdeluminator.databinding.DialogAddCategoryBinding
import com.example.budgetdeluminator.ui.adapter.CategoryManagementAdapter
import com.google.android.material.navigation.NavigationView

class CategoriesActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityCategoriesBinding
    private lateinit var categoryAdapter: CategoryManagementAdapter
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private val categoriesViewModel: CategoriesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupNavigationDrawer()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupNavigationDrawer() {
        drawerToggle = ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, 0, 0)
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        binding.navigationView.setNavigationItemSelectedListener(this)

        // Set the current menu item as selected
        binding.navigationView.setCheckedItem(R.id.nav_categories)
    }

    private fun setupRecyclerView() {
        categoryAdapter =
                CategoryManagementAdapter(
                        onEditClick = { category -> showAddCategoryDialog(category) },
                        onDeleteClick = { category -> showDeleteCategoryDialog(category) }
                )

        binding.recyclerViewCategories.apply {
            adapter = categoryAdapter
            layoutManager = LinearLayoutManager(this@CategoriesActivity)
        }
    }

    private fun setupObservers() {
        categoriesViewModel.allCategories.observe(this) { categories ->
            categoryAdapter.submitList(categories)
        }
    }

    private fun setupClickListeners() {
        binding.fabAddCategory.setOnClickListener { showAddCategoryDialog() }
    }

    private fun showAddCategoryDialog(categoryToEdit: BudgetCategory? = null) {
        val dialogBinding = DialogAddCategoryBinding.inflate(LayoutInflater.from(this))

        // Pre-fill if editing
        categoryToEdit?.let { category ->
            dialogBinding.etCategoryName.setText(category.name)
            dialogBinding.etBudgetLimit.setText(category.budgetLimit.toString())
        }

        val dialog = AlertDialog.Builder(this).setView(dialogBinding.root).create()

        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }

        dialogBinding.btnSave.setOnClickListener {
            val name = dialogBinding.etCategoryName.text.toString().trim()
            val budgetLimitText = dialogBinding.etBudgetLimit.text.toString().trim()

            if (name.isEmpty() || budgetLimitText.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val budgetLimit = budgetLimitText.toDoubleOrNull()
            if (budgetLimit == null || budgetLimit <= 0) {
                Toast.makeText(this, "Please enter a valid budget amount", Toast.LENGTH_SHORT)
                        .show()
                return@setOnClickListener
            }

            if (categoryToEdit == null) {
                // Adding new category
                val newCategory = BudgetCategory(name = name, budgetLimit = budgetLimit)
                categoriesViewModel.insertCategory(newCategory)
                Toast.makeText(this, "Category added successfully", Toast.LENGTH_SHORT).show()
            } else {
                // Editing existing category
                val updatedCategory = categoryToEdit.copy(name = name, budgetLimit = budgetLimit)
                categoriesViewModel.updateCategory(updatedCategory)
                Toast.makeText(this, "Category updated successfully", Toast.LENGTH_SHORT).show()
            }

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteCategoryDialog(category: BudgetCategory) {
        AlertDialog.Builder(this)
                .setTitle("Delete Category")
                .setMessage(
                        "Are you sure you want to delete '${category.name}'? This will also delete all associated expenses."
                )
                .setPositiveButton("Delete") { _, _ ->
                    categoriesViewModel.deleteCategory(category)
                    Toast.makeText(this, "Category deleted", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                startActivity(Intent(this, com.example.budgetdeluminator.MainActivity::class.java))
                finish()
            }
            R.id.nav_categories -> {
                // Already on categories screen
            }
            R.id.nav_settings -> {
                startActivity(
                        Intent(
                                this,
                                com.example.budgetdeluminator.ui.settings.SettingsActivity::class
                                        .java
                        )
                )
                finish()
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
