
package com.example.caffeinbody

import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.caffeinbody.databinding.FragmentHomeBinding
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.lang.Math.E
import java.time.Duration


class HomeFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private val dataClient by lazy { Wearable.getDataClient(activity) }
    private val messageClient by lazy { Wearable.getMessageClient(getActivity()) }
    private val capabilityClient by lazy { Wearable.getCapabilityClient(getActivity()) }
    private val nodeClient by lazy { Wearable.getNodeClient(getActivity()) }
    private val clientDataViewModel by viewModels<ClientDataViewModel>()
    //getActivity()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }

        lateinit var mainActivity: MainActivity
        mainActivity = context as MainActivity
        clientDataViewModel.mainActivity = mainActivity
    }

    private val binding: FragmentHomeBinding by lazy {
        FragmentHomeBinding.inflate(
            layoutInflater
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        //   initRecycler()

        binding.addBeverageBtn.setOnClickListener{
            activity?.let{
            val selectActivity =  DrinkTypeActivity()
            val intent = Intent(context, selectActivity::class.java)
            startActivity(intent)
        }}
        binding.showDetailText.setOnClickListener{
            activity?.let{
                val selectActivity =  DetailActivity()
                val intent = Intent(context, selectActivity::class.java)
                startActivity(intent)
            }
        }
        binding.checkWatchBtn.setOnClickListener{
            startWearableActivity()
            Log.e("폰에서 워치앱 열기 on HomeFrag", "하자!!!")
        }
        return binding.root
    }
///////////리스너 등록/제거 부분
    override fun onResume() {
        super.onResume()
        dataClient.addListener(clientDataViewModel)
        messageClient.addListener(clientDataViewModel)
        capabilityClient.addListener(
            clientDataViewModel,
            Uri.parse("wear://"),
            CapabilityClient.FILTER_REACHABLE
        )

        lifecycleScope.launch {
            try {
                capabilityClient.addLocalCapability(CAMERA_CAPABILITY).await()
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (exception: Exception) {
                Log.e(TAG, "Could not add capability: $exception")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        dataClient.removeListener(clientDataViewModel)
        messageClient.removeListener(clientDataViewModel)
        capabilityClient.removeListener(clientDataViewModel)

        lifecycleScope.launch {
            // This is a judicious use of NonCancellable.
            // This is asynchronous clean-up, since the capability is no longer available.
            // If we allow this to be cancelled, we may leave the capability in-place for other
            // nodes to see.
            withContext(NonCancellable) {
                try {
                    capabilityClient.removeLocalCapability(CAMERA_CAPABILITY).await()
                } catch (exception: Exception) {
                    Log.e(TAG, "Could not remove capability: $exception")
                }
            }
        }
    }
////////////////워치로 열기 버튼 누르면 워치로 메시지 보내서 앱 열게 함
    private fun startWearableActivity() {
        lifecycleScope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()//노드 검색
                Log.e("nodes: ", nodes[0].toString())//노드 검색은 된다
                // Send a message to all nodes in parallel
                nodes.map { node ->//워치에서 메시지 받게
                    async {
                        messageClient.sendMessage(node.id, START_ACTIVITY_PATH, byteArrayOf())
                            .await()
                    }
                }.awaitAll()

                Log.d(TAG, "Starting activity requests sent successfully")
            } catch (cancellationException: CancellationException) {
                Log.e(TAG, "실패1")
                throw cancellationException
            } catch (exception: Exception) {
                Log.d(TAG, "실패2")
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"

        private const val START_ACTIVITY_PATH = "/start-activity3"
        private const val COUNT_PATH = "/count"
        private const val IMAGE_PATH = "/image"
        private const val IMAGE_KEY = "photo"
        private const val TIME_KEY = "time"
        private const val COUNT_KEY = "count"
        private const val CAMERA_CAPABILITY = "camera"

        private val countInterval = Duration.ofSeconds(5)
    }

}