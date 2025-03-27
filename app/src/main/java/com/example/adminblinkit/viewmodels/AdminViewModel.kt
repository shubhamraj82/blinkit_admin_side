package com.example.adminblinkit.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.adminblinkit.Utils.Utils
import com.example.adminblinkit.model.CartProducts
import com.example.adminblinkit.model.Orders
import com.example.adminblinkit.model.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

class AdminViewModel : ViewModel() {

    private val _isImagesUploaded = MutableStateFlow(false)
    var isImagesUploaded : StateFlow<Boolean> = _isImagesUploaded

    private val _downloadUrls = MutableStateFlow<ArrayList<String?>>(arrayListOf())
    var downloadedUrls : StateFlow<ArrayList<String?>> = _downloadUrls

    private val _isProductSaved = MutableStateFlow(false)
    var isProductSaved : StateFlow<Boolean> = _isProductSaved

    // get all the images in the arraylist
   fun saveImageInDB(imageUri : ArrayList<Uri>) {
       val downloadUrls = ArrayList<String?>()

       imageUri.forEach { uri ->
           // save images to the corresponding adminUid in the database
           val imageRef = FirebaseStorage.getInstance().reference // gives reference to the storage
               .child(Utils.getCurrentUserId()) // creates a folder for each user as per their userId
               .child("Images") // place image inside a 'image' folder
               .child(UUID.randomUUID().toString()) // give a random UID to each image otherwise they will override each other

           // Upload the image to firebase storage
           imageRef.putFile(uri).continueWithTask { // upload the image and wait for upload to complete
               imageRef.downloadUrl // download the URLs for the images
           }.addOnCompleteListener { task ->
               val url = task.result // task.result holds the downloaded URL for the uploaded image
               downloadUrls.add(url.toString()) // add the downloaded url to downloadUrls arraylist

               if (downloadUrls.size == imageUri.size) {
                   _isImagesUploaded.value = true
                   _downloadUrls.value = downloadUrls
               }
           }
       }
   }

    // Here the same data is stored in 3 different locations because
    // By storing data in multiple locations, the app can fetch specific data faster without requiring complex queries.
    fun saveProduct(product: Product) {
        // saving product data in realtime database, whereas images were saved in storage
        FirebaseDatabase.getInstance().getReference("Admins").child("AllProducts/${product.productRandomId}").setValue(product)
            .addOnSuccessListener {
                FirebaseDatabase.getInstance().getReference("Admins")
                    .child("ProductCategory/${product.productCategory}/${product.productRandomId}").setValue(product)
                    .addOnSuccessListener {
                        FirebaseDatabase.getInstance().getReference("Admins")
                            .child("ProductType/${product.productType}/${product.productRandomId}").setValue(product)
                            .addOnSuccessListener {
                                _isProductSaved.value = true
                            }
                    }
            }
    }

    // It returns a Flow<List<Product>>, meaning it provides a stream of product lists that
    // updates in real-time when the database changes.
    fun fetchAllTheProducts(category: String): Flow<List<Product>> = callbackFlow {
        val db = FirebaseDatabase.getInstance().getReference("Admins").child("AllProducts")

        // ValueEventListener is an interface that listens for data changes in Firebase.
        val eventListener = object : ValueEventListener {

            // This method is triggered every time the data at "AllProducts" changes (new products added, updated, or deleted).
            override fun onDataChange(snapshot: DataSnapshot) {
                val products = ArrayList<Product>()
                for (product in snapshot.children) { // Iterates through each product entry inside "AllProducts"
                    val prod = product.getValue(Product::class.java) // convert each snapshot into a Project object
                    if(category == "All" || prod?.productCategory == category) {
                        products.add(prod!!)
                    }
                }
                // Since callbackFlow works asynchronously, trySend(products) ensures that new data is immediately
                // available to whoever is collecting the Flow.
                trySend(products)
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        }

        // This attaches eventListener to db, meaning Firebase will continuously listen for changes in "AllProducts"
        db.addValueEventListener(eventListener)

        //  ensures that when the Flow collector stops listening, we remove the Firebase listener.
        awaitClose { db.removeEventListener(eventListener)
        }
    }

    fun getAllOrders() : Flow<List<Orders>> = callbackFlow {
        val db = FirebaseDatabase.getInstance().getReference("Admins").child("Orders").orderByChild("orderStatus")

        // ValueEventListener is an interface that listens for data changes in Firebase in realtime
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val orderList = ArrayList<Orders>()

                for (orders in snapshot.children) {
                    val order = orders.getValue(Orders::class.java)
                    if (order != null) {
                        orderList.add(order)
                    }
                }
                trySend(orderList)
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        }
        db.addValueEventListener(eventListener)
        awaitClose { // remove the eventlistener
            db.removeEventListener(eventListener)
        }
    }

    fun getOrderedProducts(orderId : String) : Flow<List<CartProducts>> = callbackFlow {
        val db = FirebaseDatabase.getInstance().getReference("Admins").child("Orders").child(orderId)
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val order = snapshot.getValue(Orders::class.java)
                trySend(order?.orderList!!)
            }

            override fun onCancelled(error: DatabaseError) { }
        }

        db.addValueEventListener(eventListener)
        awaitClose {
            db.removeEventListener(eventListener)
        }
    }

    fun savingUpdateProducts(product: Product) {
        FirebaseDatabase.getInstance().getReference("Admins").child("AllProducts/${product.productRandomId}").setValue(product)
        FirebaseDatabase.getInstance().getReference("Admins").child("ProductCategory/${product.productCategory}/${product.productRandomId}").setValue(product)
        FirebaseDatabase.getInstance().getReference("Admins").child("ProductType/${product.productType}/${product.productRandomId}").setValue(product)
    }

    fun logOutUser() {
        FirebaseAuth.getInstance().signOut()
    }

    // Updating the status of the order in the database
    fun updateOrderStatus(orderId : String, status : Int) {
        FirebaseDatabase.getInstance().getReference("Admins").child("Orders").child(orderId).child("orderStatus").setValue(status)
    }
}