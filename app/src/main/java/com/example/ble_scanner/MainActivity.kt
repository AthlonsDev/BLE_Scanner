package com.example.ble_scanner

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.STATE_CONNECTED
import android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ble_scanner.BluetoothLeService.Companion.ACTION_GATT_CONNECTED
import com.example.ble_scanner.BluetoothLeService.Companion.ACTION_GATT_DISCONNECTED
import com.example.ble_scanner.BluetoothLeService.Companion.ACTION_GATT_SERVICES_DISCOVERED
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
    var bluetoothService: BluetoothLeService? = null
    var connected = false
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

        adapter?.setOnClickListener(object : DeviceListAdapter.OnClickListener {
            override fun onClick(position: Int, model: ItemsViewModel) {
//                Toast.makeText(this@MainActivity, "Clicked on $position, device: ${model.name}", Toast.LENGTH_SHORT).show()
                val device = bluetoothAdapter?.getRemoteDevice(model.address)
                if (device != null) {

                    if (connect(model.address)) {
                        Toast.makeText(this@MainActivity, "Connected to device: ${model.name}", Toast.LENGTH_SHORT).show()
//                        discover gatt services
                        gattDiscoverServices()
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to connect to device: ${model.name}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })

    }
    var connectionState = STATE_CONNECTED
    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // successfully connected to the GATT Server
//                broadcastUpdate(ACTION_GATT_CONNECTED)

                // Attempts to discover services after successful connection.
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
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
                bleGatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // disconnected from the GATT Server
//                broadcastUpdate(ACTION_GATT_DISCONNECTED)
                connectionState = STATE_DISCONNECTED
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
//                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
                displayGattServices(gatt?.services)
            } else {
//                Log.w(BluetoothLeService.TAG, "onServicesDiscovered received: $status")
            }
        }
    }

    private fun gattDiscoverServices() {
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
        bleGatt?.discoverServices()
//        if (bleGatt?.services?.size == 0) {
//            Toast.makeText(this, "GATT Services: ${bleGatt?.services}", Toast.LENGTH_SHORT).show()
//            return
//        }
        displayGattServices(bleGatt?.services)

    }

    var deviceAddress = ""
//    connect to device
    fun connect(address: String): Boolean {
        bluetoothAdapter?.let { adapter ->
            try {
                deviceAddress = address
                val device = adapter.getRemoteDevice(address)
            }
            catch (exception: IllegalArgumentException) {
                Log.w(TAG, "Device not found with provided address.")
                return false
            }
        // connect to the GATT server on the device
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {

        }
        bleGatt = adapter.getRemoteDevice(address).connectGatt(this, false, gattCallback)
        return true
        } ?: run {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return false
        }
        return true
    }

    // Code to manage Service lifecycle.
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            componentName: ComponentName,
            service: IBinder
        ) {
            bluetoothService = (service as BluetoothLeService.LocalBinder).getService()
            bluetoothService?.let { bluetooth ->
                if (!bluetooth.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth")
                    finish()
                }
                // perform device connection
                bluetooth.connect(deviceAddress)
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            bluetoothService = null
        }
    }

    private fun sendCommand(characteristic: BluetoothGattCharacteristic, command: String) {
        characteristic.setValue(command)
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
        bleGatt?.writeCharacteristic(characteristic)
    }


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
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_PRIVILEGED,
                    Manifest.permission.BLUETOOTH

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


    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    connected = true
//                    updateConnectionState(R.string.connected)
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    connected = false
//                    updateConnectionState(R.string.disconnected)
                }
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    // Show all the supported services and characteristics on the user interface.
                    displayGattServices(bluetoothService?.getSupportedGattServices() as List<BluetoothGattService>?)
                }
            }
        }
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter? {
        return IntentFilter().apply {
            addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        }
    }








    var mGattCharacteristics: MutableList<BluetoothGattCharacteristic> = mutableListOf()
    private fun displayGattServices(gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return
        var uuid: String = ""
        var unknownServiceString = "Unknown Service"
        var unknownCharaString = "Unknown Characteristic"
        val gattServiceData: ArrayList<HashMap<String, String>> = arrayListOf()
        val gattCharacteristicData: ArrayList<ArrayList<HashMap<String, String>>> = arrayListOf()


        // Loops through available GATT Services.
        gattServices.forEach { gattService ->
            val currentServiceData = HashMap<String, String>()
            uuid = gattService.uuid.toString()
//            currentServiceData[LIST_NAME] = SampleGattAttributes.lookup(uuid, unknownServiceString)
//            currentServiceData[LIST_UUID] = uuid
            gattServiceData += currentServiceData

            val gattCharacteristicGroupData: ArrayList<HashMap<String, String>> = arrayListOf()
            val gattCharacteristics = gattService.characteristics
            val charas: MutableList<BluetoothGattCharacteristic> = mutableListOf()

            // Loops through available Characteristics.
            gattCharacteristics.forEach { gattCharacteristic ->
                charas += gattCharacteristic
                val currentCharaData: HashMap<String, String> = hashMapOf()
                uuid = gattCharacteristic.uuid.toString()
//                currentCharaData[LIST_NAME] = SampleGattAttributes.lookup(uuid, unknownCharaString)
//                currentCharaData[LIST_UUID] = uuid
                gattCharacteristicGroupData += currentCharaData
            }

            mGattCharacteristics += charas
            gattCharacteristicData += gattCharacteristicGroupData
//            write characteristic
            if (charas.isNotEmpty()) {
                val charas = charas.filter { it.properties and BluetoothGattCharacteristic.PROPERTY_WRITE > 0 }
                if (charas.isNotEmpty()) {
                    sendCommand(charas[0], "Hello")
                }
            }
        }
    }



    private val bluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            // Handle connection state changes here

        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            // Handle characteristic reads here
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            // Handle characteristic writes here

        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            // Handle characteristic changes here
        }

    }





}