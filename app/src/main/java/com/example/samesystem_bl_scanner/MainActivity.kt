package com.example.samesystem_bl_scanner

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neovisionaries.bluetooth.ble.advertising.ADPayloadParser
import com.neovisionaries.bluetooth.ble.advertising.ADStructure
import com.neovisionaries.bluetooth.ble.advertising.Eddystone
import com.neovisionaries.bluetooth.ble.advertising.IBeacon

class MainActivity : ComponentActivity() {

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val scanSettings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
            .build()
    } else {
        ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (result != null) {
                val device = result.device
                device?.let {
                    if (!bluetoothDevices.contains(it)) {
                        bluetoothDevices.add(it)
                        showList()

                        var structures = ADPayloadParser.getInstance().parse(result.scanRecord?.bytes)

                        structures.forEach { structure->
                            if(structure is IBeacon){
                                Log.v("DEVICELOG:IBEACON:FOUND", structure.toString() + "Address : ${result.device.address}" + " Bytes : ${result.scanRecord?.bytes?.joinToString("," )}")
                            }
                            if(structure is Eddystone){
                                Log.v("DEVICELOG:EddySTONE:FOUND", structure.toString()+ "Address : ${result.device.address}" + " Bytes : ${result.scanRecord?.bytes?.joinToString("," )}")
                            }
                        }
                    }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e("ScanCallback", "onScanFailed: code $errorCode")
        }
    }

    private val bluetoothDevices: ArrayList<BluetoothDevice> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        showList()

        startBluetoothDiscovery()
    }

    fun showList(){
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RefreshButton(onClick = ::refreshBluetoothDevices)
                    DeviceList(devices = bluetoothDevices)
                }
            }
        }
    }

    private fun startBluetoothDiscovery() {
        bluetoothAdapter.startDiscovery()
        bleScanner.startScan(null, scanSettings, scanCallback)
    }

    private fun refreshBluetoothDevices() {
        bluetoothDevices.clear()
        startBluetoothDiscovery()
    }

    override fun onDestroy() {
        super.onDestroy()
        bleScanner.stopScan(scanCallback)
    }
}

val abdallahBeacon = "FA:62:81:77:7F:13"
val aljBeacon = "FF:69:56:1B:B3:5B"
val eddystoneBeacon = "F9:61:80:76:7E:12"

@Composable
fun RefreshButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Refresh Devices")
    }
}

@Composable
fun DeviceList(devices: List<BluetoothDevice>) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(devices) { device ->
            val name = device.name ?: when {
                device.address.equals(abdallahBeacon) -> {
                    "Abdullah Mohamed"
                }
                device.address.equals(aljBeacon) -> {
                    "ALJ30214700276"
                }
                device.address.equals(eddystoneBeacon) -> {
                    "EddyStone"
                }
                else -> {
                    "Unknown Device"
                }
            }
            Text(text = "${name} - ${device.address}")
        }
    }
}