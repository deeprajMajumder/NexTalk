package com.example.chatapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatActivity : AppCompatActivity() {
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageBox: EditText
    private lateinit var sendButton : ImageView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList : ArrayList<Message>
    private lateinit var mDbRef : DatabaseReference
    //senderRoom and receiverRoom is used to create unique room for sender and receiver so that the
    // message is private and the message is not reflected in all the user.
    // we create senderRoom and receiverRoom so that we have a unique room for sender and receiver
    var receiverRoom : String? = null
    var senderRoom :String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val name = intent.getStringExtra("name")//receiving intent information passed from ChatActivity
        val receiverUid = intent.getStringExtra("uid")

        val senderUid = FirebaseAuth.getInstance().currentUser?.uid
        mDbRef = FirebaseDatabase.getInstance().getReference()//initialising the database instance
        senderRoom = receiverUid + senderUid // this is done to create a unique room for sender
        receiverRoom = senderUid + receiverUid //this is done to create a unique room for receiver

        supportActionBar?.title = name //setting action bar name as same as of the chat name

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageBox = findViewById(R.id.messageBox)
        sendButton= findViewById(R.id.sentButton)

        messageList= ArrayList()//creating arraylist of message for adapter
        messageAdapter = MessageAdapter(this,messageList) // initialising adapter for recyclerview

        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = messageAdapter

        //logic for adding data to chatRecyclerView
        mDbRef.child("chats").child(senderRoom!!).child("messages")
            .addValueEventListener(object :ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    messageList.clear()// we have to clear the list so that it does not have repeating value when it is called again and again
                   for (postSnapshot in snapshot.children){
                       val message = postSnapshot.getValue(Message::class.java)//getting the value from Message DClass and storing it in message
                       messageList.add(message!!)// passing message and adding it to the messageList
                   }
                    messageAdapter.notifyDataSetChanged()// also notifying the adapter or else the recyclerView will not update

                }

                override fun onCancelled(error: DatabaseError) {

                }
            })

        //when sent button is clicked we add the message to the data base
        sendButton.setOnClickListener{
            //we get what is written in the message box
            val message = messageBox.text.toString()
            val messageObject = Message(message,senderUid)

            mDbRef.child("chats").child(senderRoom!!).child("messages").push()// push() will create a unique note when this push is called
                .setValue(messageObject).addOnSuccessListener {
                    //we add this addOnSuccessListner so that when sender sends the message the UI in receiver also gets updated at the same time
                    mDbRef.child("chats").child(receiverRoom!!).child("messages").push()// push() will create a unique note when this push is called
                        .setValue(messageObject)
                }
            messageBox.setText("")
        }

    }
}