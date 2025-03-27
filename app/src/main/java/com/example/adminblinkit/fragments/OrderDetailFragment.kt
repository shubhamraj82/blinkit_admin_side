package com.example.adminblinkit.fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.adminblinkit.R
import com.example.adminblinkit.Utils.Utils
import com.example.adminblinkit.adapter.AdapterCartProducts
import com.example.adminblinkit.databinding.FragmentOrderDetailBinding
import com.example.adminblinkit.viewmodels.AdminViewModel
import kotlinx.coroutines.launch

class OrderDetailFragment : Fragment() {

    private val viewModel : AdminViewModel by viewModels()
    private lateinit var binding : FragmentOrderDetailBinding
    private lateinit var adapterCartProducts: AdapterCartProducts
    private var status : Int = 0
    private var currentStatus : Int = 0
    private var orderId : String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOrderDetailBinding.inflate(layoutInflater)

        getValues() // Getting order status and id from the OrdersFragment
        SetStatusBarColor()
        settingStatus(status)
        onBackButtonClicked()
        lifecycleScope.launch { getOrderedProducts() }
        onChangeStatusButtonClicked()

        return binding.root
    }

    private fun onChangeStatusButtonClicked() {
        binding.btnChangeStatus.setOnClickListener {
            val popupMenu = PopupMenu(requireContext(), it) // it refers to the view
            popupMenu.menuInflater.inflate(R.menu.menu_popup, popupMenu.menu) // location and the layout
            popupMenu.show()
            popupMenu.setOnMenuItemClickListener { menu ->

                when(menu.itemId) {
                    R.id.menuReceived -> {
                        currentStatus = 1
                        if (currentStatus > status) { // If a product has been delivered, admin cant change its status back to Received
                            status = 1
                            settingStatus(1)
                            viewModel.updateOrderStatus(orderId, 1)
                        } else {
                            Utils.showToast(requireContext(), "Order has already been received")
                        }
                        true
                    } R.id.menuDispatched -> {
                        currentStatus = 2
                        if (currentStatus > status) {
                            status = 2
                            settingStatus(2)
                            viewModel.updateOrderStatus(orderId, 2)
                        } else {
                            Utils.showToast(requireContext(), "Order has already been dispatched")
                        }
                        true
                    } R.id.menuDelivered -> {
                        currentStatus = 3
                        if (currentStatus > status) {
                            status = 3
                            settingStatus(3)
                            viewModel.updateOrderStatus(orderId, 3)
                        } else {
                            Utils.showToast(requireContext(), "Order has already been delivered")
                        }
                        true
                    } else -> {
                        false
                    }
                }
            }
        }
    }

    // Function to get the ordered products and display them in recycler view
    suspend fun getOrderedProducts() {
        viewModel.getOrderedProducts(orderId).collect { cartList ->
            adapterCartProducts = AdapterCartProducts()
            binding.rvProductItems.adapter = adapterCartProducts
            adapterCartProducts.differ.submitList(cartList)
        }
    }

    // setting the color of the imageView in the OrderDetailFragment based on the status
    private fun settingStatus(status : Int) {
        val statusToViews = mapOf(
            0 to listOf(binding.iv1),
            1 to listOf(binding.iv1, binding.iv2, binding.view1),
            2 to listOf(binding.iv1, binding.iv2, binding.view1, binding.iv3, binding.view2),
            3 to listOf(
                binding.iv1,
                binding.iv2,
                binding.view1,
                binding.iv3,
                binding.view2,
                binding.iv4,
                binding.view3
            )
        )

        val viewsToTint = statusToViews.getOrDefault(status, emptyList())
        for (view in viewsToTint) {
            view.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.blue)
        }
    }

    // Getting order status and id from the OrdersFragment
    private fun getValues() {
        val bundle = arguments
        status = bundle?.getInt("status")!!
        orderId = bundle?.getString("orderId")!!
        binding.tvUserAddress.text = bundle?.getString("userAddress")!!
    }

    private fun onBackButtonClicked() {
        binding.tbOrderDetailFragment.setOnClickListener {
            findNavController().navigate(R.id.action_orderDetailFragment_to_orderFragment)
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