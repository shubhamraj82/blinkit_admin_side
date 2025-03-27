package com.example.adminblinkit.fragments

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.adminblinkit.Utils.Constants
import com.example.adminblinkit.R
import com.example.adminblinkit.Utils.Utils
import com.example.adminblinkit.activity.AuthMainActivty
import com.example.adminblinkit.adapter.AdapterProduct
import com.example.adminblinkit.adapter.CategoriesAdapter
import com.example.adminblinkit.databinding.EditProductLayoutBinding
import com.example.adminblinkit.databinding.FragmentHomeBinding
import com.example.adminblinkit.model.Categories
import com.example.adminblinkit.model.Product
import com.example.adminblinkit.viewmodels.AdminViewModel
import kotlinx.coroutines.launch

class homeFragment : Fragment() {

    val viewModel : AdminViewModel by viewModels()
    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapterProduct: AdapterProduct

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        SetStatusBarColor()
        onLogout()
        setCategories()
        getAllTheProducts("All")
        searchProducts()

        return binding.root
    }

    private fun onLogout() {
        binding.tbHomeFragment.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menuLogout -> {
                    logOutUser()
                    true
                } else -> {false}
            }
        }
    }


    private fun logOutUser() {
        val builder = AlertDialog.Builder(requireContext())
        val alertDialog = builder.create()

        builder.setTitle("Log Out")
            .setMessage("Do you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.logOutUser()
                startActivity(Intent(requireContext(), AuthMainActivty::class.java))
                requireActivity().finish()
            }
            .setNegativeButton("No") { _, _ ->
                alertDialog.dismiss()
            }

            .show()
            // If you click outside the layout, the alert dialog wont dismiss itself
            // Force user to click on Yes or No to log out
            .setCancelable(false)
    }


    private fun searchProducts() {
        binding.searchEt.addTextChangedListener(object : TextWatcher{

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                // the first filter is the original list of all the products in DB using the 'getFilter function'
                // the second filter will take us to the FilteringProducts class's performFiltering function
                adapterProduct.filter.filter(query)
            }
        })
    }

    private fun getAllTheProducts(category: String) {
        binding.shimmerViewContainer.visibility = View.VISIBLE
        lifecycleScope.launch {
            viewModel.fetchAllTheProducts(category).collect {
                if(it.isEmpty()) {
                    binding.rvProducts.visibility = View.GONE
                    binding.tvText.visibility = View.VISIBLE
                } else {
                    binding.rvProducts.visibility = View.VISIBLE
                    binding.tvText.visibility = View.GONE
                }
                adapterProduct = AdapterProduct(::onEditButtonClicked) // adapter for the recycler view which shows the product information
                binding.rvProducts.adapter = adapterProduct // set the adapter
                adapterProduct.differ.submitList(it) // pass the list of products to the adapter
                adapterProduct.originalList = it as ArrayList<Product> // passing the original list for searching purpose
                binding.shimmerViewContainer.visibility = View.GONE
            }
        }
    }

    private fun setCategories() {
        val categoryList = ArrayList<Categories>()
        for(i in Constants.allProductsCategory.indices) {
            categoryList.add(Categories(Constants.allProductsCategory[i], Constants.allProductCategoryIcon[i]))
        }

        // passing a function as a parameter can be done using ' :: ' symbol
        binding.rvCategories.adapter = CategoriesAdapter(categoryList, ::onCategoryClicked)
    }

    private fun onCategoryClicked(categories: Categories) {
        getAllTheProducts(categories.category)
    }

    private fun onEditButtonClicked(product: Product) {
        val editProduct = EditProductLayoutBinding.inflate(LayoutInflater.from(requireContext()))

        // Set the product data into the text fields of the alert dialog
        editProduct.apply {
            etProductTitle.setText(product.productTitle)
            editProductQuantity.setText(product.productQuantity.toString())
            editProductUnit.setText(product.productUnit)
            editProductPrice.setText(product.productPrice.toString())
            editProductStock.setText(product.productStock.toString())
            editProductCategory.setText(product.productCategory)
            editProductType.setText(product.productType)
        }

        // alert dialog is the small screen which pops up when user clicks on a button
        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(editProduct.root)
            .create()
        alertDialog.show()

        // when user clicks on edit button, enable the edit text fields and set the auto complete text views
        editProduct.btnEditProduct.setOnClickListener {
            editProduct.apply {
                etProductTitle.isEnabled = true
                editProductQuantity.isEnabled = true
                editProductUnit.isEnabled = true
                editProductPrice.isEnabled = true
                editProductStock.isEnabled = true
                editProductCategory.isEnabled = true
                editProductType.isEnabled = true
            }
            // set the auto complete text views for drop down textViews
            setAutoCompleteTextViews(editProduct)

            // change the old data with the newly entered data
            editProduct.btnSaveProduct.setOnClickListener {
                lifecycleScope.launch {
                    product.productTitle = editProduct.etProductTitle.text.toString()
                    product.productQuantity = editProduct.editProductQuantity.text.toString().toInt()
                    product.productUnit = editProduct.editProductUnit.text.toString()
                    product.productPrice = editProduct.editProductPrice.text.toString().toInt()
                    product.productStock = editProduct.editProductStock.text.toString().toInt()
                    product.productCategory = editProduct.editProductCategory.text.toString()
                    product.productType = editProduct.editProductType.text.toString()
                }

                // save the information of the new product into the database
                viewModel.savingUpdateProducts(product)
                Utils.showToast(requireContext(), "Product Updated Successfully")
                alertDialog.dismiss()
            }
        }
    }

    private fun setAutoCompleteTextViews(editProduct: EditProductLayoutBinding) {
        val units = ArrayAdapter(requireContext(), R.layout.show_list, Constants.allProductsUnits)
        val category = ArrayAdapter(requireContext(), R.layout.show_list, Constants.allProductsCategory)
        val productType = ArrayAdapter(requireContext(), R.layout.show_list, Constants.allProductTypes)

        editProduct.apply {
            editProductUnit.setAdapter(units)
            editProductCategory.setAdapter(category)
            editProductType.setAdapter(productType)
        }
    }

    fun SetStatusBarColor() {
        activity?.window?.apply {
            val statusBarColors = ContextCompat.getColor(requireContext(), R.color.yellow)
            statusBarColor = statusBarColors
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }
}
