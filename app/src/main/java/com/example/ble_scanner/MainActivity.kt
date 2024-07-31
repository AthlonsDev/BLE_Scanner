package com.example.ble_scanner

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.recyclerview.widget.LinearLayoutManager

import com.example.ble_scanner.Constants.REQUEST_ENABLE_BT
import com.example.ble_scanner.databinding.ActivityMainBinding

object Constants {
    const val REQUEST_CODE_BLUETOOTH_CONNECT = 1
    const val REQUEST_CODE_BLUETOOTH_SCAN = 1
    const val REQUEST_ENABLE_BT = 1
    const val REQUEST_FINE_LOCATION = 1
}
private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
private val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

private lateinit var binding: ActivityMainBinding
private var scanning = false
private val handler = Handler()
//private var bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()




class MainActivity : AppCompatActivity() {


    var data = ArrayList<ItemsViewModel>()
    var adapter: DeviceListAdapter? = null
    var bleGatt: BluetoothGatt? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)



        val textView = binding.textView
        val listView = binding.deviceListView

        listView.layoutManager = LinearLayoutManager(this@MainActivity)
        adapter = DeviceListAdapter(data)
        listView.adapter = adapter


        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            textView.text = "Device doesn't support Bluetooth"
        }
        else {
            // Device supports Bluetooth
            textView.text = "Device supports Bluetooth"
        }

        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        requestBluetooth()

        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
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
            startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT)
        } else {
//            Toast.makeText(this, "Bluetooth is already enabled", Toast.LENGTH_SHORT).show()
        }

        requestBluetooth()

        adapter!!.setOnClickListener {
            val position = listView.getChildAdapterPosition(it)
            val device = data[position]
            val deviceAddress = device.address
            val deviceName = device.name
            val deviceRssi = device.rssi
            Toast.makeText(
                this,
                "Device Name: $deviceName\nDevice Address: $deviceAddress\nDevice RSSI: $deviceRssi",
                Toast.LENGTH_SHORT
            ).show()
        }

    }

    private fun deviceList(deviceName: String, deviceAddress: String, rssi: Int) {

        for (i in 0 until 10) {
            data.add(ItemsViewModel(deviceName, deviceAddress, rssi))
        }
    }

    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000

    private fun scanLeDevice(enable: Boolean) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            handler.postDelayed({
                scanning = false
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_SCAN

                        ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                        Constants.REQUEST_CODE_BLUETOOTH_SCAN
                    )
                }
                bluetoothLeScanner.stopScan(leScanCallback)
            }, SCAN_PERIOD)
            scanning = true
            bluetoothLeScanner.startScan(leScanCallback)

        } else {
            scanning = false
            bluetoothLeScanner.stopScan(this.leScanCallback)
        }
    }

    override fun onResume() {
        super.onResume()
        scanLeDevice(true)
    }

    override fun onPause() {
        super.onPause()
        scanLeDevice(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        scanLeDevice(false)
    }

    fun requestBluetooth() {
        // check android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestEnableBluetooth.launch(enableBtIntent)
        }
    }

    private val requestEnableBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // granted
            } else {
                // denied
            }
        }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d("MyTag", "${it.key} = ${it.value}")
            }
        }


    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d("ScanCallback", "onScanResult")
            val device: BluetoothDevice = result.device
            val deviceName = device.name
            val deviceAddress = device.address
            val rssi = result.rssi

            if (deviceName != null && deviceAddress != null && rssi != null) {
                data.add(ItemsViewModel(deviceName, deviceAddress, rssi))
                adapter?.notifyDataSetChanged()
            }

//            Toast.makeText(this@MainActivity, "Device found: $deviceName", Toast.LENGTH_SHORT).show()
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(this@MainActivity, "Bluetooth Connect permission denied", Toast.LENGTH_SHORT).show()

                return
            }

        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Toast.makeText(this@MainActivity, "Scan failed", Toast.LENGTH_SHORT).show()
        }

    }




}