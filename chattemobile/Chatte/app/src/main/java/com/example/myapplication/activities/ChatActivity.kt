package com.example.myapplication.activities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Toast
import com.example.myapplication.adapters.ChatAdapter
import com.example.myapplication.databinding.ActivityChatBinding
import com.example.myapplication.models.ChatMessage
import com.example.myapplication.models.User
import com.example.myapplication.network.ApiClient
import com.example.myapplication.network.ApiService
import com.example.myapplication.utilities.Constants
import com.example.myapplication.utilities.PreferenceManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

import kotlin.collections.HashMap

class ChatActivity : BaseActivity() {

    private var apiClient: ApiClient = ApiClient()
    private lateinit var binding: ActivityChatBinding
    private var receiveUser: User = User()
    private val constant = Constants()
    private val chatMessages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter
    private var preferenceManager: PreferenceManager = PreferenceManager()
    private lateinit var database: FirebaseFirestore
    private var conversionId: String? = null
    private var isReceiverAvailable: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setListeners()
        loadReceiverDetails()
        init()
        listenMessages()
    }

    private fun init(){
        apiClient = ApiClient()
        preferenceManager.preferenceManager(applicationContext)
        // chatMessage = ArrayListOf<>()
        chatAdapter = ChatAdapter(
            chatMessages,
            getBitmapFromEncodedString(receiveUser.image),
            preferenceManager.getString(constant.KEY_USER_ID)!!
        )
        binding.chatRecyclerView.adapter = chatAdapter
        database = FirebaseFirestore.getInstance()
    }

     private fun sendMessage(){
        val message = hashMapOf<String, Any>()
        message[constant.KEY_SENDER_ID] = preferenceManager.getString(constant.KEY_USER_ID)!!
        message[constant.KEY_RECEIVER_ID] = receiveUser.id
        message[constant.KEY_MESSAGE] = binding.inputMessage.text.toString()
        message[constant.KEY_TIMESTAMP] = Date()
        database.collection(constant.KEY_COLLECTION_CHAT).add(message)
         if(conversionId != null){
             updateConversion(binding.inputMessage.text.toString())
         } else {
             val conversion = HashMap<String, Any>()
             conversion[constant.KEY_SENDER_ID] = preferenceManager.getString(constant.KEY_USER_ID)!!
             conversion[constant.KEY_SENDER_NAME] = preferenceManager.getString(constant.KEY_NAME)!!
             conversion[constant.KEY_RECEIVER_ID] = receiveUser.id
             conversion[constant.KEY_RECEIVER_NAME] = receiveUser.name
             conversion[constant.KEY_RECEIVER_IMAGE] = receiveUser.image
             conversion[constant.KEY_LAST_MESSAGE] = binding.inputMessage.text.toString()
             conversion[constant.KEY_TIMESTAMP] = Date()
             addConversion(conversion)
         }
         if(!isReceiverAvailable){
             try{
                val tokens:JSONArray = JSONArray()
                 tokens.put(receiveUser.token)

                val data: JSONObject = JSONObject()
                 data.put(constant.KEY_USER_ID, preferenceManager.getString(constant.KEY_USER_ID))
                 data.put(constant.KEY_NAME, preferenceManager.getString(constant.KEY_NAME))
                 data.put(constant.KEY_FCM_TOKEN, preferenceManager.getString(constant.KEY_FCM_TOKEN))
                 data.put(constant.KEY_MESSAGE, binding.inputMessage.text.toString())


                 val body : JSONObject = JSONObject()
                 body.put(constant.REMOTE_MSG_DATA, data)
                 body.put(constant.REMOTE_MSG_REGISTRATION_IDS, tokens)

                 sendNotification(body.toString())
             } catch (exception: Exception){
                 showToast(exception.message!!)
             }
         }
        binding.inputMessage.text = null
    }

   private fun showToast(message: String){
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun sendNotification(messageBody: String) {

        apiClient.getClient().create(ApiService::class.java).sendMessage(constant.getRemoteMsgHeaders2()!!, messageBody
        ).enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if(response.isSuccessful){
                    try{
                        if (response.body() != null){
                            val responseJson : JSONObject = JSONObject(response.body())
                            val results: JSONArray = responseJson.getJSONArray("results")
                            if(responseJson.getInt("failure") == 1){
                                val error: JSONObject = results.get(0) as JSONObject
                                showToast(error.getString("error"))
                                return
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                    showToast("Notificação enviada com sucesso")
                } else {
                    showToast("Error: " + response.code() )
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                showToast(t.message!!)
            }
        })
    }



    private fun listenAvailabilityOfReceiver() {
        database.collection(constant.KEY_COLLECTION_USERS).document(
            receiveUser.id
        ).addSnapshotListener { value, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            if (value != null) {
                if(value.getLong(constant.KEY_AVAILABILITY) != null){
                    val availability = value.getLong(constant.KEY_AVAILABILITY)?.toInt()
                    isReceiverAvailable = availability == 1
                }
                receiveUser.token = value.getString(constant.KEY_FCM_TOKEN)!!
            }
            if(isReceiverAvailable){
                binding.textAvailability.visibility = View.VISIBLE
        } else {
                binding.textAvailability.visibility = View.GONE
            }
        }
    }

    private fun listenMessages(){
       database.collection(constant.KEY_COLLECTION_CHAT)
           .whereEqualTo(constant.KEY_SENDER_ID, preferenceManager.getString(constant.KEY_USER_ID))
           .whereEqualTo(constant.KEY_RECEIVER_ID, receiveUser.id)
           .addSnapshotListener(eventListener)
        database.collection(constant.KEY_COLLECTION_CHAT)
            .whereEqualTo(constant.KEY_SENDER_ID, receiveUser.id)
            .whereEqualTo(constant.KEY_RECEIVER_ID, preferenceManager.getString(constant.KEY_USER_ID))
            .addSnapshotListener(eventListener)
    }

    private fun getBitmapFromEncodedString(encodedImage: String): Bitmap {
        val bytes = Base64.decode(preferenceManager.getString(constant.KEY_IMAGE), Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun loadReceiverDetails() {
        receiveUser = intent.getParcelableExtra(constant.KEY_USER)!!
        binding.textName.text = receiveUser.name
    }
    private fun setListeners(){
        binding.imageBack.setOnClickListener{ finish() }
        binding.layoutSend.setOnClickListener { sendMessage() }
    }

    private fun getReadableDatetime(date: Date): String{
        return SimpleDateFormat("dd/MM/yyyy - hh:mm a", Locale.getDefault()).format(date)

    }

    private fun addConversion(conversion: HashMap<String, Any>) {
        database.collection(constant.KEY_COLLECTION_CONVERSATIONS)
            .add(conversion)
            .addOnSuccessListener { documentReference ->
                conversionId = documentReference.id
            }
    }

    private fun updateConversion(message: String) {
        val documentReference: DocumentReference =
            database.collection(constant.KEY_COLLECTION_CONVERSATIONS).document(conversionId!!)
        documentReference.update(
            constant.KEY_LAST_MESSAGE, message,
            constant.KEY_TIMESTAMP, Date()
        )
    }


    private fun checkForConversion(){
        if(chatMessages.size != 0){
            checkForConversionRemately(
                preferenceManager.getString(constant.KEY_USER_ID)!!,
                receiveUser.id
            )
            checkForConversionRemately(
                receiveUser.id,
                preferenceManager.getString(constant.KEY_USER_ID)!!
            )
        }
    }

    private fun checkForConversionRemately(senderid: String , receiverId: String){
        database.collection(constant.KEY_COLLECTION_CONVERSATIONS)
            .whereEqualTo(constant.KEY_SENDER_ID, senderid)
            .whereEqualTo(constant.KEY_RECEIVER_ID,receiverId )
            .get()
            .addOnCompleteListener(conversionOnCompleteListener)
    }

    private val conversionOnCompleteListener = OnCompleteListener<QuerySnapshot> { task ->
        if (task.isSuccessful && task.result != null && task.result.documents.isNotEmpty()) {
            val documentSnapshot = task.result.documents[0]
            conversionId = documentSnapshot.id
        }
    }


    private val eventListener = com.google.firebase.firestore.EventListener<QuerySnapshot> { value, error ->
        if (error != null) {
            return@EventListener
        }
        value?.let { snapshot ->
            val count = chatMessages.size
            for (documentChange in value.documentChanges) {
                val chatMessage = ChatMessage()
                chatMessage.senderId = documentChange.document.getString(constant.KEY_SENDER_ID).toString()
                chatMessage.receiverId = documentChange.document.getString(constant.KEY_RECEIVER_ID).toString()
                chatMessage.message = documentChange.document.getString(constant.KEY_MESSAGE).toString()
                chatMessage.dateTime = getReadableDatetime(documentChange.document.getDate(constant.KEY_TIMESTAMP)!!)
                chatMessage.dateObject = documentChange.document.getDate(constant.KEY_TIMESTAMP)!!
                chatMessages.add(chatMessage)
            }
            chatMessages.sortWith(compareBy { it.dateObject })
            if (count == 0) {
                chatAdapter.notifyDataSetChanged()
            } else {
                chatAdapter.notifyItemRangeChanged(chatMessages.size, chatMessages.size)
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size - 1)
            }
            binding.chatRecyclerView.visibility = View.VISIBLE
        }
        binding.progressBar.visibility = View.GONE
        if(conversionId == null){
            checkForConversion()
        }
    }

    override fun onResume() {
        super.onResume()
        listenAvailabilityOfReceiver()
    }

}