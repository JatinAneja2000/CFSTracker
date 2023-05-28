package com.teessideUni.cfs_tracker.presentation.screens.settings_screen

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.*


class BluetoothDevicesScreen : ComponentActivity() {
    private val REQUEST_ENABLE_BLUETOOTH = 1
    private val REQUEST_PERMISSION_LOCATION = 2
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val pairedDevices = mutableStateListOf<BluetoothDevice>()
    private val discoveredDevices = mutableStateListOf<BluetoothDevice>()
    private val pairingDevices = mutableStateListOf<BluetoothDevice>()
    private val connectedDevices = mutableStateListOf<BluetoothDevice>()
    private var scanningState by mutableStateOf(false)
    private var showEnableBluetoothDialog by mutableStateOf(false)
    private var showLocationPermissionDialog by mutableStateOf(false)

    private val discoveryReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.S)
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action?.let { action ->
                when (action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device =
                            intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        device?.let {
                            if (!pairedDevices.contains(it) && hasBluetoothPermission() && hasAccessFineLocationPermission()) {
                                val deviceName = if (hasBluetoothScanPermission()) {
                                    if (context?.let { it1 ->
                                            ActivityCompat.checkSelfPermission(
                                                it1,
                                                Manifest.permission.BLUETOOTH_CONNECT
                                            )
                                        } != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        // TODO: Consider calling
                                        //    ActivityCompat#requestPermissions
                                        // here to request the missing permissions, and then overriding
                                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                        //                                          int[] grantResults)
                                        // to handle the case where the user grants the permission. See the documentation
                                        // for ActivityCompat#requestPermissions for more details.
                                        return
                                    }
                                    it.name
                                } else {
                                    null
                                }
                                if (deviceName != null && deviceName != "Unknown Device") {
                                    discoveredDevices.add(it)
                                }
                            }
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                        discoveredDevices.clear()
                        // Update UI or perform actions when discovery starts
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        // Update UI or perform actions when discovery finishes
                    }
                    else -> {
                    }
                }
            }
        }
    }

    private val bondStateReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.S)
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                val bondedDevice =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val bondState =
                    intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)

                if (bondedDevice != null) {
                    if (pairingDevices.contains(bondedDevice)) {
                        when (bondState) {
                            BluetoothDevice.BOND_BONDED -> {
                                // Device successfully paired
                                pairedDevices.add(bondedDevice)
                                pairingDevices.remove(bondedDevice)
                                unregisterReceiver(this)
                                connectDevice(bondedDevice)
                            }
                            BluetoothDevice.BOND_NONE -> {
                                // Pairing failed
                                pairingDevices.remove(bondedDevice)
                                unregisterReceiver(this)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun hasBluetoothPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun hasBluetoothScanPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasAccessFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BluetoothDeviceList()
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        showEnableBluetoothDialog = !bluetoothAdapter.isEnabled

        if (!hasLocationPermission()) {
            requestLocationPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(discoveryReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        registerReceiver(
            discoveryReceiver,
            IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        )
        registerReceiver(
            discoveryReceiver,
            IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        )
        registerReceiver(bondStateReceiver, IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED))

        // Refresh the paired devices list
        pairedDevices.clear()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        pairedDevices.addAll(bluetoothAdapter.bondedDevices)
    }


    override fun onPause() {
        super.onPause()
        unregisterReceiver(discoveryReceiver)
        unregisterReceiver(bondStateReceiver)
        stopScanning()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    @Composable
    fun BluetoothDeviceList() {
        val context = LocalContext.current

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Bluetooth Devices") })
            },
            content = {
                if (showEnableBluetoothDialog) {
                    AlertDialog(
                        onDismissRequest = { finish() },
                        title = { Text("Bluetooth is Disabled") },
                        text = { Text("Please enable Bluetooth to use this feature.") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val enableBluetoothIntent =
                                        Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                    if (ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.BLUETOOTH_CONNECT
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        startActivityForResult(
                                            enableBluetoothIntent,
                                            REQUEST_ENABLE_BLUETOOTH
                                        )
                                        showEnableBluetoothDialog = false
                                    }
                                }
                            ) {
                                Text("Enable Bluetooth")
                            }
                        }
                    )
                }

                if (showLocationPermissionDialog) {
                    AlertDialog(
                        onDismissRequest = { finish() },
                        title = { Text("Location Permission Required") },
                        text = { Text("Please grant location permission to use Bluetooth.") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val intent =
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    intent.data = Uri.fromParts("package", packageName, null)
                                    startActivity(intent)
                                    showLocationPermissionDialog = false
                                }
                            ) {
                                Text("Grant Permission")
                            }
                        }
                    )
                }

                Column(Modifier.fillMaxSize()) {
                    Button(
                        onClick = {
                            if (!scanningState) {
                                startScanning()
                            } else {
                                stopScanning()
                            }
                            scanningState = !scanningState
                        },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(if (scanningState) "Stop Scan" else "Start Scan")
                    }

                    Text(
                        "Paired Devices:",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )

                    LazyColumn(modifier = Modifier.padding(16.dp)) {
                        items(pairedDevices) { device ->
                            if (device.name != null && device.name != "Unknown") {
                                Card(modifier = Modifier.padding(8.dp)) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(device.name)
                                        Text(device.address)
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        "Discovered Devices:",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )

                    LazyColumn(modifier = Modifier.padding(16.dp)) {
                        items(discoveredDevices) { device ->
                            if (device.name != null && device.name != "Unknown Device") {
                                Card(modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth()) {
                                    Row(modifier = Modifier
                                        .padding(8.dp)
                                        .fillMaxWidth())
                                    {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(device.name)
                                            Text(device.address)

                                        }
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            if (!pairingDevices.contains(device)) {
                                                Button(
                                                    onClick = {
                                                        pairDevice(device)
                                                    },
                                                    modifier = Modifier.padding(top = 8.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        backgroundColor = Color.Blue
                                                    )
                                                ) {
                                                    Text("Pair", color = Color.White)
                                                }
                                            } else {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.padding(top = 8.dp),
                                                    color = Color.Blue
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (scanningState) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(16.dp),
                            color = Color.Blue
                        )
                    }
                }
            }

        )
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_PERMISSION_LOCATION
        )
    }

    private fun startScanning() {
        if (hasLocationPermission()) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                bluetoothAdapter.startDiscovery()
            }
        } else {
            showLocationPermissionDialog = true
        }
    }

    private fun stopScanning() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothAdapter.cancelDiscovery()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun pairDevice(device: BluetoothDevice) {
        if (device.bondState == BluetoothDevice.BOND_NONE) {
            pairingDevices.add(device)
            val bondRequest = device.createBond()
            if (bondRequest) {
                // Bonding request initiated successfully
            } else {
                // Bonding request failed
            }
        } else if (device.bondState == BluetoothDevice.BOND_BONDED) {
            // Device is already bonded, you can establish the connection using the UUID here
            // Example: Use the UUID to create an RfcommSocket and connect to the device
            try {
                val socket = if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                } else {
                    device.createRfcommSocketToServiceRecord(MY_UUID)
                }

                socket.connect()
                // Connection established successfully
            } catch (e: IOException) {
                // Connection failed
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.S)
    private fun connectDevice(device: BluetoothDevice) {
        val socket = if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                REQUEST_PERMISSION_LOCATION
            )
            return
        } else {
            device.createRfcommSocketToServiceRecord(MY_UUID)
        }

        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            socket.connect()
            connectedDevices.add(device)
            // Do something with the connected device
        } catch (e: IOException) {
            // Handle connection error
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (!bluetoothAdapter.isEnabled) {
                    showEnableBluetoothDialog = true
                }
            } else {
                // Permission denied. Show a message or handle the failure gracefully.
            }
        }
    }
}
