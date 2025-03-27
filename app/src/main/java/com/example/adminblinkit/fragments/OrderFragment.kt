package com.example.adminblinkit.fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.adminblinkit.R
import com.example.adminblinkit.adapter.AdapterOrders
import com.example.adminblinkit.databinding.FragmentOrderBinding
import com.example.adminblinkit.model.OrderedItems
import com.example.adminblinkit.viewmodels.AdminViewModel
import kotlinx.coroutines.launch

class orderFragment : Fragment() {

    private val viewModel : AdminViewModel by viewModels()
    private lateinit var binding : FragmentOrderBinding
    private lateinit var adapterOrders : AdapterOrders

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOrderBinding.inflate(layoutInflater)

        SetStatusBarColor()
        getAllOrders()
        return binding.root
    }

    // Getting all the orders placed by a specific user
    private fun getAllOrders() {
        binding.shimmerViewContainerOrders.visibility = View.VISIBLE
        binding.tvText.visibility = View.GONE
        lifecycleScope.launch {
            viewModel.getAllOrders().collect { orderList -> // orderList contains all the orders by specific user

                if (orderList.isNotEmpty()) {
                    val orderedList = ArrayList<OrderedItems>()
                    for (orders in orderList) {
                        val title = StringBuilder()
                        var totalPrice = 0

                        for(products in orders.orderList!!) {
                            // substring to remove the rupee symbol
                            val price = products.productPrice?.substring(1)?.toInt()
                            val itemCount = products.productCount!!
                            totalPrice += (price?.times(itemCount)!!) // multiplying price and count

                            // Concatenating the titles so that they can be displayed as one in the TextView
                            title.append("${products.productCategory}, ")
                        }

                        val orderedItems = OrderedItems(orders.orderId, orders.orderDate, orders.orderStatus, title.toString(), totalPrice, orders.userAddress)
                        orderedList.add(orderedItems)
                    }
                    adapterOrders = AdapterOrders(requireContext(), ::onOrderItemViewClicked)
                    binding.rvOrders.adapter = adapterOrders
                    adapterOrders.differ.submitList(orderedList)
                    binding.shimmerViewContainerOrders.visibility = View.GONE
                } else {
                    binding.shimmerViewContainerOrders.visibility = View.GONE
                    binding.tvText.visibility = View.VISIBLE
                }
            }
        }
    }

    fun onOrderItemViewClicked(orderedItems: OrderedItems) {
        val bundle = Bundle()
        bundle.putInt("status", orderedItems.itemStatus!!)
        bundle.putString("orderId", orderedItems.orderId)
        bundle.putString("userAddress", orderedItems.userAddress)

        findNavController().navigate(R.id.action_orderFragment_to_orderDetailFragment, bundle)
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