package com.example.adminblinkit.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.adminblinkit.activity.AdminMainActivty
import com.example.adminblinkit.R
import com.example.adminblinkit.model.Admins
import com.example.adminblinkit.Utils.Utils
import com.example.adminblinkit.databinding.FragmentOTPBinding
import com.example.adminblinkit.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

class OTPFragment : Fragment() {

    private val viewModel : AuthViewModel by viewModels()
    private lateinit var binding: FragmentOTPBinding
    private lateinit var userNumber : String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentOTPBinding.inflate(layoutInflater)
        getUserNumber()
        customizingEnteringOTP() // function to move the OTP editText to next editText when a number has been entered
        sendOTP()
        onLoginButtonClicked()
        onBackButtonClicked()
        return binding.root
    }

    // When u click the login button collect the OTP and send for verification
    private fun onLoginButtonClicked() {
        binding.btnLogin.setOnClickListener {
            Utils.showDialog(requireContext(), "Signing you up!")
            val editTexts = arrayOf(
                binding.otp1,
                binding.otp2,
                binding.otp3,
                binding.otp4,
                binding.otp5,
                binding.otp6
            )

            // join the OTPs together to form a single OTP string
            val otp = editTexts.joinToString("") {
                it.text.toString()
            }

            if(otp.length < editTexts.size) {
                Utils.showToast(requireContext(), "Please enter the correct OTP")
            } else {
                // clear the otp text fields after an attempt of login has been made
                editTexts.forEach {
                    it.text?.clear()
                    it.clearFocus()
                }
                // Verifying the OTP
                verifyOtp(otp)
            }
        }
    }

    // this function verifies the OTP with 'signInWithPhoneAuthCredential' in viewModel
    private fun verifyOtp(otp: String) {
        val user = Admins(uid = null, userPhoneNumber = userNumber)

        viewModel.signInWithPhoneAuthCredential(otp, userNumber, user)

        // collect is a suspend fun therefore use coroutines
        lifecycleScope.launch {
            viewModel.isSignedInSuccessfully.collect {
                if (it) {
                    Utils.hideDialog()
                    Utils.showToast(requireContext(), "Logged IN...")
                    startActivity(Intent(requireActivity(), AdminMainActivty::class.java))
                    requireActivity().finish()
                }
            }
        }
    }

    private fun sendOTP() {
        Utils.showDialog(requireContext(), "Sending OTP....")
        viewModel.apply {
            sendOTP(userNumber, requireActivity())
            lifecycleScope.launch { // using coroutine because 'collect' below is a suspend function
                otpSent.collect { otpSent ->
                    if(otpSent) {
                        Utils.hideDialog()
                        Utils.showToast(requireContext(), "OTP sent to the number..", )
                    }
                }
            }
        }

    }

    // functionality to move back to the previous fragment
    private fun onBackButtonClicked() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigate(R.id.action_OTPFragment_to_signinFragment)
        }
    }

    // function to move the OTP editText to next editText when a number has been entered
    private fun customizingEnteringOTP() {
        val editTexts = arrayOf(
            binding.otp1,
            binding.otp2,
            binding.otp3,
            binding.otp4,
            binding.otp5,
            binding.otp6
        )
        for (i in editTexts.indices) {
            editTexts[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1) { // If you enter a number, move to the next EditText
                        if (i < editTexts.size - 1) {
                            editTexts[i + 1].requestFocus()
                        }
                    } else if (s?.length == 0) { // If you remove the number, move back to the previous EditText
                        if (i > 0) {
                            editTexts[i - 1].requestFocus()
                        }
                    }
                }
            })
        }
    }

    // get the user number which was sent by signin fragment and display it
    private fun getUserNumber() {
        val bundle = arguments
        userNumber = bundle?.getString("number").toString()

        binding.tvUsernumber.text = userNumber
    }
}