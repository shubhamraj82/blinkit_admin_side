package com.example.adminblinkit.fragments

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.adminblinkit.Utils.Constants
import com.example.adminblinkit.R
import com.example.adminblinkit.Utils.Utils
import com.example.adminblinkit.activity.AdminMainActivty
import com.example.adminblinkit.adapter.AdapterSelectedImage
import com.example.adminblinkit.databinding.FragmentAddProductBinding
import com.example.adminblinkit.model.Product
import com.example.adminblinkit.viewmodels.AdminViewModel
import kotlinx.coroutines.launch

class addProductFragment : Fragment() {

    private val viewModel : AdminViewModel by viewModels()
    private lateinit var binding: FragmentAddProductBinding
    private val imageUris : ArrayList<Uri> = arrayListOf()
    // The new way of making implicit intents which allows us to pick multiple images from gallery
    // This variables is used when the user clicks on the button to pick images
    val selectedImage = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { listOfUri ->// listOfUri receives the multiple images from the user
        val fiveImages = listOfUri.take(5) // only up to 5 images are kept
        imageUris.clear() // clear any previous images
        imageUris.addAll(fiveImages) /// add the new images

        binding.rvProductImages.adapter = AdapterSelectedImage(imageUris)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddProductBinding.inflate(layoutInflater)
        SetStatusBarColor()
        setAutoCompleteTextViews()
        onImageSelectClicked()
        onAddButtonClicked()


        return binding.root
    }

    // Display appropriate dialogue based on data given by admin
    // If required data is given, store it in the database
    private fun onAddButtonClicked() {
        binding.btnAddProduct.setOnClickListener {
            Utils.showDialog(requireContext(), "Uploading images....")
            val productTitle = binding.etProductTitle.text.toString()
            val productQuantity = binding.etProductQuantity.text.toString()
            val productPrice = binding.etProductPrice.text.toString()
            val productUnit = binding.etProductUnit.text.toString()
            val productStock = binding.etProductStock.text.toString()
            val productCategory = binding.etProductCategory.text.toString()
            val productType = binding.etProductType.text.toString()

            if(productTitle.isEmpty() || productQuantity.isEmpty() || productPrice.isEmpty()
                || productUnit.isEmpty() || productStock.isEmpty() || productCategory.isEmpty()
                || productType.isEmpty()) {
                Utils.apply {
                    Utils.hideDialog()
                    Utils.showToast(requireContext(), "Please fill all the fields")
                }
            } else if(imageUris.isEmpty()) {
                Utils.apply {
                    Utils.hideDialog()
                    Utils.showToast(requireContext(), "Please upload some images")
                }
            } else {
                val product = Product(
                    productTitle = productTitle,
                    productQuantity = productQuantity.toInt(),
                    productUnit = productUnit,
                    productPrice = productPrice.toInt(),
                    productStock = productStock.toInt(),
                    productCategory = productCategory,
                    productType = productType,
                    itemCount = 0,
                    adminUid = Utils.getCurrentUserId(),
                    productRandomId = Utils.getRandomId()
                    // Not updating productImageUris because we need their URLs from the database itself
                )
                saveImage(product)
            }
        }
    }

    // save the images in the database
    private fun saveImage(product: Product) {
        viewModel.saveImageInDB(imageUris) // passing all the images the user has given as input to be stored in the database
        // collect is a suspend fun therefore using coroutines
        lifecycleScope.launch {
            viewModel.isImagesUploaded.collect {
                if(it) {
                    Utils.apply {
                        Utils.hideDialog()
                        Utils.showToast(requireContext(), "Image Saved")
                    }
                    getUrls(product)
                }
            }
        }
    }

    private fun getUrls(product: Product) {
        Utils.showDialog(requireContext(), "Publishing Product....")
        lifecycleScope.launch {
            viewModel.downloadedUrls.collect {
                val urls = it
                product.productImageUris = urls
                saveProduct(product)
            }
        }
    }

    private fun saveProduct(product: Product) {
        viewModel.saveProduct(product)
        lifecycleScope.launch {
            viewModel.isProductSaved.collect {
                if(it) {
                    Utils.hideDialog()
                    startActivity(Intent(requireActivity(), AdminMainActivty::class.java))
                    Utils.showToast(requireContext(), "Your product is live")
                }
            }
        }
    }

    // Only getting 5 images of the product from the user
    private fun onImageSelectClicked() {
        binding.btnPickImage.setOnClickListener {
            // The older version of implicit intent has been deprecated
            selectedImage.launch("image/*") // take in as input all types of image files such as png, jpg etc.
        }
    }

    // setting auto complete functionality for drop down lists of units, category and product type
    private fun setAutoCompleteTextViews() {
        val units = ArrayAdapter(requireContext(), R.layout.show_list, Constants.allProductsUnits)
        val category = ArrayAdapter(requireContext(), R.layout.show_list, Constants.allProductsCategory)
        val productType = ArrayAdapter(requireContext(), R.layout.show_list, Constants.allProductTypes)

        binding.apply {
            etProductUnit.setAdapter(units)
            etProductCategory.setAdapter(category)
            etProductType.setAdapter(productType)
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